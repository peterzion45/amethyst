package com.vitorpamplona.amethyst.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.ServiceManager
import com.vitorpamplona.amethyst.model.Account
import fr.acinq.secp256k1.Hex
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nostr.postr.Persona
import nostr.postr.bechToBytes

class AccountStateViewModel(private val localPreferences: LocalPreferences): ViewModel() {
  private val _accountContent = MutableStateFlow<AccountState>(AccountState.LoggedOff)
  val accountContent = _accountContent.asStateFlow()

  init {
    // pulls account from storage.
    localPreferences.loadFromEncryptedStorage()?.let {
      login(it)
    }
  }

  fun login(key: String) {
    val pattern = Pattern.compile(".+@.+\\.[a-z]+")

    val account =
      if (key.startsWith("nsec")) {
        Account(Persona(privKey = key.bechToBytes()))
      } else if (key.startsWith("npub")) {
        Account(Persona(pubKey = key.bechToBytes()))
      } else if (pattern.matcher(key).matches()) {
        // Evaluate NIP-5
        Account(Persona())
      } else {
        Account(Persona(Hex.decode(key)))
      }

    localPreferences.saveToEncryptedStorage(account)

    login(account)
  }

  fun newKey() {
    val account = Account(Persona())
    localPreferences.saveToEncryptedStorage(account)
    login(account)
  }

  fun login(account: Account) {
    if (account.loggedIn.privKey != null)
      _accountContent.update { AccountState.LoggedIn ( account ) }
    else
      _accountContent.update { AccountState.LoggedInViewOnly ( account ) }

    viewModelScope.launch(Dispatchers.IO) {
      ServiceManager.start(account)
    }
  }

  fun logOff() {
    _accountContent.update { AccountState.LoggedOff }

    localPreferences.clearEncryptedStorage()
  }
}