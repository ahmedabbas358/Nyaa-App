package com.aniflow.provider.nyaa

import com.aniflow.domain.identity.ProviderId

/**
 * Canonical constant identifier for Nyaa provider (Section 4).
 * Prevents raw stringly-typed "nyaa" littering the codebase.
 */
val nyaaProviderId = ProviderId("nyaa")
