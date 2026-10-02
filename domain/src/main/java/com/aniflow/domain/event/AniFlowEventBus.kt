package com.aniflow.domain.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AniFlowEventBus (Sections 43, 44).
 * Central, thread-safe asynchronous domain event bus allowing decoupled communication
 * between Provider, Selection, Download Runtime, Storage, Library, and Automation layers
 * without any Android framework dependencies.
 */
class AniFlowEventBus {

    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    val events: Flow<DomainEvent> = _events.asSharedFlow()

    suspend fun emit(event: DomainEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: DomainEvent): Boolean {
        return _events.tryEmit(event)
    }
}
