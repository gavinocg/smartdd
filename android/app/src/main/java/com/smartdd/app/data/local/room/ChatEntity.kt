package com.smartdd.app.data.local.room

import androidx.room.*

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val text: String,
    @ColumnInfo(name = "is_mine") val isMine: Boolean,
    @ColumnInfo(name = "sender_id") val senderId: String? = null,
    @ColumnInfo(name = "sender_name") val senderName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: String): List<ChatEntity>

    @Insert
    suspend fun insert(message: ChatEntity)

    @Insert
    suspend fun insertAll(messages: List<ChatEntity>)

    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
