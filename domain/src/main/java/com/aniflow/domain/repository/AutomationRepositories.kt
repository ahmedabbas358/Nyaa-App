package com.aniflow.domain.repository

import com.aniflow.domain.automation.model.AutomationRule
import com.aniflow.domain.identity.AutomationRuleId
import com.aniflow.domain.identity.WatchlistId
import com.aniflow.domain.identity.WatchlistItemId
import com.aniflow.domain.watchlist.model.Watchlist
import com.aniflow.domain.watchlist.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun getById(id: WatchlistId): Watchlist?
    fun observeAll(): Flow<List<Watchlist>>
    suspend fun save(watchlist: Watchlist)
    suspend fun delete(id: WatchlistId)
    suspend fun addItem(item: WatchlistItem)
    suspend fun removeItem(itemId: WatchlistItemId)
}

interface AutomationRuleRepository {
    suspend fun getById(id: AutomationRuleId): AutomationRule?
    fun observeAll(): Flow<List<AutomationRule>>
    suspend fun save(rule: AutomationRule)
    suspend fun delete(id: AutomationRuleId)
}
