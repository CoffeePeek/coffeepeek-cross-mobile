package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.domain.model.PriceRange
import com.coffeepeek.feature.contributions.impl.shop.*
import com.coffeepeek.feature.contributions.impl.roaster.*
import com.coffeepeek.feature.contributions.impl.change.*

internal fun editShop(model: AddShopViewModel, field: String, value: String) {
    val action = when (field) {
        "name" -> AddShopAction.Name(value)
        "description" -> AddShopAction.Description(value)
        "address" -> AddShopAction.Address(value)
        "phone" -> AddShopAction.Phone(value)
        "email" -> AddShopAction.Email(value)
        "website" -> AddShopAction.Website(value)
        "instagram" -> AddShopAction.Instagram(value)
        "price" -> AddShopAction.Price(PriceRange.entries.firstOrNull { it.name == value })
        else -> return
    }
    model.onAction(action)
}

internal fun editRoaster(model: AddRoasterViewModel, field: String, value: String) {
    val action = when (field) {
        "name" -> AddRoasterAction.Name(value)
        "description" -> AddRoasterAction.About(value)
        "address" -> AddRoasterAction.Address(value)
        "instagram" -> AddRoasterAction.Instagram(value)
        "website" -> AddRoasterAction.Website(value)
        "city" -> AddRoasterAction.City(value)
        else -> return
    }
    model.onAction(action)
}

internal fun editEditor(model: ChangeEditorViewModel, field: String, value: String) {
    val action = when (field) {
        "description" -> ChangeEditorAction.Description(value)
        "phone" -> ChangeEditorAction.Phone(value)
        "email" -> ChangeEditorAction.Email(value)
        "website" -> ChangeEditorAction.Website(value)
        "instagram" -> ChangeEditorAction.Instagram(value)
        else -> return
    }
    model.onAction(action)
}

internal fun toggleShop(model: AddShopViewModel, group: String, id: String) {
    val action = when (group) {
        "city" -> AddShopAction.City(id)
        "price" -> AddShopAction.Price(PriceRange.entries.firstOrNull { it.name == id })
        "beans" -> AddShopAction.ToggleBean(id)
        "equipment" -> AddShopAction.ToggleEquipment(id)
        "roasters" -> AddShopAction.ToggleRoaster(id)
        "brewMethods" -> AddShopAction.ToggleBrewMethod(id)
        else -> return
    }
    model.onAction(action)
}

