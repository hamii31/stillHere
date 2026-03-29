package com.stillhere.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class AlertWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "stillhere_alerts"
        const val NOTIF_ID_REMINDER = 1001
        const val NOTIF_ID_CONTACT = 1002
        const val NOTIF_ID_CRISIS = 1003
        const val NOTIF_ID_112 = 1004

        // Belgian crisis line
        const val CRISIS_LINE = "0800 32 123"
        const val CRISIS_LINE_DIAL = "080032123"
    }

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("stillhere_prefs", Context.MODE_PRIVATE)
        val lastCheckIn = prefs.getString("last_check_in", null) ?: return Result.success()

        val daysMissed = daysSince(lastCheckIn)
        if (daysMissed <= 0) return Result.success()

        val contactName = prefs.getString("contact_name", "").orEmpty()
        val contactNumber = prefs.getString("contact_number", "").orEmpty()
        val gracePeriod = prefs.getInt("grace_period_days", 3)
        val enable112 = prefs.getBoolean("enable_112", false)

        createNotificationChannel()

        when {
            // Tier 1 — grace period hit: notify personal contact
            daysMissed == gracePeriod && contactNumber.isNotEmpty() -> {
                sendSms(
                    contactNumber,
                    "Hi${if (contactName.isNotEmpty()) " $contactName" else ""}. " +
                    "This is an automated message from the Still Here app. " +
                    "Your contact hasn't checked in for $daysMissed days. " +
                    "Please reach out to them when you can."
                )
                showNotification(
                    NOTIF_ID_CONTACT,
                    "Alert sent to your contact",
                    "${if (contactName.isNotEmpty()) contactName else "Your contact"} has been notified that you haven't checked in."
                )
            }

            // Tier 2 — 7 days: SMS to crisis line + prominent notification
            daysMissed == 7 -> {
                sendSms(
                    CRISIS_LINE_DIAL,
                    "Still Here app alert: A user has not checked in for 7 days. " +
                    "This may require a wellness check."
                )
                showCrisisNotification(daysMissed)
            }

            // Tier 3 — 10 days: 112 alert (only if opted in)
            daysMissed == 10 && enable112 -> {
                showEmergencyNotification()
                // Also re-alert personal contact if available
                if (contactNumber.isNotEmpty()) {
                    sendSms(
                        contactNumber,
                        "URGENT — Still Here app: Your contact has not checked in for 10 days. " +
                        "Emergency services may be contacted. Please check on them immediately."
                    )
                }
            }

            // Daily gentle reminder to user after 2 days
            daysMissed == 2 -> {
                showNotification(
                    NOTIF_ID_REMINDER,
                    "Still Here",
                    "You haven't checked in for 2 days. We're thinking of you."
                )
            }
        }

        return Result.success()
    }

    private fun daysSince(dateStr: String): Int {
        return try {
            val last = DATE_FORMAT.parse(dateStr) ?: return 0
            val now = Calendar.getInstance().time
            val diff = now.time - last.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) { 0 }
    }

    private fun sendSms(number: String, message: String) {
        try {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applicationContext.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(number, null, message, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        }
    }

    private fun showCrisisNotification(days: Int) {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$CRISIS_LINE_DIAL")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "You haven't checked in for $days days. " +
            "The Belgian Crisis Line ($CRISIS_LINE) has been notified. " +
            "Tap to call them directly — they're available 24/7."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("We haven't heard from you")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Call $CRISIS_LINE", pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID_CRISIS, notification)
        }
    }

    private fun showEmergencyNotification() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:112")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "10 days without a check-in. Emergency services (112) have been alerted. " +
            "If you are safe, please open Still Here and check in now."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Emergency alert sent")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Call 112", pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID_112, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Still Here Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when check-ins are missed"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
