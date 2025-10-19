package com.chat.common;

import java.util.Optional;

/**
 * 애플리케이션에서 지원하는 언어의 종류를 정의하고 관리하는 열거형(Enum)입니다.
 *
 * 이 Enum은 각 언어에 대해 여러 외부 서비스(e.g., Naver CSR, Papago)에서 사용하는
 * 각기 다른 언어 코드를 중앙에서, 타입에 안전한(type-safe) 방식으로 관리하는 역할을 합니다.
 * 예를 들어, 클라이언트는 "Kor"이라는 코드를 보내면, 서버 내부에서는 이 Enum을 통해
 * Naver 음성 인식(CSR)에는 "Kor"을, Papago 번역에는 "ko"를 사용하는 식으로 변환할 수 있습니다.
 *
 * 객체를 사용하는 이유
 * -> 관련 데이터를 함께 묶어서 관리 (캡슐화)
 * -> 객체 안에는 단순한 이름뿐만 아니라, 여러 서비스에서 필요한 다양한 정보들이 함께 들어있음
 * * Lang.KOR.csr -> "Kor"
 * * Lang.KOR.bcp47 -> "ko-KR"
 */
public enum Lang {
    KOR("Kor", "ko-KR"),
    ENG("Eng", "en-US"),
    JPN("Jpn", "ja-JP"),
    CHN("Chn", "zh-CN");

    // Naver Clova Speech Recognition (CSR) API의 lang 파라미터에 사용되는 코드 ("Kor", "Eng")
    public final String csr;
    // IETF BCP 47 표준을 따르는 언어 태그 ("ko-KR", "en-US")
    public final String bcp47;

    Lang(String csr, String bcp47) {
        this.csr = csr;
        this.bcp47 = bcp47;
    }

    /**
     * Naver CSR 언어 코드를 Papago API에서 사용하는 언어 코드로 변환
     *
     * @param csrLang 변환할 Naver CSR 언어 코드
     * @return Papago API에서 사용하는 언어 코드
     */
    public static String mapCsrToPapago(String csrLang) {
        switch (csrLang) {
            case "Kor": return "ko";
            case "Eng": return "en";
            case "Jpn": return "ja";
            case "Chn": return "zh-CN";
            default:    return "ko";
        }
    }

    /**
     * 클라이언트로부터 받은 문자열 코드(e.g., "Kor")를 기반으로 해당하는 Lang Enum 상수를 안전하게 찾아 반환하는 정적 팩토리 메서드
     *
     * @param code 클라이언트가 전송한 언어 코드 문자열 (e.g., "Kor", "Eng")
     * @return 해당하는 Lang Enum 상수를 담은 Optional 객체를 반환합니다.
     *
     */
    public static Optional<Lang> fromClientCode(String code) {
        if (code == null) return Optional.empty();
        return switch (code) {
            case "Kor" -> Optional.of(KOR);
            case "Eng" -> Optional.of(ENG);
            case "Jpn" -> Optional.of(JPN);
            case "Chn" -> Optional.of(CHN);
            default -> Optional.empty();
        };
    }

}
