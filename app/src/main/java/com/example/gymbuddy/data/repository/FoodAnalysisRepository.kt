package com.example.gymbuddy.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

private const val TAG = "FoodAnalysis"

// ── Besin verisi ─────────────────────────────────────────────────────

data class DetectedFood(
    val name: String,
    val grams: Int,
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val saturatedFat: Float = 0f,
    val sodium: Int = 0,
    val cholesterol: Int = 0,
    val potassium: Int = 0
)

data class FoodNutrition(
    val foodName: String,
    val servingSize: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val confidence: Float,
    val sugar: Float = 0f,
    val saturatedFat: Float = 0f,
    val sodium: Int = 0,
    val cholesterol: Int = 0,
    val potassium: Int = 0,
    val detectedFoods: List<DetectedFood> = emptyList()
)

// ── Repository ───────────────────────────────────────────────────────

class FoodAnalysisRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1")
) {

    suspend fun analyzeFood(context: Context, imageUri: Uri): Result<FoodNutrition> {
        return try {

            val currentUser = FirebaseAuth.getInstance().currentUser
            Log.d(TAG, "Auth: kullanıcı=${currentUser?.uid ?: "NULL"}")
            if (currentUser == null) {
                return Result.failure(Exception("Kullanıcı oturumu yok"))
            }

            Log.d(TAG, "Görsel dönüştürülüyor: uri=$imageUri")
            val base64 = uriToBase64Jpeg(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Görsel okunamadı"))
            }
            Log.d(TAG, "Base64 uzunluğu: ${base64.length}")

            val data = mapOf("imageBase64" to base64, "mimeType" to "image/jpeg")

            val result = functions
                .getHttpsCallable("analyzeFood")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any>
            Log.d(TAG, "Yanıt: $map")

            if (map == null) {
                return Result.failure(Exception("Geçersiz yanıt"))
            }
            if (map.containsKey("error")) {
                return Result.failure(Exception(map["error"] as? String ?: "Analiz başarısız"))
            }

            // foods listesini parse et — artık her besin için detaylı veri var
            @Suppress("UNCHECKED_CAST")
            val foodsList = (map["foods"] as? List<Map<String, Any>>)?.map { f ->
                DetectedFood(
                    name         = f["name"]         as? String ?: "Bilinmeyen",
                    grams        = (f["grams"]        as? Number)?.toInt()   ?: 100,
                    calories     = (f["calories"]     as? Number)?.toInt()   ?: 0,
                    protein      = (f["protein"]      as? Number)?.toFloat() ?: 0f,
                    carbs        = (f["carbs"]        as? Number)?.toFloat() ?: 0f,
                    fat          = (f["fat"]          as? Number)?.toFloat() ?: 0f,
                    fiber        = (f["fiber"]        as? Number)?.toFloat() ?: 0f,
                    sugar        = (f["sugar"]        as? Number)?.toFloat() ?: 0f,
                    saturatedFat = (f["saturatedFat"] as? Number)?.toFloat() ?: 0f,
                    sodium       = (f["sodium"]       as? Number)?.toInt()   ?: 0,
                    cholesterol  = (f["cholesterol"]  as? Number)?.toInt()   ?: 0,
                    potassium    = (f["potassium"]    as? Number)?.toInt()   ?: 0,
                )
            } ?: emptyList()

            Result.success(
                FoodNutrition(
                    foodName      = map["foodName"]     as? String ?: "Bilinmeyen",
                    servingSize   = map["servingSize"]  as? String ?: "1 porsiyon",
                    calories      = (map["calories"]    as? Number)?.toInt()   ?: 0,
                    protein       = (map["protein"]     as? Number)?.toFloat() ?: 0f,
                    carbs         = (map["carbs"]       as? Number)?.toFloat() ?: 0f,
                    fat           = (map["fat"]         as? Number)?.toFloat() ?: 0f,
                    fiber         = (map["fiber"]       as? Number)?.toFloat() ?: 0f,
                    confidence    = (map["confidence"]  as? Number)?.toFloat() ?: 0.5f,
                    sugar         = (map["sugar"]       as? Number)?.toFloat() ?: 0f,
                    saturatedFat  = (map["saturatedFat"]as? Number)?.toFloat() ?: 0f,
                    sodium        = (map["sodium"]      as? Number)?.toInt()   ?: 0,
                    cholesterol   = (map["cholesterol"] as? Number)?.toInt()   ?: 0,
                    potassium     = (map["potassium"]   as? Number)?.toInt()   ?: 0,
                    detectedFoods = foodsList,
                )
            )

        } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
            Log.e(TAG, "FUNCTIONS HATA — kod=${e.code} mesaj=${e.message} detay=${e.details}", e)
            Result.failure(Exception("[${e.code}] ${e.message}"))

        } catch (e: Exception) {
            Log.e(TAG, "GENEL HATA — ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(Exception("Hata: ${e.localizedMessage}"))
        }
    }

    private fun uriToBase64Jpeg(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (original == null) return null

            val resized = resizeBitmap(original, 1024)

            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
            val bytes = out.toByteArray()
            Log.d(TAG, "JPEG boyutu: ${bytes.size / 1024} KB")

            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "uriToBase64Jpeg hata: ${e.message}", e)
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
