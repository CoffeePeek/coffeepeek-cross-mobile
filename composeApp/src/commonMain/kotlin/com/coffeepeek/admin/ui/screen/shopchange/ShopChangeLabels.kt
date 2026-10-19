package com.coffeepeek.admin.ui.screen.shopchange

import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.ShopChangeSection

fun ShopChangeSection.title(): String = when (this) {
    ShopChangeSection.Photos -> "Фотографии"
    ShopChangeSection.Contacts -> "Контакты"
    ShopChangeSection.Description -> "Описание"
    ShopChangeSection.Tags -> "Особенности"
    ShopChangeSection.Roasters -> "Обжарщики"
    ShopChangeSection.Equipment -> "Оборудование"
    ShopChangeSection.Menu -> "Меню"
    ShopChangeSection.BrewMethods -> "Методы заваривания"
}

fun ShopChangeSection.hint(): String = when (this) {
    ShopChangeSection.Photos -> "Порядок и состав фотографий кофейни"
    ShopChangeSection.Contacts -> "Телефон, сайт, почта и Instagram"
    ShopChangeSection.Description -> "Текст описания заведения"
    ShopChangeSection.Tags -> "Теги и особенности"
    ShopChangeSection.Roasters -> "С кем работает кофейня"
    ShopChangeSection.Equipment -> "Кофемашины и оборудование"
    ShopChangeSection.Menu -> "Фото меню, по желанию — напитки и цены"
    ShopChangeSection.BrewMethods -> "Способы приготовления"
}

fun ModerationStatus.title(): String = when (this) {
    ModerationStatus.Pending -> "На модерации"
    ModerationStatus.Approved -> "Одобрено"
    ModerationStatus.Rejected -> "Отклонено"
}
