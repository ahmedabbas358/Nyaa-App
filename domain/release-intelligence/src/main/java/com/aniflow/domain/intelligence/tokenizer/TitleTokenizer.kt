package com.aniflow.domain.intelligence.tokenizer

import com.aniflow.domain.intelligence.model.TitleToken

/**
 * Backward compatibility facade delegating to ReleaseTokenizer (Section 8, 9).
 */
object TitleTokenizer {
    fun tokenize(preNormalizedTitle: String): List<TitleToken> {
        return ReleaseTokenizer.tokenize(preNormalizedTitle)
    }
}
