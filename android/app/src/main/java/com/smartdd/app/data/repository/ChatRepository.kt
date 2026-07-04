package com.smartdd.app.data.repository

import com.smartdd.app.data.local.room.ChatDao
import com.smartdd.app.data.local.room.ChatEntity
import com.smartdd.app.data.remote.websocket.WebSocketClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val chatDao: ChatDao
) {
    suspend fun getLocalMessages(sessionId: String): List<ChatEntity> =
        chatDao.getMessages(sessionId)

    fun sendMessageViaWS(sessionId: String, text: String, senderId: String, senderName: String?) {
        webSocketClient.send(mapOf(
            "type" to "chat",
            "sessionId" to sessionId,
            "message" to text,
            "senderId" to senderId,
            "senderName" to (senderName ?: "")
        ))
    }

    suspend fun persistOutgoingMessage(sessionId: String, text: String, senderId: String, senderName: String?) {
        chatDao.insert(ChatEntity(sessionId = sessionId, text = text, isMine = true, senderId = senderId, senderName = senderName))
    }

    suspend fun persistIncomingMessage(sessionId: String, text: String, senderId: String, senderName: String?) {
        chatDao.insert(ChatEntity(sessionId = sessionId, text = text, isMine = false, senderId = senderId, senderName = senderName))
    }
}
