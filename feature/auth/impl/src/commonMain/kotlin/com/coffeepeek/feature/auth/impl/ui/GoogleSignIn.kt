package com.coffeepeek.feature.auth.impl.ui

import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_ic_google_g
import com.coffeepeek.feature.auth.impl.resources.auth_login_network
import com.coffeepeek.feature.auth.impl.resources.auth_or
import com.coffeepeek.feature.auth.impl.resources.auth_sign_in_google
import com.coffeepeek.feature.auth.impl.resources.auth_social_account_exists
import com.coffeepeek.feature.auth.impl.resources.auth_social_diag
import com.coffeepeek.feature.auth.impl.resources.auth_social_diag_domain
import com.coffeepeek.feature.auth.impl.resources.auth_social_diag_oauth
import com.coffeepeek.feature.auth.impl.resources.auth_social_email_not_verified
import com.coffeepeek.feature.auth.impl.resources.auth_social_generic
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.auth.domain.SocialFailure
import com.coffeepeek.feature.auth.domain.SocialFailureKind

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
        Image(painterResource(Res.drawable.auth_ic_google_g), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.auth_sign_in_google),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
internal fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            text = "  ${stringResource(Res.string.auth_or)}  ",
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
            SocialFailureKind.AccountExists -> Res.string.auth_social_account_exists
            SocialFailureKind.EmailNotVerified -> Res.string.auth_social_email_not_verified
            SocialFailureKind.Network -> Res.string.auth_login_network
            SocialFailureKind.Generic -> Res.string.auth_social_generic
        },
    )
    val diagnostic = failure.diagnostic
    val text = if (diagnostic == null) base else {
        val domain = diagnostic.domain?.let { stringResource(Res.string.auth_social_diag_domain, it) }.orEmpty()
        val oauth = diagnostic.oauthError?.let { stringResource(Res.string.auth_social_diag_oauth, it) }.orEmpty()
        stringResource(Res.string.auth_social_diag, base, diagnostic.code, domain, oauth)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier.fillMaxWidth().padding(top = 6.dp))
}
