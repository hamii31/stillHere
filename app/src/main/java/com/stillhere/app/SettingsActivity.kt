package com.stillhere.app

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stillhere.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val SMS_PERMISSION_REQUEST = 101
    }

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
        val contactNumber = binding.contactNumberField.text.toString().trim()
        val contactName = binding.contactNameField.text.toString().trim()
        val gracePeriod = binding.gracePeriodSlider.value.toInt()
        val enable112 = binding.enable112Toggle.isChecked

        if (contactNumber.isNotEmpty() || enable112) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS),
                    SMS_PERMISSION_REQUEST
                )
                return
            }
        }

        prefs.edit()
            .putString("contact_name", contactName)
            .putString("contact_number", contactNumber)
            .putInt("grace_period_days", gracePeriod)
            .putBoolean("enable_112", enable112)
            .apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveSettings()
            } else {
                Toast.makeText(
                    this,
                    "SMS permission is needed to send alerts",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
