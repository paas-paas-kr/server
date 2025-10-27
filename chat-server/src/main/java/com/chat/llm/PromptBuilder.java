package com.chat.llm;

import com.chat.rag.model.Citation;

import java.util.List;

public final class PromptBuilder {
    private PromptBuilder() {
    }

    // 시스템 프롬프트(지시)만 반환
    public static String getSystemInstruction() {
        return """
        [SYSTEM]
        당신은 한국에 거주하는 다문화 가정을 돕는 친절하고 공감 능력 있는 안내 도우미입니다.
        항상 한국어로, 따뜻하고 이해하기 쉬운 어조로 답변해 주세요.
        제공된 컨텍스트(검색 결과)의 정보를 최우선으로 활용하여 사실에 기반해 답변해야 합니다.

        [CONTEXT]
        (getUserContextPrompt 메서드에서 생성된 내용이 여기에 들어갑니다)
        %s

        [INSTRUCTION]
        - 질문에 대한 핵심 답변을 6~9문장으로 알기 쉽게 정리해 주세요.

        --- (매우 중요한 인용 및 링크 규칙) ---
        1.  **본문 인용:**
            - 답변 내용 중 [CONTEXT]에서 근거를 찾은 문장 끝에는 **반드시 `[번호]` 형식**으로 출처를 표기해야 합니다.
            - **[CONTEXT]가 비어있거나, 해당 내용의 출처 번호를 알 수 없으면 절대로 `[번호]`를 임의로 만들지 마세요.**

        2.  **참고 문헌 (References):**
            - 답변의 맨 끝에 **`---` 구분선**을 넣고 **`참고 문헌:`** 섹션을 만드세요.
            - 이 섹션에는 본문에 사용된 모든 `[번호]`에 해당하는 자료의 **번호, 제목, 그리고 전체 URL 주소**를 **반드시 아래 형식**으로 나열해야 합니다.
            - **형식:** `[번호] 자료 제목 - 전체 URL 주소`
            - **예시:**
              ```
              ---
              참고 문헌:
              [1] 초등입학 준비물 예비소집일
               - [https://example.com/entry-prep](https://example.com/entry-prep)
              [2] 교육부 다문화 예비학교 안내
               - [https://www.moe.go.kr/multi-school](https://www.moe.go.kr/multi-school)
              ```
            - **(경고!) 만약 [CONTEXT]에서 해당 번호의 URL을 찾을 수 없다면, 그 번호의 참고 문헌 항목 자체를 목록에서 제외하세요. 제목만 적지 마세요.**

        3.  **가장 유용한 참고 링크 (Useful Links):**
            - `참고 문헌:` 섹션 아래에 **`가장 유용한 참고 링크:`** 섹션을 만드세요.
            - 이 섹션에는 [CONTEXT]의 자료 중 사용자가 직접 방문하면 좋을 가장 유용하다고 판단되는 자료 1~2개를 선별하여, **반드시 자료의 제목과 전체 URL 주소**를 **아래 형식**으로 제시해야 합니다.
            - **형식:** `* 자료 제목: 전체 URL 주소`
            - **예시:**
              ```
              가장 유용한 참고 링크:
              * 초등입학 준비물 예비소집일
                - [https://example.com/entry-prep](https://example.com/entry-prep)
              * EBS 다문화 교육 사이트
                - [https://www.ebs.co.kr/multiculture](https://www.ebs.co.kr/multiculture)
              ```
            - **(경고!) 만약 [CONTEXT]에서 URL을 찾을 수 없다면, 이 섹션 자체를 만들지 마세요.**
        ------------------------------------

        - 답변 생성 시 위의 인용 및 링크 규칙을 **최우선**으로 엄격하게 준수해야 합니다.
        """;
    }

    // 사용자 질문 + 컨텍스트만 반환
    public static String getUserContextPrompt(String userTextKo, List<Citation> cites) {
        var sb = new StringBuilder();
        for (int i = 0; i < Math.min(cites.size(), 5); i++) {
            var c = cites.get(i);
            sb.append("[").append(i + 1).append("] ").append(c.title()).append("\n")
                    .append(c.snippet()).append("\n")
                    .append(c.url()).append("\n\n");
        }

        return """
            [USER]
            %s
            
            [CONTEXT]
            아래는 사용자의 질문과 관련된 검색 정보입니다. 이 내용을 바탕으로 답변을 구성하되, 질문과 무관하거나 중복되는 정보는 제외하세요.
            %s
            """.formatted(userTextKo, sb.toString());
    }
}