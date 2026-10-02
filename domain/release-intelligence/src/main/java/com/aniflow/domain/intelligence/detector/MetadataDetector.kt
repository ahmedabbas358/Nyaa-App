package com.aniflow.domain.intelligence.detector

import com.aniflow.domain.intelligence.model.DetectionResult
import com.aniflow.domain.intelligence.model.ParseContext

/**
 * Universal contract for specialized, deterministic metadata extractors (Section 15, 16).
 */
interface MetadataDetector<T> {
    val name: String
    fun detect(context: ParseContext): DetectionResult<T>
}
