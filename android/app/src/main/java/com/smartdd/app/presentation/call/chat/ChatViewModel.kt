package com.smartdd.app.presentation.call.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.local.preferences.TokenManager
import com.smartdd.app.data.local.room.ChatEntity
import com.smartdd.app.data.remote.websocket.WebSocketClient
import com.smartdd.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketClient: WebSocketClient,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private var collectJob: kotlinx.coroutines.Job? = null

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _state.value = ChatUiState(isLoading = true)
            _state.value = ChatUiState(messages = chatRepository.getLocalMessages(sessionId))

            collectJob?.cancel()
            collectJob = viewModelScope.launch {
                webSocketClient.events.collect { msg ->
                    if (msg.type == "chat" && msg.sessionId == sessionId) {
                        chatRepository.persistIncomingMessage(sessionId, msg.message ?: "", msg.from ?: "", msg.userId)
                        _state.value = ChatUiState(messages = chatRepository.getLocalMessages(sessionId))
                    }
                }
            }
        }
    }

    fun sendMessage(sessionId: String, text: String) {
        val senderId = tokenManager.getUserId() ?: return
        val senderName = tokenManager.getUserName()
        viewModelScope.launch {
            chatRepository.persistOutgoingMessage(sessionId, text, senderId, senderName)
            chatRepository.sendMessageViaWS(sessionId, text, senderId, senderName)
            _state.value = ChatUiState(messages = chatRepository.getLocalMessages(sessionId))
        }
    }
}
