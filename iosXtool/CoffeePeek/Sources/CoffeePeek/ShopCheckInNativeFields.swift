import ComposeApp
import SwiftUI
import UIKit

struct ShopCheckInNativeFields: View {
    let state: IosShopCheckInState
    let strings: CheckInNativeStrings
    let actions: ShopCheckInNativeActions
    @State private var sourcePresented = false

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            shop
            CheckInNativeField(title: strings["shop_checkin_form_date"]) {
                CheckInNativeDateField(title: strings["shop_checkin_form_date"], value: state.visitDate,
                    today: state.today, label: state.dateLabel,
                    done: strings["shop_checkin_form_date_confirm"], cancel: strings["shop_checkin_form_cancel"],
                    changed: { actions.dateChanged($0) })
            }
            drink
            ratings
            CheckInNativeField(title: strings["shop_checkin_form_note"]) {
                CheckInNativeInput(title: strings["shop_checkin_form_note"], placeholder: strings["shop_checkin_form_note_hint"],
                    value: Binding(get: { state.note }, set: { actions.noteChanged($0) }), multiline: true)
            }
            photos
        }
        .disabled(!state.editable)
        .confirmationDialog(strings["shop_review_photo_source_title"], isPresented: $sourcePresented, titleVisibility: .visible) {
            Button(strings["shop_review_photo_source_camera"]) { actions.choosePhotos(true) }
            Button(strings["shop_review_photo_source_gallery"]) { actions.choosePhotos(false) }
            Button(strings["shop_review_photo_source_cancel"], role: .cancel) {}
        }
    }

    private var shop: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(state.shopName).font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
                if let address = state.shopAddress { Text(address).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            }
            Spacer(minLength: 0)
            Button(strings["shop_checkin_form_change_shop"], action: actions.changeShop).frame(minHeight: 44)
        }
    }

    private var drink: some View {
        CheckInNativeField(title: strings["shop_checkin_form_drink"]) {
            if state.drinksLoading { ProgressView() }
            if state.drinksFailed {
                Button(strings["shop_checkin_form_drinks_error"], action: actions.retryDrinks)
            }
            Picker(strings["shop_checkin_form_drink"], selection: drinkBinding) {
                Text(strings["shop_checkin_form_drink_none"]).tag("")
                ForEach(state.drinks, id: \.slug) { Text($0.name).tag($0.slug) }
                if let slug = state.drinkSlug, !state.drinks.contains(where: { $0.slug == slug }) {
                    Text(slug == "other" ? strings["shop_checkin_form_drink_other"] : slug).tag(slug)
                }
            }.pickerStyle(.menu).frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                .disabled(state.drinksLoading)
            if state.drinkSlug == "other" {
                CheckInNativeInput(title: strings["shop_checkin_form_drink_custom"],
                    placeholder: strings["shop_checkin_form_drink_custom"],
                    value: Binding(get: { state.customDrink }, set: { actions.customDrinkChanged($0) }))
            }
        }
    }

    private var drinkBinding: Binding<String> {
        Binding(get: { state.drinkSlug ?? "" }, set: { actions.drinkChanged($0.isEmpty ? nil : $0) })
    }

    private var ratings: some View {
        VStack(spacing: 12) {
            rating("coffee", score: state.coffeeRating) { actions.ratingChanged(state.placeRating, state.serviceRating, Int32($0)) }
            rating("service", score: state.serviceRating) { actions.ratingChanged(state.placeRating, Int32($0), state.coffeeRating) }
            rating("place", score: state.placeRating) { actions.ratingChanged(Int32($0), state.serviceRating, state.coffeeRating) }
        }
    }

    private func rating(_ kind: String, score: Int32, select: @escaping (Int) -> Void) -> some View {
        HStack(spacing: 12) {
            if let image = ratingImage(kind) {
                Image(uiImage: image).resizable().scaledToFill().frame(width: 56, height: 56).clipShape(RoundedRectangle(cornerRadius: 8))
            }
            CheckInNativeRating(title: strings["shop_review_" + kind], value: Int(score),
                label: { strings.format("shop_review_rating_value", strings["shop_review_" + kind], $0) }, select: select)
        }
        .padding(.horizontal, 8).padding(.vertical, 12)
        .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(CoffeePeekNativeStyle.border))
    }

    private func ratingImage(_ kind: String) -> UIImage? {
        let resource = "shop_checkin_rating_" + (kind == "place" ? "atmosphere" : kind)
        let directory = "compose-resources/composeResources/com.coffeepeek.feature.shop.impl.resources/drawable"
        return Bundle.main.url(forResource: resource, withExtension: "png", subdirectory: directory)
            .flatMap { UIImage(contentsOfFile: $0.path) }
    }

    private var photos: some View {
        CheckInNativeField(title: strings["shop_checkin_form_photos"]) {
            Text(strings.format("shop_checkin_form_photos_hint", 5))
                .font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            ScrollView(.horizontal) {
                HStack(spacing: 12) {
                    ForEach(state.photos, id: \.index) { photo in
                        VStack {
                            if let image = photo.image {
                                Image(uiImage: image).resizable().scaledToFill().frame(width: 88, height: 88)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            HStack {
                                Button { actions.movePhoto(photo.index, photo.index - 1) } label: { Image(systemName: "arrow.left") }
                                    .disabled(photo.index == 0).accessibilityLabel(strings["shop_checkin_photo_move_before"])
                                Button(role: .destructive) { actions.removePhoto(photo.index) } label: { Image(systemName: "trash") }
                                    .accessibilityLabel(strings.format("shop_review_photo_remove", Int(photo.index) + 1))
                                Button { actions.movePhoto(photo.index, photo.index + 1) } label: { Image(systemName: "arrow.right") }
                                    .disabled(Int(photo.index) == state.photos.count - 1).accessibilityLabel(strings["shop_checkin_photo_move_after"])
                            }.frame(minHeight: 44)
                        }
                    }
                }
            }
            if state.photosPreparing { ProgressView() }
            else if state.photos.count < 5 {
                Button(strings["shop_review_photo_add"]) { sourcePresented = true }.frame(minHeight: 44)
            }
        }
    }
}

#Preview("Shop fields light") {
    ScrollView {
        ShopCheckInNativeFields(state: shopCheckInPreviewState(),
            strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"), actions: ShopCheckInNativeActions())
    }.padding(16).preferredColorScheme(.light)
}

#Preview("Shop fields dark") {
    ScrollView {
        ShopCheckInNativeFields(state: shopCheckInPreviewState(),
            strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"), actions: ShopCheckInNativeActions())
    }.padding(16).preferredColorScheme(.dark)
}
