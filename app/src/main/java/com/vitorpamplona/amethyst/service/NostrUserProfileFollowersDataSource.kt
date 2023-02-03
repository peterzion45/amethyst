package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.User
import nostr.postr.JsonFilter
import nostr.postr.events.ContactListEvent

object NostrUserProfileFollowersDataSource: NostrDataSource<User>("UserProfileFollowerFeed") {
  lateinit var account: Account
  var user: User? = null

  fun loadUserProfile(userId: String) {
    user = LocalCache.users[userId]
    resetFilters()
  }

  override fun feed(): List<User> {
    return user?.followers?.filter { account.isAcceptable(it) } ?: emptyList()
  }

  override fun updateChannelFilters() {}
}