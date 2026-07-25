package com.badalbiswas.filevault

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val hiddenSwitch = findViewById<Switch>(R.id.hiddenFilesSwitch)
        hiddenSwitch.isChecked = AppPreferences.getShowHidden(this)
        hiddenSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setShowHidden(this, isChecked)
        }

        findViewById<TextView>(R.id.userAgreementItem).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("User Agreement")
                .setMessage("By using FileVault Manager, you agree to use this app responsibly for managing your own device files. This app does not collect or transmit your personal data to any server.")
                .setPositiveButton("OK", null)
                .show()
        }

        findViewById<TextView>(R.id.privacyPolicyItem).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("FileVault Manager operates entirely on your device. No file data, personal information, or usage statistics are collected or shared with third parties.")
                .setPositiveButton("OK", null)
                .show()
        }

        val versionText = findViewById<TextView>(R.id.versionText)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = pInfo.versionName
        } catch (e: Exception) {
            versionText.text = "1.0.0"
        }
    }
}
