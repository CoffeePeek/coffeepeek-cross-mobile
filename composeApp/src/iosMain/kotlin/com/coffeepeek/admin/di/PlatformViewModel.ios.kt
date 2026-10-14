package com.coffeepeek.admin.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

@Composable
actual inline fun <reified T : ViewModel> platformViewModel(
    qualifier: Qualifier?,
    key: String?,
    noinline parameters: ParametersDefinition?,
): T = koinViewModel(
    qualifier = qualifier,
    key = key,
    parameters = parameters,
)
