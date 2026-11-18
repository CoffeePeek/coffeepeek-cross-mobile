import ComposeApp
import SwiftUI
import UIKit

struct HomeCheckInNativePhotos: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let actions: HomeCheckInNativeActions
    @State private var sourcePresented = false
    @State private var preview: Int?
    private var count: Int { state.status.editing ? state.draft.existingPhotoUrls.count : state.draft.photos.count }
    private var editable: Bool { state.status.canEdit && !state.status.editing && !state.status.photosPreparing }
    private var identities: [AnyHashable] {
        state.status.editing ? state.draft.existingPhotoUrls.map(AnyHashable.init) :
            state.draft.photos.map { AnyHashable($0.bytes) }
    }

    var body: some View {
        CheckInNativeField(title: strings["home_check_in_photos"], error: photoError) {
            if state.status.editing {
                Text(strings["home_check_in_keep_photos"]).font(CoffeePeekNativeStyle.font(13))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            } else { creationHeader }
            attachments
        }
        .confirmationDialog(strings["home_check_in_add_photos"], isPresented: $sourcePresented, titleVisibility: .visible) {
            Button(strings["home_check_in_gallery"]) { actions.photos(false) }
            Button(strings["home_check_in_camera"]) { actions.photos(true) }
            Button(strings["home_check_in_cancel"], role: .cancel) {}
        }
        .coffeePeekPhotoViewer(count: count, selection: $preview, languageTag: strings.languageTag, load: loadPhoto)
        .onChange(of: identities) { _, _ in resetInteraction() }
        .onChange(of: state.status.canEdit) { _, _ in resetInteraction() }
        .onChange(of: state.status.photosPreparing) { _, _ in resetInteraction() }
        .onChange(of: state.status.editing) { _, _ in resetInteraction() }
        .onChange(of: state.status.revoked || state.status.closed) { _, _ in resetInteraction() }
    }

    private var attachments: some View {
        CoffeePeekPhotoAttachmentGrid(count: count, editable: editable, viewable: state.status.canEdit,
            managesPhotos: !state.status.editing,
            showAdd: !state.status.editing && count < 5, preparing: state.status.photosPreparing, revision: identities,
            labels: .init(add: strings["home_check_in_add_photos"],
                description: { strings.format("home_check_in_photo_description", $0 + 1, count) },
                remove: { strings.format("coffeepeek_photo_remove", $0 + 1) },
                earlier: strings["coffeepeek_photo_move_earlier"], later: strings["coffeepeek_photo_move_later"]),
            open: { preview = $0 }, move: actions.movePhoto, remove: actions.removePhoto,
            add: { if state.status.canAddPhotos { sourcePresented = true } }, thumbnail: photo)
    }

    private var photoError: String? {
        state.issues.fields.first { $0.field == "Photos" }.map { strings[$0.messageKey] }
    }

    private var creationHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings["home_check_in_photos_hint"]).font(CoffeePeekNativeStyle.font(13))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            Text(strings.format("home_check_in_photos_count", count)).font(CoffeePeekNativeStyle.font(15, weight: "Medium"))
            if count > 0 {
                Text(strings["coffeepeek_photos_manage_hint"]).font(CoffeePeekNativeStyle.font(13))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
    }

    @ViewBuilder private func photo(_ index: Int) -> some View {
        if state.status.editing, state.draft.existingPhotoUrls.indices.contains(index) {
            HomeCheckInNativeRemoteImage(url: state.draft.existingPhotoUrls[index], fits: false, load: actions.loadImage)
        } else if state.draft.photos.indices.contains(index), let image = UIImage(data: state.draft.photos[index].bytes as Data) {
            Image(uiImage: image).resizable().scaledToFill()
        } else { CoffeePeekPhotoPlaceholder() }
    }

    private func loadPhoto(_ index: Int, receive: @escaping (UIImage?) -> Void) -> (() -> Void) {
        guard state.status.canEdit && !state.status.closed && !state.status.revoked else { receive(nil); return {} }
        if state.status.editing, state.draft.existingPhotoUrls.indices.contains(index) {
            let observation = actions.loadImage(state.draft.existingPhotoUrls[index], receive)
            return { observation?.cancel() }
        }
        let image = state.draft.photos.indices.contains(index) ? UIImage(data: state.draft.photos[index].bytes as Data) : nil
        receive(image)
        return {}
    }

    private func resetInteraction() {
        preview = nil
        sourcePresented = false
    }
}

