package com.example.gymbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.model.WorkoutExercise
import com.example.gymbuddy.data.model.WorkoutSession
import com.example.gymbuddy.data.model.WorkoutSet
import com.example.gymbuddy.data.repository.WorkoutRepository
import com.example.gymbuddy.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ── Draft egzersiz state (UI için) ──────────────────────────────────

data class DraftSet(
    val setNumber: Int = 1,
    val weightKg: String = "",
    val reps: String = ""
)

data class DraftExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sets: List<DraftSet> = listOf(DraftSet(1))
)

// ── UI State ─────────────────────────────────────────────────────────

data class WorkoutUiState(
    val sessions: List<WorkoutSession> = emptyList(),
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    /** Hangi tarihlerde antrenman var (String set: "2026-05-20") */
    val workoutDates: Set<String> get() = sessions.map { it.date }.toSet()

    /** Mevcut streak (bugünden geriye kaç gün üst üste antrenman) */
    val currentStreak: Int get() {
        if (workoutDates.isEmpty()) return 0
        var streak = 0
        var checkDate = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        while (workoutDates.contains(checkDate.format(fmt))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    /** Bugünkü en ağır tek set (kg cinsinden) */
    val prTodayKg: Float? get() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todaySessions = sessions.filter { it.date == today }
        val maxKg = todaySessions
            .flatMap { it.exercises }
            .flatMap { it.sets }
            .mapNotNull { it.weightKg.toFloatOrNull() }
            .maxOrNull()
        return maxKg
    }

    /** Toplam antrenman sayısı */
    val totalWorkouts: Int get() = sessions.size
}

// ── ViewModel ─────────────────────────────────────────────────────────

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    // ── Kullanıcının workout'larını dinle ────────────────────────────
    fun startObserving(uid: String) {
        if (currentUid == uid) return   // zaten dinleniyor
        currentUid = uid

        viewModelScope.launch {
            workoutRepository.observeWorkouts(uid).collect { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoaded = true) }
            }
        }
    }

    // ── Workout Kaydet ────────────────────────────────────────────────
    fun saveWorkout(
        uid: String,
        date: LocalDate,
        draftExercises: List<DraftExercise>
    ) {
        // Boş egzersizleri filtrele
        val validExercises = draftExercises
            .filter { it.name.isNotBlank() }
            .map { draft ->
                WorkoutExercise(
                    id = draft.id,
                    name = draft.name.trim(),
                    sets = draft.sets
                        .filter { it.weightKg.isNotBlank() || it.reps.isNotBlank() }
                        .map { s ->
                            WorkoutSet(
                                setNumber = s.setNumber,
                                weightKg = s.weightKg,
                                reps = s.reps
                            )
                        }
                        .ifEmpty {
                            listOf(WorkoutSet(1, draft.sets.firstOrNull()?.weightKg ?: "", draft.sets.firstOrNull()?.reps ?: ""))
                        }
                )
            }

        if (validExercises.isEmpty()) {
            _uiState.update { it.copy(error = "En az bir egzersiz adı gir") }
            return
        }

        // Toplam hacim hesapla
        val totalVolume = validExercises
            .flatMap { it.sets }
            .sumOf { s ->
                val kg = s.weightKg.toFloatOrNull() ?: 0f
                val reps = s.reps.toIntOrNull() ?: 0
                (kg * reps).toDouble()
            }
            .toFloat()

        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            uid = uid,
            date = dateStr,
            dateTimestamp = date.toEpochDay() * 86_400_000L,
            exercises = validExercises,
            totalVolumeKg = totalVolume
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false, error = null) }

            workoutRepository.saveWorkout(session)
                .onSuccess {
                    // Firestore'daki totalWorkouts alanını artır
                    val newTotal = (_uiState.value.sessions.size + 1)
                    userRepository.updateUserFields(
                        uid,
                        mapOf("totalWorkouts" to newTotal)
                    )
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Kaydedilemedi: ${e.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
