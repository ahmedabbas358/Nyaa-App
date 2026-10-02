package com.aniflow.data.repository

import com.aniflow.domain.automation.model.AutomationAction
import com.aniflow.domain.automation.model.AutomationRule
import com.aniflow.domain.automation.model.AutomationTrigger
import com.aniflow.domain.identity.AutomationRuleId
import com.aniflow.domain.identity.WatchlistId
import com.aniflow.domain.identity.WatchlistItemId
import com.aniflow.domain.repository.AutomationRuleRepository
import com.aniflow.domain.repository.WatchlistRepository
import com.aniflow.domain.watchlist.model.Watchlist
import com.aniflow.domain.watchlist.model.WatchlistItem
import com.aniflow.domain.watchlist.model.WatchlistPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class WatchlistRepositoryImpl : WatchlistRepository {
    private val watchlists = ConcurrentHashMap<WatchlistId, Watchlist>()
    private val _flow = MutableStateFlow<List<Watchlist>>(emptyList())

    init {
        val sample = Watchlist(
            id = WatchlistId("wl_default"),
            name = "Active Seasonal Anime",
            enabled = true,
            policy = WatchlistPolicy(notifyOnly = true, autoDownload = false)
        )
        watchlists[sample.id] = sample
        _flow.value = watchlists.values.toList()
    }

    override suspend fun getById(id: WatchlistId): Watchlist? = watchlists[id]

    override fun observeAll(): Flow<List<Watchlist>> = _flow.asStateFlow()

    override suspend fun save(watchlist: Watchlist) {
        watchlists[watchlist.id] = watchlist
        _flow.value = watchlists.values.toList()
    }

    override suspend fun delete(id: WatchlistId) {
        watchlists.remove(id)
        _flow.value = watchlists.values.toList()
    }

    override suspend fun addItem(item: WatchlistItem) {
        watchlists[item.watchlistId]?.let { wl ->
            val updatedItems = wl.items.filter { it.id != item.id } + item
            watchlists[wl.id] = wl.copy(items = updatedItems)
            _flow.value = watchlists.values.toList()
        }
    }

    override suspend fun removeItem(itemId: WatchlistItemId) {
        for ((id, wl) in watchlists) {
            val updated = wl.items.filter { it.id != itemId }
            if (updated.size != wl.items.size) {
                watchlists[id] = wl.copy(items = updated)
            }
        }
        _flow.value = watchlists.values.toList()
    }
}

class AutomationRuleRepositoryImpl : AutomationRuleRepository {
    private val rules = ConcurrentHashMap<AutomationRuleId, AutomationRule>()
    private val _flow = MutableStateFlow<List<AutomationRule>>(emptyList())

    init {
        val sampleRule = AutomationRule(
            id = AutomationRuleId("rule_one_piece"),
            name = "Auto-download One Piece (1080p HEVC)",
            enabled = true,
            trigger = AutomationTrigger.EpisodeAvailable(
                animeId = com.aniflow.domain.identity.AnimeId("one_piece"),
                episodeNumber = 1090.0
            ),
            actions = listOf(AutomationAction.QueueDownload),
            priority = 100
        )
        rules[sampleRule.id] = sampleRule
        _flow.value = rules.values.toList()
    }

    override suspend fun getById(id: AutomationRuleId): AutomationRule? = rules[id]

    override fun observeAll(): Flow<List<AutomationRule>> = _flow.asStateFlow()

    override suspend fun save(rule: AutomationRule) {
        rules[rule.id] = rule
        _flow.value = rules.values.toList()
    }

    override suspend fun delete(id: AutomationRuleId) {
        rules.remove(id)
        _flow.value = rules.values.toList()
    }
}
