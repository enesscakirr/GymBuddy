package com.example.gymbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.model.ChatMessage
import com.example.gymbuddy.data.repository.ChatRepository
import com.example.gymbuddy.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI model: konuşma listesi satırı ────────────────────────────────

data class ConversationUiItem(
    val conversationId: String,
    val otherUid: String,
    val otherName: String,
    val otherInitials: String,
    val otherPhotoUrl: String = "",
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isOnline: Boolean = false,
    val hasStarted: Boolean = true   // false → henüz sohbet başlamamış arkadaş
)

// ── UI model: açık sohbet ────────────────────────────────────────────

data class ConversationDetail(
    val conversationId: String,
    val otherUid: String,
    val otherName: String,
    val otherInitials: String,
    val otherPhotoUrl: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoadingMessages: Boolean = true
)

// ── Ana UI state ─────────────────────────────────────────────────────

data class ChatUiState(
    val conversations: List<ConversationUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val openConversation: ConversationDetail? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentMyUid: String = ""
    private var currentMyName: String = ""

    // ── Konuşma listesini yükle ───────────────────────────────────────
    fun loadConversations(uid: String, myName: String = "") {
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        currentMyUid = uid
        if (myName.isNotBlank()) currentMyName = myName

        viewModelScope.launch {
            // 1) Arkadaş listesini al
            val friends = userRepository.getFriends(uid).getOrNull() ?: emptyList()

            // 2) Mevcut sohbetleri dinle
            chatRepository.observeConversations(uid)
                .catch { e ->
                    android.util.Log.e("ChatVM", "observeConversations error", e)
                    emit(emptyList())
                }
                .collect { conversations ->
                // Firestore'daki sohbetleri UI modeline çevir
                val existingItems = conversations.map { conv ->
                    val otherUid  = conv.participants.firstOrNull { it != uid } ?: ""
                    val otherName = conv.participantNames[otherUid] ?: "Kullanıcı"
                    val friend = friends.find { it.uid == otherUid }
                    ConversationUiItem(
                        conversationId  = conv.id,
                        otherUid        = otherUid,
                        otherName       = otherName,
                        otherInitials   = otherName.initials(),
                        otherPhotoUrl   = friend?.profilePhotoUrl ?: "",
                        lastMessage     = conv.lastMessage,
                        lastMessageTime = conv.lastMessageTime,
                        unreadCount     = (conv.unreadCounts[uid] ?: 0L).toInt(),
                        hasStarted      = conv.lastMessage.isNotBlank()
                    )
                }

                // 3) Henüz sohbet başlatmamış arkadaşlar
                val existingOtherUids = existingItems.map { it.otherUid }.toSet()
                val newFriendItems = friends
                    .filter { it.uid !in existingOtherUids }
                    .map { friend ->
                        ConversationUiItem(
                            conversationId  = chatRepository.conversationId(uid, friend.uid),
                            otherUid        = friend.uid,
                            otherName       = friend.fullName.ifBlank { "Kullanıcı" },
                            otherInitials   = friend.fullName.ifBlank { "K" }.initials(),
                            otherPhotoUrl   = friend.profilePhotoUrl,
                            lastMessage     = "Sohbeti başlat",
                            lastMessageTime = 0L,
                            unreadCount     = 0,
                            hasStarted      = false
                        )
                    }

                // Birleştir: mesajı olan üstte, yeni arkadaşlar altta
                val all = existingItems + newFriendItems
                _uiState.update { it.copy(conversations = all, isLoading = false) }
            }
        }
    }

    // ── Sohbet aç ────────────────────────────────────────────────────
    fun openConversation(item: ConversationUiItem, myUid: String) {
        _uiState.update {
            it.copy(
                openConversation = ConversationDetail(
                    conversationId    = item.conversationId,
                    otherUid          = item.otherUid,
                    otherName         = item.otherName,
                    otherInitials     = item.otherInitials,
                    otherPhotoUrl     = item.otherPhotoUrl,
                    isLoadingMessages = true
                )
            )
        }

        viewModelScope.launch {
            try {
                chatRepository.markAsRead(item.conversationId, myUid)
                chatRepository.markMessagesAsRead(item.conversationId, myUid)
            } catch (_: Exception) {}
            chatRepository.observeMessages(item.conversationId)
                .catch { e ->
                    android.util.Log.e("ChatVM", "observeMessages error", e)
                    emit(emptyList())
                }
                .collect { messages ->
                _uiState.update { state ->
                    state.copy(
                        openConversation = state.openConversation?.copy(
                            messages          = messages,
                            isLoadingMessages = false
                        )
                    )
                }
            }
        }
    }

    // ── Sohbeti kapat (listeye dön) ───────────────────────────────────
    fun closeConversation() {
        _uiState.update { it.copy(openConversation = null) }
    }

    // ── Mesaj gönder ────────────────────────────────────────────────
    fun sendMessage(myUid: String, text: String) {
        val conv    = _uiState.value.openConversation ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // Sohbet henüz yoksa Firestore'da oluştur
            chatRepository.getOrCreateConversation(
                myUid     = myUid,
                otherUid  = conv.otherUid,
                myName    = currentMyName,
                otherName = conv.otherName
            )
            chatRepository.sendMessage(conv.conversationId, myUid, trimmed, conv.otherUid)
        }
    }

    // ── Yardımcı ─────────────────────────────────────────────────────
    private fun String.initials(): String =
        split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
}
