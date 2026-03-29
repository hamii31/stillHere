package com.stillhere.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*
import com.stillhere.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DISPLAY_FORMAT = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("stillhere_prefs", Context.MODE_PRIVATE)

        scheduleAlertWorker()
        updateUI()

        binding.checkInButton.setOnClickListener { handleCheckIn() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun handleCheckIn() {
        val today = DATE_FORMAT.format(Date())
        val lastCheckIn = prefs.getString("last_check_in", null)

        if (lastCheckIn == today) {
            pulseButton()
            return
        }

        val yesterday = getYesterday()
        val currentStreak = prefs.getInt("streak", 0)
        val totalDays = prefs.getInt("total_days", 0)
        val newStreak = if (lastCheckIn == yesterday) currentStreak + 1 else 1
        val newTotal = totalDays + 1

        prefs.edit()
            .putString("last_check_in", today)
            .putInt("streak", newStreak)
            .putInt("total_days", newTotal)
            .apply()

        celebrateAnimation()
        updateUI()
    }

    private fun updateUI() {
        val today = DATE_FORMAT.format(Date())
        val lastCheckIn = prefs.getString("last_check_in", null)
        val streak = prefs.getInt("streak", 0)
        val totalDays = prefs.getInt("total_days", 0)
        val checkedInToday = lastCheckIn == today

        if (checkedInToday) {
            binding.checkInButton.text = "✓  Still Here"
            binding.checkInButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_checked)
            binding.statusText.text = "You checked in today. See you tomorrow."
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.text_success))
        } else {
            binding.checkInButton.text = "I'm Still Here"
            binding.checkInButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_default)
            binding.statusText.text = if (lastCheckIn == null)
                "Press the button to start your streak."
            else
                "Last seen: ${formatDisplayDate(lastCheckIn)}"
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        binding.streakCount.text = streak.toString()
        binding.totalCount.text = totalDays.toString()
        binding.streakLabel.text = if (streak == 1) "day streak" else "days streak"
        binding.totalLabel.text = if (totalDays == 1) "day total" else "days total"
        binding.statsCard.visibility = if (totalDays > 0) View.VISIBLE else View.INVISIBLE
    }

    private fun scheduleAlertWorker() {
        val request = PeriodicWorkRequestBuilder<AlertWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "stillhere_alert_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = DATE_FORMAT.parse(dateStr)
            if (date != null) DISPLAY_FORMAT.format(date) else dateStr
        } catch (e: Exception) { dateStr }
    }

    private fun getYesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return DATE_FORMAT.format(cal.time)
    }

    private fun celebrateAnimation() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.checkInButton, "scaleX", 1f, 1.15f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.checkInButton, "scaleY", 1f, 1.15f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.checkInButton, "scaleX", 1.15f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.checkInButton, "scaleY", 1.15f, 1f)
        val scaleUp = AnimatorSet().apply { playTogether(scaleUpX, scaleUpY); duration = 150 }
        val scaleDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 300
            interpolator = OvershootInterpolator(3f)
        }
        AnimatorSet().apply { playSequentially(scaleUp, scaleDown); start() }

        binding.streakCount.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
            .withEndAction {
                binding.streakCount.animate().scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }.start()
    }

    private fun pulseButton() {
        binding.checkInButton.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction {
                binding.checkInButton.animate().scaleX(1f).scaleY(1f).setDuration(120)
                    .setInterpolator(OvershootInterpolator(4f)).start()
            }.start()
    }
}
