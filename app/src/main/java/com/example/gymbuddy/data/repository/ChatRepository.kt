package com.example.gymbuddy.data.repository

import com.example.gymbuddy.data.model.ChatMessage
import com.example.gymbuddy.data.model.Conversation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore "conversations" koleksiyonu işlemleri.
 *
 * Yapı:
 *   conversations/{conversationId}          ← sohbet belgesi
 *   conversations/{conversationId}/messages ← mesajlar alt koleksiyonu
 *
 * Conversation ID = listOf(uid1, uid2).sorted().joinToString("_")
 */
class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val conversationsRef = firestore.collection("conversations")

    // ── Conversation ID oluştur ─────────────────────────────────────
    fun conversationId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")

    // ── Sohbet oluştur / var olanı getir ────────────────────────────
    suspend fun getOrCreateConversation(
        myUid: String,
        otherUid: String,
        myName: String,
        otherName: String
    ): Result<String> {
        val convId = conversationId(myUid, otherUid)
        return try {
            val doc = conversationsRef.document(convId).get().await()
            if (!doc.exists()) {
                val data = mapOf(
                    "id"                   to convId,
                    "participants"         to listOf(myUid, otherUid),
                    "participantNames"     to mapOf(myUid to myName, otherUid to otherName),
                    "lastMessage"          to "",
                    "lastMessageTime"      to 0L,
                    "lastMessageSenderId"  to "",
                    "unreadCounts"         to mapOf(myUid to 0L, otherUid to 0L)
                )
                conversationsRef.document(convId).set(data).await()
            }
            Result.success(convId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Kullanıcının tüm sohbetlerini dinle ──────────────────────────
    fun observeConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsRef
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Index yoksa veya başka hata olursa boş liste gönder, crash yapma
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    parseConversation(doc.data ?: return@mapNotNull null)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    // ── Sohbetteki mesajları dinle ───────────────────────────────────
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = conversationsRef
            .document(conversationId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ChatMessage(
                        id        = doc.id,
                        senderId  = data["senderId"] as? String ?: "",
                        text      = data["text"] as? String ?: "",
                        timestamp = data["timestamp"] as? Long ?: 0L,
                        isRead    = data["isRead"] as? Boolean ?: false
                    )
                }?.sortedBy { it.timestamp } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // ── Mesaj gönder ────────────────────────────────────────────────
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        otherUid: String
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            val messageData = mapOf(
                "senderId"  to senderId,
                "text"      to text,
                "timestamp" to timestamp,
                "isRead"    to false
            )
            conversationsRef
                .document(conversationId)
                .collection("messages")
                .add(messageData)
                .await()

            // Sohbet metadata güncelle
            conversationsRef.document(conversationId).update(
                mapOf(
                    "lastMessage"         to text,
                    "lastMessageTime"     to timestamp,
                    "lastMessageSenderId" to senderId,
                    "unreadCounts.$otherUid" to FieldValue.increment(1)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Okundu işaretle ─────────────────────────────────────────────
    suspend fun markAsRead(conversationId: String, uid: String): Result<Unit> {
        return try {
            conversationsRef.document(conversationId)
                .update("unreadCounts.$uid", 0L)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Karşı tarafın mesajlarını "okundu" yap ──────────────────────
    suspend fun markMessagesAsRead(conversationId: String, myUid: String) {
        try {
            val snapshot = conversationsRef
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            val batch = firestore.batch()
            snapshot.documents
                .filter { (it.data?.get("senderId") as? String) != myUid }
                .forEach { doc -> batch.update(doc.reference, "isRead", true) }
            batch.commit().await()
        } catch (_: Exception) { }
    }

    // ── Yardımcı: Firestore map → Conversation ───────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun parseConversation(data: Map<String, Any>): Conversation = Conversation(
        id                   = data["id"] as? String ?: "",
        participants         = data["participants"] as? List<String> ?: emptyList(),
        participantNames     = (data["participantNames"] as? Map<String, String>) ?: emptyMap(),
        lastMessage          = data["lastMessage"] as? String ?: "",
        lastMessageTime      = data["lastMessageTime"] as? Long ?: 0L,
        lastMessageSenderId  = data["lastMessageSenderId"] as? String ?: "",
        unreadCounts         = (data["unreadCounts"] as? Map<String, Long>) ?: emptyMap()
    )
}
