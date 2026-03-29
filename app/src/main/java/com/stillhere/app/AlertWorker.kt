package com.stillhere.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
            // Tier 1 — grace period: prompt user to SMS personal contact
            daysMissed == gracePeriod && contactNumber.isNotEmpty() -> {
                val message = "Hi${if (contactName.isNotEmpty()) " $contactName" else ""}. " +
                    "This is a message from the Still Here app. " +
                    "Your contact hasn't checked in for $daysMissed days. " +
                    "Please reach out to them when you can."
                showSmsPromptNotification(
                    id = NOTIF_ID_CONTACT,
                    title = "Tap to alert your contact",
                    body = "${if (contactName.isNotEmpty()) contactName else "Your contact"} hasn't heard from you in $daysMissed days. Tap to send them a message.",
                    number = contactNumber,
                    smsMessage = message
                )
            }

            // Tier 2 — 7 days: prompt to contact crisis line
            daysMissed == 7 -> {
                showCrisisNotification(daysMissed)
            }

            // Tier 3 — 10 days: prompt to call 112 (opt-in only)
            daysMissed == 10 && enable112 -> {
                showEmergencyNotification()
                if (contactNumber.isNotEmpty()) {
                    val urgentMessage = "URGENT — Still Here app: Your contact has not checked in for 10 days. Please check on them immediately."
                    showSmsPromptNotification(
                        id = NOTIF_ID_CONTACT + 1,
                        title = "Alert your contact urgently",
                        body = "Tap to send an urgent SMS to ${if (contactName.isNotEmpty()) contactName else "your contact"}.",
                        number = contactNumber,
                        smsMessage = urgentMessage
                    )
                }
            }

            // Gentle reminder at day 2
            daysMissed == 2 -> {
                showSimpleNotification(
                    id = NOTIF_ID_REMINDER,
                    title = "Still Here",
                    body = "You haven't checked in for 2 days. We're thinking of you."
                )
            }
        }

        return Result.success()
    }

    private fun daysSince(dateStr: String): Int {
        return try {
            val last = DATE_FORMAT.parse(dateStr) ?: return 0
            val diff = Calendar.getInstance().time.time - last.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) { 0 }
    }

    /**
     * Shows a notification that, when tapped, opens the SMS app
     * with the recipient and message pre-filled. No SEND_SMS permission needed.
     */
    private fun showSmsPromptNotification(
        id: Int,
        title: String,
        body: String,
        number: String,
        smsMessage: String
    ) {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", smsMessage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "Send SMS", pendingIntent)
            .setAutoCancel(true)
            .build()

        postNotification(id, notification)
    }

    private fun showSimpleNotification(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        postNotification(id, notification)
    }

    private fun showCrisisNotification(days: Int) {
        // Action 1: call the crisis line
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$CRISIS_LINE_DIAL")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPending = PendingIntent.getActivity(
            applicationContext, NOTIF_ID_CRISIS, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: open SMS to crisis line with pre-filled message
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$CRISIS_LINE_DIAL")
            putExtra("sms_body",
                "Still Here app: A user has not checked in for $days days. This may require a wellness check.")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val smsPending = PendingIntent.getActivity(
            applicationContext, NOTIF_ID_CRISIS + 1, smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "You haven't checked in for $days days. " +
            "Tap to call the Belgian Crisis Line ($CRISIS_LINE) — available 24/7, free, confidential."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("We haven't heard from you")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(callPending)
            .addAction(android.R.drawable.ic_menu_call, "Call $CRISIS_LINE", callPending)
            .addAction(android.R.drawable.ic_menu_send, "Send SMS alert", smsPending)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        postNotification(NOTIF_ID_CRISIS, notification)
    }

    private fun showEmergencyNotification() {
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:112")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPending = PendingIntent.getActivity(
            applicationContext, NOTIF_ID_112, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "10 days without a check-in. Tap to call emergency services (112). " +
            "If you are safe, open Still Here and check in now."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Emergency — please respond")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(callPending)
            .addAction(android.R.drawable.ic_menu_call, "Call 112", callPending)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        postNotification(NOTIF_ID_112, notification)
    }

    private fun postNotification(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Still Here Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when check-ins are missed" }
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
