package com.termex.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.termex.app.MainActivity
import com.termex.app.R

/**
 * Foreground service that keeps the process alive while SSH connections are active.
 * Started by ConnectionManager when the first session opens; stopped when all sessions close.
 * Holds a partial WakeLock so the CPU (and network) stays active even when the screen is off,
 * preventing the OS from interrupting long-running SSH sessions.
 */
class TermexConnectionService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "termex_connections"
        const val ACTION_UPDATE_NOTIFICATION = "com.termex.app.UPDATE_NOTIFICATION"
        const val EXTRA_CONNECTION_COUNT = "connection_count"
        private const val WAKE_LOCK_TAG = "termex:ssh_connections"
    }

    private var connectionCount = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_NOTIFICATION) {
                connectionCount = intent.getIntExtra(EXTRA_CONNECTION_COUNT, 0)
                updateNotification()
                // Release wake lock if all connections closed, re-acquire if any active
                if (connectionCount <= 0) releaseWakeLock() else acquireWakeLock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ContextCompat.registerReceiver(
            this,
            updateReceiver,
            IntentFilter(ACTION_UPDATE_NOTIFICATION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Call startForeground immediately - must happen before any async work
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        acquireWakeLock()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        try { unregisterReceiver(updateReceiver) } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = when (connectionCount) {
            0, 1 -> "1 active SSH connection"
            else -> "$connectionCount active SSH connections"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Termex")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_terminal)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SSH Connections",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active SSH connections"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
