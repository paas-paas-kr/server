package com.chat.audio;

import com.chat.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class AudioTranscoder {

    private final AppProperties props;

    /** webm/opus → WAV(PCM s16le, 16kHz, mono) */
    /**
     * ffmpeg: 합친 오디오를 STT가 요구하는 포맷으로 바꾸는 외부 실행 도구
     * 1. 클라이언트에서 WebSocket으로 오디오 청크 전송
     * 2. 서버에서 aggregator.merge()로 하나의 바이트 배열로 합침
     * 3. ffmpeg로 변환: webm/opus -> wav(PCM 16k mono)
     * 4. 변환된 바이트를 STT 서버에 전송(헤더+바디로 HTTP 호출)
     * 즉, merge 결과(바이트)를 ffmpeg에 입력으로 주고, ffmpeg가 출력 바이트를 만들어주면 그걸 STT로 보낸다.
     *
     *
     */
    public byte[] webmOpusToPcmWav16kMono(byte[] webmBytes) throws IOException, InterruptedException {
        // 설정 파일에서 ffmpeg 실행 파일 경로를 가져온다.
        String ffmpeg = props.getAudio().getFfmpegPath();
        System.out.println("ffmpeg 경로:" + ffmpeg);

        /**
         *  createTempFile은 안전하게 유니크한 임시 파일을 즉시 만들어주는 API
         *  파일명은 prefix + 랜덤 문자열 + suffix 형식으로 만들어 충돌 없이 유일
         *  예: in-3721al.webm, out-6fcd02.wav
         *  임시 입력 파일에 바이트를 쓰고(.WebM)-> ffmpeg로 변환 -> 임시 출력 파일에서 결과를 읽고(.WAV) -> 모두 삭제
         */
        File in = File.createTempFile("in-", ".webm");
        File out = File.createTempFile("out-", ".wav");

        /**
         * new FileOutputStream(in)은 in이 가리키는 파일을 쓰기 모드로 연다
         * 기본은 덮어쓰기 모드, 파일 안의 기존 내용이 있다면 처음부터 다시 쓴다.
         * 추가로 붙이고 싶으면 new FileOutputStream(in, true)처럼 두 번째 인자에 true를 줘야함
         */
        try (FileOutputStream fos = new FileOutputStream(in)) {
            fos.write(webmBytes);
        }
        System.out.println(in.toString());
        File ff= new File(ffmpeg);
        if(!ff.exists() || !ff.isFile()){
            throw new FileNotFoundException("ffmpeg 실행 파일을 찾을 수 없습니다.");
        }

        /**
         * 자바 프로세스 밖에서 외부 프로그램(ffmpeg)을 실행하기 위한 명령을 구성
         * ffmpeg: 실행 파일 경로, -y: 출력 파일이 이미 있어도 무조건 덮어쓰기
         * -i <in>: 입력 파일 경로 (in.getAbsolutePath()로 절대경로 사용)
         * -ac 1: 채널 수를 모노(1ch)로 변환
         * -ar 16000: 샘플레이트 16kHz로 리샘플링
         * -c:a pcm_s16le: 오디오 코텍을 PCM 16-bit little endian으로 지정
         * <out> : 출력 경로 확장자가 .wav라서 FFmpeg가 자동으로 WAV 컨테이너를 선택
         * -> 명시적으로 하려면 -f wav 추가
         */
        ProcessBuilder pb = new ProcessBuilder(
                ffmpeg, "-y",
                "-i", in.getAbsolutePath(),
                "-vn", "-sn",
                "-ac", "1", "-ar", "16000",
                "-af", "aformat=sample_fmts=s16:channel_layouts=mono,aresample=resampler=soxr:precision=33:cutoff=0.97:dither_method=triangular",
                "-c:a", "pcm_s16le",
                "-map_metadata", "-1",
                "-fflags", "+bitexact",
                out.getAbsolutePath()
        );

        /**
         * stderr를 stdout으로 합친다.
         * FFmpeg는 진행 상황/경고/오류를 주로 stderr로 쓴다.
         * 이 설정을 켜면 p.getInputStream() 하나만 읽으면 된다
         */
        pb.redirectErrorStream(true);

        /**
         * 실제로 프로세스를 시작, 이 순간부터 OS에서 ffmpeg가 실행된다.
         * 반환된 Process로 표준 입출력/에러/종료코드 등을 제어한다.
         * - p.getInputStream(): ffmpeg의 로그/메시지를 읽음
         * - p.waitFor(): 동기 대기(블로킹) 후 종료코드 확인
         */
        Process p = pb.start();
        System.out.println("4");

        /**
         * p.getInputStream() = ffmpeg의 표준 출력(stdout)
         *
         * ffmpeg는 변환 중 로그를 계속 쓰는데, 이 스트림을 안 읽으면 내부 파이프 버퍼가
         * 꽉 차서 ffmpeg쓰기가 막히고 변환 자체가 멈출 수 있다(데드락)
         * -> ffmpeg가 쓰는 로그: 오디오 파일 데이터랑 전혀 관계없는 콘솔 출력(상태메시지)
         * br.readLine()은 fmmpeg가 한 줄 쓰면 그 줄을 읽어 오고,
         * 더 이상 쓸 게 없으면(프로세스가 stdout을 닫으면) null을 반환한다.
         * -> 즉 while 루프는 데이터가 들어오는 대로 계속 소비하다가, ffmpeg가 종료해 스트림이 닫히면 끝난다.
         */
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {}
        }

        /**
         * p.waitfor()는 ffmpeg 프로세스가 완전히 끝날 때까지 블로킹 대기한다.
         * 이 시점에 ffmpeg가 파일 핸들을 닫았으므로, 출력 파일의 쓰기가 완료된 상태가 된다.
         * exit code!=0이면 FFMPEG가 오류로 종료했다는 뜻이니 즉시 실패 처리한다.
         */
        int code = p.waitFor();
        if (code != 0) throw new IOException("ffmpeg failed: exit=" + code);

        /**
         * ffmpeg 종료 후라 WAV 파일이 온전히 완성되어 있어야 한다.
         * 이 라인은 파일 전체를 메모리로 읽어 바이트 배열로 돌려준다.
         */
        byte[] wav = Files.readAllBytes(out.toPath());

        // 임시로 생성된 in,out 파일 정리
        in.delete();
        out.delete();

        return wav;
    }
}
