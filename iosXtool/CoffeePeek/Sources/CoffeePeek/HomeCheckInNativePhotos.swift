import ComposeApp
import SwiftUI
import UIKit

struct HomeCheckInNativePhotos: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let actions: HomeCheckInNativeActions
    @State private var sourcePresented = false
    @State private var preview: HomeCheckInNativePhotoSelection?
    var body: some View {
        CheckInNativeField(title: strings["home_check_in_photos"], error: photoError) {
            if state.status.editing {
                Text(strings["home_check_in_keep_photos"]).font(CoffeePeekNativeStyle.font(13))
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
            } else { creationHeader }
            thumbnails
            if state.status.photosPreparing { ProgressView(strings["home_check_in_loading"]) }
        }
        .confirmationDialog(strings["home_check_in_add_photos"], isPresented: $sourcePresented, titleVisibility: .visible) {
            Button(strings["home_check_in_gallery"]) { actions.photos(false) }
            Button(strings["home_check_in_camera"]) { actions.photos(true) }
            Button(strings["home_check_in_cancel"], role: .cancel) {}
        }
        .sheet(item: $preview) { selected in previewContent(selected.index) }
    }
    private var photoError: String? {
        state.issues.fields.first { $0.field == "Photos" }.map { strings[$0.messageKey] }
    }
    private var count: Int { state.status.editing ? state.draft.existingPhotoUrls.count : state.draft.photos.count }
    private var creationHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings["home_check_in_photos_hint"]).font(CoffeePeekNativeStyle.font(13))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            Text(strings.format("home_check_in_photos_count", count))
            if state.status.canAddPhotos {
                Button { sourcePresented = true } label: {
                    Label(strings["home_check_in_add_photos"], systemImage: "plus").frame(minHeight: 48)
                }.buttonStyle(.bordered).buttonBorderShape(.capsule)
            }
        }
    }
    private var thumbnails: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                ForEach(0..<count, id: \.self) { index in
                    Button { preview = HomeCheckInNativePhotoSelection(index: index) } label: {
                        photo(index).frame(width: 104, height: 104).clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }.buttonStyle(.plain)
                        .accessibilityLabel(strings.format("home_check_in_photo_description", index + 1, count))
                        .overlay(alignment: .topTrailing) { if !state.status.editing { photoMenu(index) } }
                }
            }
        }.scrollIndicators(.hidden)
    }
    private func photoMenu(_ index: Int) -> some View {
        Menu {
            Button(strings["coffeepeek_photo_move_earlier"], systemImage: "arrow.left") { actions.movePhoto(index, index - 1) }
                .disabled(index == 0)
            Button(strings["coffeepeek_photo_move_later"], systemImage: "arrow.right") { actions.movePhoto(index, index + 1) }
                .disabled(index == count - 1)
            Button(strings.format("coffeepeek_photo_remove", index + 1), systemImage: "trash", role: .destructive) { actions.removePhoto(index) }
        } label: {
            Image(systemName: "ellipsis.circle.fill").font(.system(size: 24))
                .symbolRenderingMode(.palette).foregroundStyle(.black, .white)
                .frame(width: 44, height: 44)
        }
        .accessibilityLabel(strings.format("home_check_in_photo_description", index + 1, count))
        .disabled(!state.status.canEdit || state.status.photosPreparing)
    }
    @ViewBuilder private func photo(_ index: Int) -> some View {
        if state.status.editing, state.draft.existingPhotoUrls.indices.contains(index) {
            HomeCheckInNativeRemoteImage(url: state.draft.existingPhotoUrls[index], fits: false, load: actions.loadImage)
        } else if state.draft.photos.indices.contains(index), let image = UIImage(data: state.draft.photos[index].bytes as Data) {
            Image(uiImage: image).resizable().scaledToFill()
        } else { CoffeePeekNativeStyle.surface.overlay { Image(systemName: "photo") } }
    }
    private func previewContent(_ index: Int) -> some View {
        NavigationStack {
            Group {
                if state.status.editing, state.draft.existingPhotoUrls.indices.contains(index) {
                    HomeCheckInNativeRemoteImage(url: state.draft.existingPhotoUrls[index], load: actions.loadImage)
                } else if state.draft.photos.indices.contains(index), let image = UIImage(data: state.draft.photos[index].bytes as Data) {
                    Image(uiImage: image).resizable().scaledToFit()
                }
            }.padding().toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(strings["home_check_in_close"]) { preview = nil } }
            }
        }
    }
}

private struct HomeCheckInNativePhotoSelection: Identifiable {
    let index: Int
    var id: Int { index }
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
                } else { CoffeePeekNativeStyle.surface.overlay { Image(systemName: "leaf").foregroundStyle(CoffeePeekNativeStyle.secondary) } }
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