struct HomeCheckInNativeRemoteImage: View {
    @Environment(\.scenePhase) private var scenePhase
    let url: String?
    var fits = true
    let load: (String, @escaping (UIImage?) -> Void) -> IosHomeCheckInObservation?
    @State private var image: UIImage?
    @State private var observation: IosHomeCheckInObservation?
    @State private var request: UUID?
    @State private var visible = false
    var body: some View {
        GeometryReader { geometry in
            Group {
                if let image {
                    Image(uiImage: image).resizable().aspectRatio(contentMode: fits ? .fit : .fill)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                } else { CoffeePeekPhotoPlaceholder() }
            }.clipped()
        }.accessibilityHidden(true)
            .onAppear {
                visible = true
                start()
            }
            .onChange(of: url) { _, _ in start() }
            .onChange(of: scenePhase) { _, _ in start() }
            .onDisappear {
                visible = false
                cancel()
            }
    }
    private func start() {
        cancel()
        guard visible, scenePhase == .active, let url, !url.isEmpty else { return }
        let token = UUID()
        request = token
        observation = load(url) { next in if request == token { image = next } }
    }
    private func cancel() {
        request = nil
        image = nil
        observation?.cancel()
        observation = nil
    }
}

#Preview("Home photos light") {
    HomeCheckInNativePhotos(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}
#Preview("Home photos dark") {
    HomeCheckInNativePhotos(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("be"), actions: .init())
        .padding().background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

#Preview("Check-in photo placeholder light") {
    HomeCheckInNativeRemoteImage(url: nil, load: { _, _ in nil }).frame(width: 160, height: 200)
        .preferredColorScheme(.light)
}

private func homePhotoPreviewState(count: Int, editing: Bool = false) -> IosHomeCheckInState {
    let state = homeCheckInNativePreviewState(editing: editing)
    return IosHomeCheckInState(draft: homePhotoPreviewDraft(count: count, editing: editing), status: state.status,
                               catalog: state.catalog, issues: state.issues, saved: nil)
}

private func homePhotoPreviewDraft(count: Int, editing: Bool) -> IosHomeCheckInDraft {
    let data = UIImage(systemName: "cup.and.saucer.fill")?.pngData() ?? Data()
    let photos = (0..<count).map { IosCheckInPhoto(fileName: "preview-\($0).png", contentType: "image/png", bytes: data) }
    return IosHomeCheckInDraft(preparedOn: "2026-11-17", dateLabel: "17.11.2026", methodCode: nil,
        dose: "", durationSeconds: nil, tasteScore: nil, note: "", drinkName: "", isPublic: nil,
        beanName: nil, shelfBagId: nil, showShelfBag: false, shelfBagCardUrl: nil,
        photos: editing ? [] : photos,
        existingPhotoUrls: editing ? (0..<count).map { "https://preview.invalid/photo/\($0)" } : [], customBeanName: nil)
}

#Preview("Home photo grid light") {
    HomeCheckInNativePhotos(state: homePhotoPreviewState(count: 4), strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
        .padding(16).frame(width: 320).background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Home photo grid full dark") {
    HomeCheckInNativePhotos(state: homePhotoPreviewState(count: 5), strings: homeCheckInNativePreviewStrings("be"), actions: .init())
        .padding(16).frame(width: 320).background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

#Preview("Home retained photos light") {
    HomeCheckInNativePhotos(state: homePhotoPreviewState(count: 2, editing: true),
        strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
        .padding(16).background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Home retained photos dark") {
    HomeCheckInNativePhotos(state: homePhotoPreviewState(count: 2, editing: true),
        strings: homeCheckInNativePreviewStrings("be"), actions: .init())
        .padding(16).background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}

#Preview("Check-in photo placeholder dark") {
    HomeCheckInNativeRemoteImage(url: nil, load: { _, _ in nil }).frame(width: 160, height: 200)
        .preferredColorScheme(.dark)
}
