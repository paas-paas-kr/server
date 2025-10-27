package com.document.application.summary.llm;

import com.common.enumtype.Language;

public interface AiSummaryProcessor {

	String summarizeText(final String text, final Language language);
}
