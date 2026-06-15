package com.example.gymbuddy.data.model

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),   // uid → isim
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCounts: Map<String, Long> = emptyMap()          // uid → okunmamış sayısı
)
