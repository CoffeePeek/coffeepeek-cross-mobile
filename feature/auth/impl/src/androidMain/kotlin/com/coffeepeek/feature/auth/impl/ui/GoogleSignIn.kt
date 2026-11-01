package com.coffeepeek.feature.auth.impl.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.auth.domain.SocialFailure
import com.coffeepeek.feature.auth.domain.SocialFailureKind
import com.coffeepeek.feature.auth.impl.R
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
    val currentContext = rememberUpdatedState(context)
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
            if (token.isNullOrBlank()) currentOnFailure.value(currentContext.value.getString(R.string.auth_google_token_failed)) else currentOnToken.value(token)
        } catch (e: ApiException) {
            Log.w(Tag, "signIn failed: status=${e.statusCode}", e)
            googleSignInFailure(currentContext.value, e)?.let { currentOnFailure.value(it) }
        } catch (e: Exception) {
            Log.w(Tag, "signIn failed", e)
            currentOnFailure.value(e.message ?: currentContext.value.getString(R.string.auth_google_sign_in_failed))
        }
    }
    val activity = LocalActivity.current
    return {
        if (activity == null) currentOnFailure.value(currentContext.value.getString(R.string.auth_google_activity_unavailable)) else launcher.launch(client.signInIntent)
    }
}


private fun googleSignInFailure(context: Context, error: ApiException): String? = when (error.statusCode) {
    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> null
    CommonStatusCodes.DEVELOPER_ERROR -> context.getString(R.string.auth_google_configuration_error)
    GoogleSignInStatusCodes.SIGN_IN_FAILED -> context.getString(R.string.auth_google_rejected)
    else -> context.getString(R.string.auth_google_status_error, error.statusCode,
        error.message ?: context.getString(R.string.auth_google_unknown_error))
}

/** The white pill with the Google "G"; it bleeds into the card padding like the legacy social buttons. */
@Composable
internal fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(50)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CoffeePeekDimens.controlHeight)
            .shadow(
                elevation = if (enabled && !isDark) 3.dp else 0.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            ),
        shape = shape,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFE9E9EC)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            contentColor = if (isDark) Color.White else Color(0xFF15171C),
            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        ),
    ) {
        Image(painterResource(R.drawable.auth_ic_google_g), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.auth_sign_in_google),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
internal fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            text = "  ${stringResource(R.string.auth_or)}  ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
internal fun SocialFailureText(failure: SocialFailure?, modifier: Modifier = Modifier) {
    if (failure == null) return
    val base = stringResource(
        when (failure.kind) {
            SocialFailureKind.AccountExists -> R.string.auth_social_account_exists
            SocialFailureKind.EmailNotVerified -> R.string.auth_social_email_not_verified
            SocialFailureKind.Network -> R.string.auth_login_network
            SocialFailureKind.Generic -> R.string.auth_social_generic
        },
    )
    val diagnostic = failure.diagnostic
    val text = if (diagnostic == null) base else {
        val domain = diagnostic.domain?.let { stringResource(R.string.auth_social_diag_domain, it) }.orEmpty()
        val oauth = diagnostic.oauthError?.let { stringResource(R.string.auth_social_diag_oauth, it) }.orEmpty()
        stringResource(R.string.auth_social_diag, base, diagnostic.code, domain, oauth)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier.fillMaxWidth().padding(top = 6.dp))
}
