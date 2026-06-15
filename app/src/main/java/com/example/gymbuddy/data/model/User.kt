package com.example.gymbuddy.data.model

/**
 * Firestore "users" koleksiyonundaki kullanıcı belgesi.
 * Document ID = Firebase Auth UID
 */
data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val age: String = "",
    val height: String = "",
    val weight: Float = 0f,
    val address: String = "",
    val fitnessFocus: String = "",
    val profilePhotoUrl: String = "",
    val badge: String = "NEWBIE",           // NEWBIE, PRO, ELITE
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val following: List<String> = emptyList(),  // takip edilen UID'ler
    val followers: List<String> = emptyList(),  // takipçi UID'ler
    val totalWorkouts: Int = 0,
    val isProfileComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Konum (Connect ekranı)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastSeen: Long = 0L,          // epoch ms — konum ne zaman güncellendi
    val locationVisible: Boolean = true,   // kullanıcı görünür olmak istemiyor olabilir
    val profileVisibility: String = "public",  // "public" | "friends" | "private"
    // Spor salonu
    val gymName: String = "",
    val gymLatitude: Double = 0.0,
    val gymLongitude: Double = 0.0,
    // Hedefler
    val targetWorkoutsPerWeek: Int = 0,    // haftalık antrenman hedefi (0 = belirlenmemiş)
    val targetWeightKg: Float = 0f         // kilo hedefi (0 = belirlenmemiş)
)
