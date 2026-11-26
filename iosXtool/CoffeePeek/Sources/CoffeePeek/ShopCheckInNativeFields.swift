import ComposeApp
import SwiftUI
import UIKit

struct ShopCheckInNativeFields: View {
    let state: IosShopCheckInState
    let strings: CheckInNativeStrings
    let actions: ShopCheckInNativeActions

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
            if state.drinksLoading { CoffeePeekNativeLoader(label: strings["shop_checkin_form_drink"], size: 24) }
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
        ShopCheckInNativePhotos(photos: state.photos, editable: state.editable && !state.photosPreparing,
            preparing: state.photosPreparing, strings: strings, move: actions.movePhoto,
            remove: actions.removePhoto, add: actions.choosePhotos)
    }

}

private struct ShopCheckInNativePhotos: View {
    let photos: [IosShopCheckInPhoto]
    let editable: Bool
    let preparing: Bool
    let strings: CheckInNativeStrings
    let move: (Int32, Int32) -> Void
    let remove: (Int32) -> Void
    let add: (Bool) -> Void
    @State private var preview: Int?
    private var identities: [AnyHashable] { photos.map { AnyHashable($0.image.map(ObjectIdentifier.init)) } }

    var body: some View {
        CheckInNativeField(title: strings["shop_checkin_form_photos"]) {
            Text(strings.format("shop_checkin_form_photos_hint", 5))
                .font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Text(strings.format("shop_review_photo_count", photos.count, 5)).font(CoffeePeekNativeStyle.font(12, weight: "Medium"))
            if !photos.isEmpty {
                Text(strings["coffeepeek_photos_manage_hint"])
                    .font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            CoffeePeekPhotoAttachmentGrid(count: photos.count, editable: editable, viewable: editable, managesPhotos: true,
                showAdd: photos.count < 5, preparing: preparing, revision: identities,
                labels: .init(add: strings["shop_review_photo_add"], sourceTitle: strings["shop_review_photo_source_title"],
                    gallery: strings["shop_review_photo_source_gallery"], camera: strings["shop_review_photo_source_camera"],
                    cancel: strings["shop_review_photo_source_cancel"],
                    description: { strings.format("coffeepeek_photo_description", $0 + 1, photos.count) },
                    remove: { strings.format("shop_review_photo_remove", $0 + 1) },
                    earlier: strings["shop_checkin_photo_move_before"], later: strings["shop_checkin_photo_move_after"]),
                open: { preview = $0 }, move: { move(Int32($0), Int32($1)) },
                remove: { remove(Int32($0)) }, add: add) { index in
                    if let image = photos[index].image { Image(uiImage: image).resizable().scaledToFill() }
                    else { CoffeePeekPhotoPlaceholder() }
                }
        }
        .coffeePeekPhotoViewer(images: photos.map(\.image), selection: $preview, languageTag: strings.languageTag)
        .onChange(of: identities) { _, _ in preview = nil }
        .onChange(of: editable) { _, _ in preview = nil }
    }
}

struct CoffeePeekPhotoAttachmentLabels {
    let add: String
    let sourceTitle: String
    let gallery: String
    let camera: String
    let cancel: String
    let description: (Int) -> String
    let remove: (Int) -> String
    let earlier: String
    let later: String
}

/// Presentation only: each editor keeps ownership of images, permissions and photo mutations.
struct CoffeePeekPhotoAttachmentGrid<Thumbnail: View>: View {
    let count: Int
    let editable: Bool
    let viewable: Bool
    let managesPhotos: Bool
    let showAdd: Bool
    let preparing: Bool
    let revision: [AnyHashable]
    let labels: CoffeePeekPhotoAttachmentLabels
    let open: (Int) -> Void
    let move: (Int, Int) -> Void
    let remove: (Int) -> Void
    let add: (Bool) -> Void
    @ViewBuilder let thumbnail: (Int) -> Thumbnail
    @State private var dragSession = UUID()
    @State private var dropTarget: Int?
    @State private var sourcePresented = false

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 96, maximum: 96), spacing: 8)], alignment: .leading, spacing: 8) {
            ForEach(0..<count, id: \.self) { index in tile(index) }
            if showAdd { addTile }
        }
        .onChange(of: revision) { _, _ in resetInteraction() }
        .onChange(of: editable) { _, _ in resetInteraction() }
        .onChange(of: showAdd) { _, _ in resetInteraction() }
    }

    @ViewBuilder private func tile(_ index: Int) -> some View {
        if editable {
            photoButton(index)
                .draggable(dragSession.uuidString + ":" + String(index))
                .dropDestination(for: String.self, action: { values, _ in acceptDrop(values, target: index) },
                    isTargeted: { dropTarget = $0 ? index : nil })
                .overlay(alignment: .topTrailing) { removeButton(index) }
                .accessibilityActions {
                    if index > 0 { Button(labels.earlier) { move(index, index - 1) } }
                    if index < count - 1 { Button(labels.later) { move(index, index + 1) } }
                }
        } else {
            photoButton(index).overlay(alignment: .topTrailing) {
                if managesPhotos { removeButton(index) }
            }
        }
    }

    private func photoButton(_ index: Int) -> some View {
        Button { open(index) } label: {
            thumbnail(index).frame(width: 96, height: 96).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(dropTarget == index ?
                    CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.border, lineWidth: dropTarget == index ? 3 : 1))
                .overlay(alignment: .bottomLeading) {
                    Text(String(index + 1)).font(CoffeePeekNativeStyle.font(11, weight: "Medium"))
                        .foregroundStyle(.white).padding(.horizontal, 4)
                        .background(.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 4)).padding(4)
                }
        }.buttonStyle(.plain).disabled(!viewable).accessibilityLabel(labels.description(index))
    }

    private func removeButton(_ index: Int) -> some View {
        Button(role: .destructive) { remove(index) } label: {
            Image(systemName: "xmark").font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.white).frame(width: 24, height: 24)
                .background(.black.opacity(0.55), in: Circle()).overlay(Circle().stroke(.white, lineWidth: 1.5))
                .frame(width: 44, height: 44)
        }.buttonStyle(.plain).disabled(!editable).accessibilityLabel(labels.remove(index))
    }

    private var addTile: some View {
        Button { sourcePresented = true } label: {
            Group {
                if preparing { CoffeePeekNativeLoader(size: 24) }
                else { Image(systemName: "camera").font(.system(size: 28)) }
            }
            .foregroundStyle(CoffeePeekNativeStyle.secondary).frame(width: 96, height: 96)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(CoffeePeekNativeStyle.border))
        }.buttonStyle(.plain).disabled(!editable).accessibilityLabel(labels.add)
            .confirmationDialog(labels.sourceTitle, isPresented: $sourcePresented, titleVisibility: .visible) {
                Button(labels.gallery) { chooseSource(camera: false) }
                Button(labels.camera) { chooseSource(camera: true) }
                Button(labels.cancel, role: .cancel) {}
            }
            .tint(CoffeePeekNativeStyle.foreground)
    }

    private func chooseSource(camera: Bool) {
        guard editable && showAdd && !preparing else { return }
        add(camera)
    }

    private func acceptDrop(_ values: [String], target: Int) -> Bool {
        guard editable, let value = values.first else { return false }
        let parts = value.split(separator: ":")
        guard parts.count == 2, String(parts[0]) == dragSession.uuidString,
              let source = Int(parts[1]), source != target,
              (0..<count).contains(source), (0..<count).contains(target) else { return false }
        resetInteraction()
        move(source, target)
        return true
    }

    private func resetInteraction() {
        dragSession = UUID()
        dropTarget = nil
        sourcePresented = false
    }
}

#Preview("Shop photo attachments light") {
    ShopCheckInNativePhotos(photos: shopPhotoPreviewItems(), editable: true, preparing: false,
        strings: CheckInNativeStrings(languageTag: "ru", table: "NativeShopCheckIn"), move: { _, _ in }, remove: { _ in }, add: { _ in })
        .padding(16).preferredColorScheme(.light)
}

#Preview("Shop photo attachments dark") {
    ShopCheckInNativePhotos(photos: shopPhotoPreviewItems(), editable: true, preparing: false,
        strings: CheckInNativeStrings(languageTag: "be", table: "NativeShopCheckIn"), move: { _, _ in }, remove: { _ in }, add: { _ in })
        .padding(16).preferredColorScheme(.dark)
}

private func shopPhotoPreviewItems() -> [IosShopCheckInPhoto] {
    (0..<4).map { index in
        IosShopCheckInPhoto(index: Int32(index), image: UIImage(systemName: "cup.and.saucer.fill"), name: "preview-\(index)")
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
