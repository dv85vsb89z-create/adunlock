package com.example.adunlock

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.adunlock.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var rewardedAdManager: RewardedAdManager

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                if (RewardStore.isUnlocked(this@MainActivity)) {
                    startVpnService()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adUnitId = "ca-app-pub-3940256099942544/5224354917"
        rewardedAdManager = RewardedAdManager(this, adUnitId)
        rewardedAdManager.initialize()

        binding.watchAdButton.setOnClickListener {
            rewardedAdManager.showReward {
                lifecycleScope.launch {
                    val unlockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30)
                    RewardStore.setUnlockUntil(this@MainActivity, unlockUntil)
                    updateStatus()
                    requestVpnPermissionIfNeeded()
                }
            }
        }

        lifecycleScope.launch {
            updateStatus()
            if (RewardStore.isUnlocked(this@MainActivity)) {
                requestVpnPermissionIfNeeded()
            }
        }
    }

    private fun requestVpnPermissionIfNeeded() {
        if (RewardStore.isUnlocked(this)) {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                vpnPermissionLauncher.launch(intent)
            } else {
                startVpnService()
            }
        } else {
            stopVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, AdBlockVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpnService() {
        stopService(Intent(this, AdBlockVpnService::class.java))
    }

    private suspend fun updateStatus() {
        val remainingMs = RewardStore.remainingMs(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60

        binding.statusText.text = if (remainingMs > 0) {
            "Blocage actif : ${minutes} min ${seconds} s"
        } else {
            "Aucune récompense active"
        }
    }
}
