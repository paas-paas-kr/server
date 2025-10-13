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
    public byte[] webmOpusToPcmWav16kMono(byte[] webmBytes) throws IOException, InterruptedException {
        String ffmpeg = props.getAudio().getFfmpegPath();

        File in = File.createTempFile("in-", ".webm");
        File out = File.createTempFile("out-", ".wav");

        try (FileOutputStream fos = new FileOutputStream(in)) {
            fos.write(webmBytes);
        }

        ProcessBuilder pb = new ProcessBuilder(
                ffmpeg, "-y",
                "-i", in.getAbsolutePath(),
                "-ac", "1",
                "-ar", "16000",
                "-c:a", "pcm_s16le",
                out.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // 로그 흡수
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {}
        }

        int code = p.waitFor();
        if (code != 0) throw new IOException("ffmpeg failed: exit=" + code);

        byte[] wav = Files.readAllBytes(out.toPath());
        // 정리
        in.delete();
        out.delete();
        return wav;
    }
}
