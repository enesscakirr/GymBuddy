package com.example.gymbuddy.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// ── Öğün Tipi ───────────────────────────────────────────────────────

enum class MealType(val label: String) {
    BREAKFAST("Kahvaltı"),
    LUNCH("Öğle Yemeği"),
    DINNER("Akşam Yemeği"),
    SNACK("Ara Öğün");
}

// ── Firestore Modeli ────────────────────────────────────────────────

data class MealEntry(
    val id: String = "",
    val uid: String = "",
    val mealName: String = "",
    val mealType: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val photoUrl: String = "",
    val foodNames: List<String> = emptyList(),
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val saturatedFat: Float = 0f,
    val sodium: Int = 0,
    val cholesterol: Int = 0,
    val potassium: Int = 0,
    val confidence: Float = 0f
)

// ── Repository ──────────────────────────────────────────────────────

class MealRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val mealsCollection = firestore.collection("meals")

    /**
     * adjustedFoods: Kullanıcının gramajı düzelttiği besinler.
     * Besin değerleri orantılı olarak yeniden hesaplanır.
     */
    suspend fun saveMeal(
        imageUri: Uri?,
        mealName: String,
        mealType: MealType,
        date: String,
        adjustedFoods: List<DetectedFood>
    ): Result<MealEntry> {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Oturum açılmamış"))

            var photoUrl = ""
            if (imageUri != null) {
                val fileName = "meal_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child("meal_photos/$uid/$fileName")
                ref.putFile(imageUri).await()
                photoUrl = ref.downloadUrl.await().toString()
            }

            // Düzeltilmiş besinlerden toplamları hesapla
            val totals = adjustedFoods.fold(Totals()) { acc, f ->
                Totals(
                    calories     = acc.calories + f.calories,
                    protein      = acc.protein + f.protein,
                    carbs        = acc.carbs + f.carbs,
                    fat          = acc.fat + f.fat,
                    fiber        = acc.fiber + f.fiber,
                    sugar        = acc.sugar + f.sugar,
                    saturatedFat = acc.saturatedFat + f.saturatedFat,
                    sodium       = acc.sodium + f.sodium,
                    cholesterol  = acc.cholesterol + f.cholesterol,
                    potassium    = acc.potassium + f.potassium,
                )
            }

            val docRef = mealsCollection.document()
            val entry = MealEntry(
                id           = docRef.id,
                uid          = uid,
                mealName     = mealName,
                mealType     = mealType.name,
                date         = date,
                timestamp    = System.currentTimeMillis(),
                photoUrl     = photoUrl,
                foodNames    = adjustedFoods.map { "${it.name} (${it.grams}g)" },
                calories     = totals.calories,
                protein      = totals.protein,
                carbs        = totals.carbs,
                fat          = totals.fat,
                fiber        = totals.fiber,
                sugar        = totals.sugar,
                saturatedFat = totals.saturatedFat,
                sodium       = totals.sodium,
                cholesterol  = totals.cholesterol,
                potassium    = totals.potassium,
            )

            docRef.set(entry).await()
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllMeals(): Result<List<MealEntry>> {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Oturum açılmamış"))

            val snapshot = mealsCollection
                .whereEqualTo("uid", uid)
                .get()
                .await()

            val meals = snapshot.documents
                .mapNotNull { it.toObject(MealEntry::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(meals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMealsForDate(date: String): Result<List<MealEntry>> {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("Oturum açılmamış"))

            val snapshot = mealsCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("date", date)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val meals = snapshot.documents.mapNotNull { it.toObject(MealEntry::class.java) }
            Result.success(meals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private data class Totals(
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val saturatedFat: Float = 0f,
    val sodium: Int = 0,
    val cholesterol: Int = 0,
    val potassium: Int = 0,
)
