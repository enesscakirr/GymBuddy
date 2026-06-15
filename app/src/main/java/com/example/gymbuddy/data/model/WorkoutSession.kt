package com.example.gymbuddy.data.model

/**
 * Firestore yapısı:
 *   workouts/{uid}/sessions/{sessionId}
 */

data class WorkoutSet(
    val setNumber: Int = 1,
    val weightKg: String = "",
    val reps: String = ""
)

data class WorkoutExercise(
    val id: String = "",
    val name: String = "",
    val sets: List<WorkoutSet> = listOf(WorkoutSet(1))
)

data class WorkoutSession(
    val id: String = "",
    val uid: String = "",
    val date: String = "",               // "2026-05-20"
    val dateTimestamp: Long = 0L,
    val exercises: List<WorkoutExercise> = emptyList(),
    val totalVolumeKg: Float = 0f,       // toplam kg × tekrar
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Antrenmanın adını otomatik üret (ilk egzersiz ismi veya "Antrenman") */
    fun displayName(): String =
        exercises.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: "Antrenman"

    /** Toplam set sayısı */
    fun totalSets(): Int = exercises.sumOf { it.sets.size }
}

/** Firestore'a seri hale getirmek için flat map */
fun WorkoutSession.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "uid" to uid,
    "date" to date,
    "dateTimestamp" to dateTimestamp,
    "totalVolumeKg" to totalVolumeKg,
    "createdAt" to createdAt,
    "exercises" to exercises.map { ex ->
        mapOf(
            "id" to ex.id,
            "name" to ex.name,
            "sets" to ex.sets.map { s ->
                mapOf(
                    "setNumber" to s.setNumber,
                    "weightKg" to s.weightKg,
                    "reps" to s.reps
                )
            }
        )
    }
)
