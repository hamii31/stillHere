package com.stillhere.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stillhere.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("stillhere_prefs", Context.MODE_PRIVATE)

        loadSettings()

        binding.backButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { saveSettings() }

        binding.enable112Toggle.setOnCheckedChangeListener { _, isChecked ->
            binding.warningText.visibility = if (isChecked)
                android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun loadSettings() {
        binding.contactNameField.setText(prefs.getString("contact_name", ""))
        binding.contactNumberField.setText(prefs.getString("contact_number", ""))
        binding.gracePeriodSlider.value = prefs.getInt("grace_period_days", 3).toFloat()
        binding.enable112Toggle.isChecked = prefs.getBoolean("enable_112", false)
        updateGracePeriodLabel(prefs.getInt("grace_period_days", 3))

        binding.gracePeriodSlider.addOnChangeListener { _, value, _ ->
            updateGracePeriodLabel(value.toInt())
        }
    }

    private fun updateGracePeriodLabel(days: Int) {
        binding.gracePeriodLabel.text = "Alert personal contact after $days missed days"
    }

    private fun saveSettings() {
        prefs.edit()
            .putString("contact_name", binding.contactNameField.text.toString().trim())
            .putString("contact_number", binding.contactNumberField.text.toString().trim())
            .putInt("grace_period_days", binding.gracePeriodSlider.value.toInt())
            .putBoolean("enable_112", binding.enable112Toggle.isChecked)
            .apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
