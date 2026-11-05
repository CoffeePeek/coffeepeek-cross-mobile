package com.coffeepeek.feature.auth.impl.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

private const val Tag = "CoffeePeek-GoogleAuth"

/**
 * The Google account chooser. A token goes to [onToken]; a failure goes to [onFailure] with the text the legacy launcher produced,
 * which the domain classifier reads (the diagnostic codes of the data layer travel in it). A cancelled chooser reports nothing.
 */
@Composable
internal fun rememberGoogleSignIn(webClientId: String, onToken: (String) -> Unit, onFailure: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnToken = rememberUpdatedState(onToken)
    val currentOnFailure = rememberUpdatedState(onFailure)
    val client = remember(webClientId) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webClientId).requestEmail().build(),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) currentOnFailure.value("Не удалось получить Google token") else currentOnToken.value(token)
        } catch (e: ApiException) {
            Log.w(Tag, "signIn failed: status=${e.statusCode}", e)
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> Unit
                CommonStatusCodes.DEVELOPER_ERROR -> currentOnFailure.value("Google Sign-In: неверная конфигурация. Добавьте SHA-1 debug/release keystore и Android OAuth client (com.coffeepeek) в Google Cloud Console.")
                GoogleSignInStatusCodes.SIGN_IN_FAILED -> currentOnFailure.value("Google Sign-In отклонён. Проверьте GOOGLE_WEB_CLIENT_ID и OAuth-клиенты в Google Cloud Console.")
                else -> currentOnFailure.value("Google Sign-In ошибка ${e.statusCode}: ${e.message ?: "неизвестная"}")
            }
        } catch (e: Exception) {
            Log.w(Tag, "signIn failed", e)
            currentOnFailure.value(e.message ?: "Не удалось войти через Google")
        }
    }
    val activity = context as? Activity
    return {
        if (activity == null) currentOnFailure.value("Activity недоступна") else launcher.launch(client.signInIntent)
    }
}

/** The Android Google account chooser for the OAuth project's [webClientId]. */
internal class AndroidGoogleSignInLauncher(private val webClientId: String) : GoogleSignInLauncher {
    @Composable
    override fun rememberSignIn(onToken: (String) -> Unit, onFailure: (String) -> Unit): () -> Unit =
        rememberGoogleSignIn(webClientId, onToken, onFailure)
}
