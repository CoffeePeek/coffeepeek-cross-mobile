package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.create_account
import coffeepeek.composeapp.generated.resources.forgot_password
import coffeepeek.composeapp.generated.resources.login
import coffeepeek.composeapp.generated.resources.login_subtitle
import coffeepeek.composeapp.generated.resources.login_title
import coffeepeek.composeapp.generated.resources.password
import com.coffeepeek.admin.auth.GoogleSignInButton
import com.coffeepeek.admin.auth.handleGoogleSignInResult
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.di.platformViewModel

object AuthScreen {

    @Composable
    operator fun invoke(
        vm: AuthViewModel = platformViewModel(),
    ) {
        val email by vm.email.collectAsState()
        val password by vm.password.collectAsState()
        val emailError by vm.emailError.collectAsState()
        val passwordError by vm.passwordError.collectAsState()
        val loginError by vm.loginError.collectAsState()

        AuthScreenScaffold(
            mascot = AuthMascot.Laptop,
            onClose = Navigator::closeAuth,
        ) {
            Text(
                text = stringResource(Res.string.login_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.52).sp,
                    lineHeight = 28.6.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = CpDimens.spacing5),
            )

            if (isGoogleSignInConfigured()) {
                GoogleSignInButton(
                    onResult = { result ->
                        handleGoogleSignInResult(
                            result = result,
                            onSuccess = vm::onGoogleLogin,
                            onFailure = vm::onGoogleLoginError,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthOrDivider()
                Spacer(modifier = Modifier.height(14.dp))
            }

            AuthTextField(
                label = "Email",
                value = email,
                onValueChange = vm::onEmailChange,
                placeholder = "name@example.com",
                leadingIcon = CpIcons.Email,
                errorText = emailError,
                next = true,
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                label = stringResource(Res.string.password),
                value = password,
                onValueChange = vm::onPasswordChange,
                placeholder = stringResource(Res.string.password),
                leadingIcon = CpIcons.Lock,
                isPassword = true,
                errorText = passwordError,
            )

            AnimatedVisibility(visible = loginError != null) {
                Text(
                    text = loginError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(Res.string.forgot_password),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = CpColor.Primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.clickable {
                        Navigator.navigate(Navigator.Screen.ForgotPassword(email))
                    },
                )
            }

            AuthPrimaryButton(
                text = stringResource(Res.string.login),
                onClick = { vm.onLoginClick() },
            )

            AuthFooterRow(
                onBack = null,
                onSecondary = { Navigator.popThenNavigate(Navigator.Screen.Register) },
                secondaryText = stringResource(Res.string.create_account),
            )
        }
    }
}
