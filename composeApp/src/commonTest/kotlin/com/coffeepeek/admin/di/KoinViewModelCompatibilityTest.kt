package com.coffeepeek.admin.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(KoinInternalApi::class)
class KoinViewModelCompatibilityTest {
    @Test
    fun resolvesRootViewModelAndRetainsItUntilItsOwnerIsCleared() {
        val owner = Owner()
        val app = koinApplication { modules(module { factory { Model("root") } }) }
        try {
            fun resolve() = resolveViewModel<Model>(
                Model::class, owner.viewModelStore, extras = defaultExtras(owner), scope = app.koin.scopeRegistry.rootScope,
            )
            val model = resolve()
            assertSame(model, resolve())
            owner.viewModelStore.clear()
            assertTrue(model.cleared)
            assertNotSame(model, resolve())
        } finally {
            owner.viewModelStore.clear()
            app.close()
        }
    }

    @Test
    fun preservesQualifierParametersAndSeparateNavigationKeys() {
        val owner = Owner()
        val qualifier = named("detail")
        val app = koinApplication { modules(module { factory(qualifier) { (id: String) -> Model(id) } }) }
        try {
            fun resolve(key: String, id: String) = resolveViewModel<Model>(
                Model::class, owner.viewModelStore, key, defaultExtras(owner), qualifier, app.koin.scopeRegistry.rootScope,
                parameters = { parametersOf(id) },
            )
            val first = resolve("first", "shop-1")
            val second = resolve("second", "shop-2")
            assertSame(first, resolve("first", "ignored"))
            assertNotSame(first, second)
            assertEquals("shop-1", first.id)
            assertEquals("shop-2", second.id)
        } finally {
            owner.viewModelStore.clear()
            app.close()
        }
    }

    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class Model(val id: String) : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() { cleared = true }
    }
}
