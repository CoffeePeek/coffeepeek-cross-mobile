package com.coffeepeek.admin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID

internal object AndroidPush {
    const val CHANNEL = "coffeepeek_notifications"
    var requestPermission: () -> Unit = {}
    var requestPermissionFromSettings: () -> Unit = {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private var activeUser: String? = null
    private var configured = false
    private var revokePending = true
    private lateinit var context: Context

    fun install(application: Context) {
        context = application.applicationContext
        configured = FirebaseApp.initializeApp(context) != null
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Уведомления CoffeePeek", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            })
        val preferences = context.getSharedPreferences("push_device", Context.MODE_PRIVATE)
        val installation = preferences.getString("installation", null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("installation", it).apply()
        }
        NativePushBridge.configure(installation, configured, permitted(), { requestPermission() }, ::accountChanged,
            { requestPermissionFromSettings() })
    }

    fun permitted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    fun refresh() {
        NativePushBridge.updatePermission(permitted())
        if (configured) scope.launch { lock.withLock { token() } }
    }

    private fun accountChanged(userId: String?) {
        NotificationManagerCompat.from(context).cancelAll()
        if (!configured) return
        scope.launch {
            lock.withLock {
                if (activeUser != userId) revokePending = true
                activeUser = userId
                FirebaseMessaging.getInstance().isAutoInitEnabled = false
                token()
            }
        }
    }

    private suspend fun token() {
        if (revokePending) {
            try { await(FirebaseMessaging.getInstance().deleteToken()); revokePending = false }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return }
        }
        val user = activeUser ?: return
        if (!permitted() || !NativePushBridge.accepts(user)) return
        try {
            val token = await(FirebaseMessaging.getInstance().token)
            if (activeUser == user) NativePushBridge.updateToken(user, token)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
    }

    private suspend fun <T> await(task: com.google.android.gms.tasks.Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        task.addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        task.addOnCanceledListener { continuation.cancel() }
    }
}
