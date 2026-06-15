package com.example.gymbuddy.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gymbuddy.MainActivity
import com.example.gymbuddy.R
import java.util.Calendar

object NotificationHelper {

    private const val CHANNEL_WORKOUT  = "workout_reminder"
    private const val CHANNEL_SOCIAL   = "social_notifications"
    private const val ALARM_REQUEST    = 1001

    // ── Kanalları oluştur (uygulama açılışında çağır) ───────────────
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val workoutChannel = NotificationChannel(
            CHANNEL_WORKOUT,
            "Antrenman Hatırlatıcısı",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Günlük antrenman hatırlatma bildirimleri"
        }

        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL,
            "Sosyal Bildirimler",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Arkadaş istekleri ve seri uyarıları"
        }

        nm.createNotificationChannel(workoutChannel)
        nm.createNotificationChannel(socialChannel)
    }

    // ── Günlük hatırlatıcı planla ───────────────────────────────────
    fun scheduleWorkoutReminder(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Geçmişse yarına al
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Exact alarm izni kontrol
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Exact izin yoksa inexact kullan
            am.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pi
            )
        } else {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pi
            )
        }
    }

    // ── Hatırlatıcıyı iptal et ──────────────────────────────────────
    fun cancelWorkoutReminder(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    // ── Bildirim göster ─────────────────────────────────────────────
    fun showWorkoutReminder(context: Context) {
        if (!hasNotifPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Antrenman Zamanı!")
            .setContentText("Bugün antrenman yapmayı unutma. Hedeflerine bir adım daha yaklaş!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2001, notif)
    }

    fun showStreakWarning(context: Context, streak: Int) {
        if (!hasNotifPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Serin kırılmasın!")
            .setContentText("$streak günlük serin devam ediyor. Bugün antrenman yapmazsan sıfırlanacak!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2002, notif)
    }

    fun showFollowRequest(context: Context, fromName: String) {
        if (!hasNotifPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Yeni Takip İsteği")
            .setContentText("$fromName sana takip isteği gönderdi")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            (fromName.hashCode() and 0x7FFFFFFF) + 3000,
            notif
        )
    }

    private fun hasNotifPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
