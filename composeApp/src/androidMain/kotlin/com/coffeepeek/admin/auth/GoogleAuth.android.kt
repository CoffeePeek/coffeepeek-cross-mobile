package com.coffeepeek.admin.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.coffeepeek.admin.CoffeePeekApplication
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

actual object GoogleAuth {
    actual fun isSupported(): Boolean = true

    actual suspend fun signIn(): Result<String> =
        Result.failure(UnsupportedOperationException("Используйте rememberGoogleSignInLauncher()"))

    actual suspend fun signOut() {
        if (com.coffeepeek.BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        runCatching {
            val context = CoffeePeekApplication.context
            val client = GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .build(),
            )
            client.signOut()
        }
    }

    @Composable
    fun rememberGoogleSignInLauncher(
        webClientId: String,
        onResult: (Result<String>) -> Unit,
    ): () -> Unit {
        if (webClientId.isBlank()) {
            return { onResult(Result.failure(Exception("GOOGLE_WEB_CLIENT_ID не настроен"))) }
        }

        val context = LocalContext.current
        val activity = context as? Activity

        val gso = remember(webClientId) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        }
        val client = remember(gso) { GoogleSignIn.getClient(context, gso) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken
                if (token.isNullOrBlank()) {
                    onResult(Result.failure(Exception("Не удалось получить Google token")))
                } else {
                    onResult(Result.success(token))
                }
            } catch (e: ApiException) {
                Log.w("CoffeePeek-GoogleAuth", "signIn failed: status=${e.statusCode}", e)
                val message = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                        GOOGLE_SIGN_IN_CANCELLED
                    CommonStatusCodes.DEVELOPER_ERROR ->
                        "Google Sign-In: неверная конфигурация. Добавьте SHA-1 debug/release keystore и Android OAuth client (com.coffeepeek) в Google Cloud Console."
                    GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                        "Google Sign-In отклонён. Проверьте GOOGLE_WEB_CLIENT_ID и OAuth-клиенты в Google Cloud Console."
                    else ->
                        "Google Sign-In ошибка ${e.statusCode}: ${e.message ?: "неизвестная"}"
                }
                onResult(Result.failure(Exception(message)))
            } catch (e: Exception) {
                Log.w("CoffeePeek-GoogleAuth", "signIn failed", e)
                onResult(Result.failure(e))
            }
        }

        return {
            if (activity == null) {
                onResult(Result.failure(Exception("Activity недоступна")))
            } else {
                launcher.launch(client.signInIntent)
            }
        }
    }
}
