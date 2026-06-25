package com.example.gymbuddy.data.repository

import android.net.Uri
import com.example.gymbuddy.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// ── Takip İsteği Modeli ──────────────────────────────────────────────
data class FollowRequest(
    val fromUid: String = "",
    val toUid: String = "",
    val timestamp: Long = 0L
)

/**
 * Firestore "users" koleksiyonu ve Firebase Storage işlemleri.
 */
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    // ── Get User ────────────────────────────────────────────────────
    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val user = doc.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Create / Update User ────────────────────────────────────────
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update specific fields ──────────────────────────────────────
    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Check if profile is complete ────────────────────────────────
    suspend fun isProfileComplete(uid: String): Boolean {
        return try {
            val doc = usersCollection.document(uid).get().await()
            val result = doc.getBoolean("isProfileComplete") ?: false
            android.util.Log.d("UserRepo", "isProfileComplete uid=$uid exists=${doc.exists()} result=$result")
            result
        } catch (e: Exception) {
            android.util.Log.e("UserRepo", "isProfileComplete HATA: ${e.message}", e)
            false
        }
    }

    // ── Upload Profile Photo ────────────────────────────────────────
    suspend fun uploadProfilePhoto(uid: String, imageUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("profile_photos/$uid.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            // Firestore'daki profil URL'ini güncelle
            usersCollection.document(uid)
                .update("profilePhotoUrl", downloadUrl)
                .await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Konum Güncelle ──────────────────────────────────────────────
    suspend fun updateLocation(uid: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Spor Salonu Kaydet ──────────────────────────────────────────
    suspend fun saveGym(uid: String, gymName: String, lat: Double, lng: Double): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                mapOf(
                    "gymName" to gymName,
                    "gymLatitude" to lat,
                    "gymLongitude" to lng
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Yakın Kullanıcıları Getir (salon konumuna göre) ─────────────
    // Salon koordinatları etrafında ±radiusKm arar.
    suspend fun getNearbyUsers(
        currentUid: String,
        gymLat: Double,
        gymLng: Double,
        radiusKm: Double
    ): Result<List<User>> {
        return try {
            val latDelta = radiusKm / 111.0          // 1° lat ≈ 111 km
            val minLat = gymLat - latDelta
            val maxLat = gymLat + latDelta

            val snapshot = usersCollection
                .whereGreaterThanOrEqualTo("gymLatitude", minLat)
                .whereLessThanOrEqualTo("gymLatitude", maxLat)
                .whereEqualTo("locationVisible", true)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .filter { user ->
                    user.uid != currentUid &&
                    user.gymName.isNotBlank() &&
                    haversineKm(gymLat, gymLng, user.gymLatitude, user.gymLongitude) <= radiusKm
                }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TAKIP İSTEĞİ SİSTEMİ
    // ════════════════════════════════════════════════════════════════

    private val requestsCollection = firestore.collection("followRequests")

    // ── Takip İsteği Gönder ─────────────────────────────────────────
    suspend fun sendFollowRequest(fromUid: String, toUid: String): Result<Unit> {
        return try {
            val docId = "${fromUid}_${toUid}"
            val data = mapOf(
                "fromUid" to fromUid,
                "toUid" to toUid,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )
            requestsCollection.document(docId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Takip İsteğini Kabul Et ─────────────────────────────────────
    suspend fun acceptFollowRequest(fromUid: String, toUid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            // İstek durumunu güncelle
            val reqDoc = requestsCollection.document("${fromUid}_${toUid}")
            batch.update(reqDoc, "status", "accepted")

            // Karşılıklı takip — her iki kullanıcı da birbirini takip etsin
            batch.update(usersCollection.document(toUid),
                "following", FieldValue.arrayUnion(fromUid),
                "followers", FieldValue.arrayUnion(fromUid),
                "followingCount", FieldValue.increment(1),
                "followersCount", FieldValue.increment(1)
            )
            batch.update(usersCollection.document(fromUid),
                "following", FieldValue.arrayUnion(toUid),
                "followers", FieldValue.arrayUnion(toUid),
                "followingCount", FieldValue.increment(1),
                "followersCount", FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Takip İsteğini Reddet ───────────────────────────────────────
    suspend fun rejectFollowRequest(fromUid: String, toUid: String): Result<Unit> {
        return try {
            requestsCollection.document("${fromUid}_${toUid}")
                .update("status", "rejected")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Bana gelen bekleyen istekler ────────────────────────────────
    suspend fun getPendingRequests(myUid: String): Result<List<FollowRequest>> {
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "pending")
                .get().await()
            val requests = snapshot.documents.mapNotNull { doc ->
                val fromUid = doc.getString("fromUid") ?: return@mapNotNull null
                val ts = doc.getLong("timestamp") ?: 0L
                FollowRequest(fromUid = fromUid, toUid = myUid, timestamp = ts)
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Benim gönderdiğim bekleyen istekler ─────────────────────────
    suspend fun getSentPendingRequests(myUid: String): Result<Set<String>> {
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", "pending")
                .get().await()
            val uids = snapshot.documents.mapNotNull { it.getString("toUid") }.toSet()
            Result.success(uids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Arkadaşlar (karşılıklı takip) ───────────────────────────────
    suspend fun getFriends(uid: String): Result<List<User>> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val followingList = doc.get("following") as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val followersList = doc.get("followers") as? List<String> ?: emptyList()

            // Karşılıklı olanlar
            val mutualUids = followingList.intersect(followersList.toSet())
            if (mutualUids.isEmpty()) return Result.success(emptyList())

            // Firestore "in" query max 30
            val friends = mutualUids.chunked(30).flatMap { chunk ->
                usersCollection.whereIn("uid", chunk).get().await()
                    .documents.mapNotNull { it.toObject(User::class.java) }
            }
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Takip İsteğini İptal Et ─────────────────────────────────────
    suspend fun cancelFollowRequest(fromUid: String, toUid: String): Result<Unit> {
        return try {
            requestsCollection.document("${fromUid}_${toUid}").delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Arkadaşlıktan Çık ──────────────────────────────────────────
    suspend fun unfriend(myUid: String, targetUid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.update(usersCollection.document(myUid),
                "following", FieldValue.arrayRemove(targetUid),
                "followers", FieldValue.arrayRemove(targetUid),
                "followingCount", FieldValue.increment(-1),
                "followersCount", FieldValue.increment(-1)
            )
            batch.update(usersCollection.document(targetUid),
                "following", FieldValue.arrayRemove(myUid),
                "followers", FieldValue.arrayRemove(myUid),
                "followingCount", FieldValue.increment(-1),
                "followersCount", FieldValue.increment(-1)
            )
            // İstek dokümanlarını da sil
            batch.delete(requestsCollection.document("${myUid}_${targetUid}"))
            batch.delete(requestsCollection.document("${targetUid}_${myUid}"))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Takip Edilen UID'leri Getir ─────────────────────────────────
    suspend fun getFollowing(uid: String): Result<Set<String>> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("following") as? List<String> ?: emptyList()
            Result.success(list.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Upload Meal Photo ───────────────────────────────────────────
    suspend fun uploadMealPhoto(uid: String, imageUri: Uri): Result<String> {
        return try {
            val fileName = "meal_${System.currentTimeMillis()}.jpg"
            val ref = storage.reference.child("meal_photos/$uid/$fileName")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ── Delete User ─────────────────────────────────────────────────
    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Haversine mesafe (km) ────────────────────────────────────────────
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
