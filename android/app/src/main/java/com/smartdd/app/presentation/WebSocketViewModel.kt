package com.smartdd.app.presentation

import androidx.lifecycle.ViewModel
import com.smartdd.app.data.remote.websocket.WebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebSocketViewModel @Inject constructor(
    val webSocketClient: WebSocketClient
) : ViewModel() {
    init { webSocketClient.connect() }
    override fun onCleared() { webSocketClient.disconnect() }
}
