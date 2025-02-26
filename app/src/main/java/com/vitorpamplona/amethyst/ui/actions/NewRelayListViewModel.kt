package com.vitorpamplona.amethyst.ui.actions

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.service.relays.Constants
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.RelayPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nostr.postr.events.ContactListEvent

class NewRelayListViewModel: ViewModel() {
    private lateinit var account: Account

    data class Relay(
        val url: String,
        val read: Boolean,
        val write: Boolean,
        val errorCount: Int = 0,
        val downloadCount: Int = 0,
        val uploadCount: Int = 0,
        val feedTypes: Set<FeedType>
    )

    private val _relays = MutableStateFlow<List<Relay>>(emptyList())
    val relays = _relays.asStateFlow()

    fun load(account: Account, ctx: Context) {
        this.account = account
        clear(ctx)
    }

    fun create(ctx: Context) {
        relays.let {
            account.saveRelayList(it.value)
            LocalPreferences(ctx).saveToEncryptedStorage(account)
        }

        clear(ctx)
    }

    fun clear(ctx: Context) {
        _relays.update {
            val relayFile = account.userProfile().relays

            if (relayFile != null)
                relayFile.map {
                    val liveRelay = RelayPool.getRelay(it.key)
                    val localInfoFeedTypes = account.localRelays.filter { localRelay -> localRelay.url == it.key }.firstOrNull()?.feedTypes ?: FeedType.values().toSet()

                    val errorCounter = liveRelay?.errorCounter ?: 0
                    val eventDownloadCounter = liveRelay?.eventDownloadCounter ?: 0
                    val eventUploadCounter = liveRelay?.eventUploadCounter ?: 0

                    Relay(it.key, it.value.read, it.value.write, errorCounter, eventDownloadCounter, eventUploadCounter, localInfoFeedTypes)
                }.sortedBy { it.downloadCount }.reversed()
            else
                account.localRelays.map {
                    val liveRelay = RelayPool.getRelay(it.url)

                    val errorCounter = liveRelay?.errorCounter ?: 0
                    val eventDownloadCounter = liveRelay?.eventDownloadCounter ?: 0
                    val eventUploadCounter = liveRelay?.eventUploadCounter ?: 0

                    Relay(it.url, it.read, it.write, errorCounter, eventDownloadCounter, eventUploadCounter, it.feedTypes)
                }.sortedBy { it.downloadCount }.reversed()
        }
    }

    fun addRelay(relay: Relay) {
        if (relays.value.any { it.url == relay.url }) return

        _relays.update {
            it.plus(relay)
        }
    }

    fun deleteRelay(relay: Relay) {
        _relays.update {
            it.minus(relay)
        }
    }

    fun toggleDownload(relay: Relay) {
        _relays.update {
            it.updated(relay, relay.copy(read = !relay.read))
        }
    }

    fun toggleUpload(relay: Relay) {
        _relays.update {
            it.updated(relay, relay.copy(write = !relay.write))
        }
    }

    fun toggleFollows(relay: Relay) {
        val newTypes = togglePresenceInSet(relay.feedTypes, FeedType.FOLLOWS)
        _relays.update {
            it.updated(relay, relay.copy(feedTypes = newTypes))
        }
    }

    fun toggleMessages(relay: Relay) {
        val newTypes = togglePresenceInSet(relay.feedTypes, FeedType.PRIVATE_DMS)
        _relays.update {
            it.updated(relay, relay.copy(feedTypes = newTypes))
        }
    }

    fun togglePublicChats(relay: Relay) {
        val newTypes = togglePresenceInSet(relay.feedTypes, FeedType.PUBLIC_CHATS)
        _relays.update {
            it.updated(relay, relay.copy(feedTypes = newTypes))
        }
    }

    fun toggleGlobal(relay: Relay) {
        val newTypes = togglePresenceInSet(relay.feedTypes, FeedType.GLOBAL)
        _relays.update {
            it.updated(relay, relay.copy( feedTypes = newTypes ))
        }
    }
}

fun <T> Iterable<T>.updated(old: T, new: T): List<T> = map { if (it == old) new else it }

fun <T> togglePresenceInSet(set: Set<T>, item: T): Set<T> {
    return if (set.contains(item)) set.minus(item) else set.plus(item)
}