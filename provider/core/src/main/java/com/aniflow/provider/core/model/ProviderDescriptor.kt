package com.aniflow.provider.core.model

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.valueobject.UrlValue

/**
 * Metadata descriptor for a ReleaseProvider (Section 6).
 */
data class ProviderDescriptor(
    val id: ProviderId,
    val name: String,
    val baseUrl: UrlValue,
    val capabilities: ProviderCapabilities,
    val version: String = "1.0.0",
    val description: String = ""
)
