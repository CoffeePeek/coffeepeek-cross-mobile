import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeSavedPublicCollectionController(
    collection: IosSavedPublicCollection,
    languageTag: String
) -> UIViewController {
    UIHostingController(rootView: SavedPublicCollectionHost(
        collection: collection,
        strings: SavedPublicCollectionStrings(languageTag: languageTag)
    ))
}

private final class SavedPublicCollectionModel: ObservableObject {
    let collection: IosSavedPublicCollection
    @Published var state: IosSavedPublicCollectionState
    private var observation: IosSavedPublicCollectionObservation?

    init(collection: IosSavedPublicCollection) {
        self.collection = collection
        state = collection.current()
    }

    func appear() {
        guard observation == nil else { return }
        state = collection.current()
        observation = collection.observe { [weak self] in
            guard let self else { return }
            state = collection.current()
        }
    }

    func disappear() {
        observation?.cancel()
        observation = nil
    }

    deinit {
        observation?.cancel()
        collection.close()
    }
}

private struct SavedPublicCollectionHost: View {
    @StateObject private var model: SavedPublicCollectionModel
    let strings: SavedPublicCollectionStrings

    init(collection: IosSavedPublicCollection, strings: SavedPublicCollectionStrings) {
        _model = StateObject(wrappedValue: SavedPublicCollectionModel(collection: collection))
        self.strings = strings
    }

    var body: some View {
        Group {
            if model.state.preview {
                SavedPublicCollectionPoster(collection: model.collection)
            } else {
                content
            }
        }
        .onAppear(perform: model.appear)
        .onDisappear(perform: model.disappear)
    }

    private var content: some View {
        SavedPublicCollectionScreen(
            state: model.state,
            strings: strings,
            back: model.collection.back,
            retry: model.collection.reload,
            share: model.collection.shareLink,
            preview: model.collection.preview,
            openPlace: { model.collection.openPlace(key: $0) }
        ) { key in
            SavedPublicCollectionCover(collection: model.collection, key: key)
                .id(key)
        }
    }
}

private struct SavedPublicCollectionPoster: UIViewControllerRepresentable {
    let collection: IosSavedPublicCollection

    func makeUIViewController(context: Context) -> UIViewController {
        collection.makePoster()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct SavedPublicCollectionScreen<Cover: View>: View {
    let state: IosSavedPublicCollectionState
    let strings: SavedPublicCollectionStrings
    let back: () -> Void
    let retry: () -> Void
    let share: () -> Void
    let preview: () -> Void
    let openPlace: (String) -> Void
    @ViewBuilder let cover: (String) -> Cover

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                status
                if state.title != nil { collectionContent }
            }
            .padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .safeAreaInset(edge: .bottom, spacing: 0) { actions }
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: back) {
                Label(strings.text("public.back"), systemImage: "chevron.backward")
                    .labelStyle(.iconOnly)
                    .frame(minWidth: 44, minHeight: 44)
            }
            Text(state.title ?? strings.text("public.title"))
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .background(.bar)
    }

    @ViewBuilder private var status: some View {
        if state.loading {
            ProgressView(strings.text("public.loading"))
                .frame(maxWidth: .infinity)
        }
        if state.failed {
            Text(strings.text("public.error"))
                .foregroundStyle(.red)
            Button(strings.text("public.retry"), action: retry)
                .frame(minHeight: 44)
                .disabled(state.loading || state.busy)
        }
    }

    @ViewBuilder private var collectionContent: some View {
        if let key = state.coverKey {
            cover(key)
                .aspectRatio(1.6, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        if !state.description.isEmpty {
            Text(state.description)
        }
        Text(strings.count(state.places.count)).foregroundStyle(.secondary)
        ForEach(state.places, id: \.key) { place in
            Button { openPlace(place.key) } label: {
                SavedPublicPlaceRow(place: place, strings: strings)
            }
            .buttonStyle(.plain)
            .disabled(state.busy)
        }
        if state.places.isEmpty {
            Text(strings.text("public.empty"))
        }
    }

    @ViewBuilder private var actions: some View {
        if state.canShare || !state.places.isEmpty {
            VStack(spacing: 4) {
                if state.canShare {
                    Button(action: share) {
                        Text(strings.text("public.share")).frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                }
                if !state.places.isEmpty {
                    Button(action: preview) {
                        Text(strings.text("public.poster")).frame(maxWidth: .infinity, minHeight: 44)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .disabled(state.busy)
            .background(.bar)
        }
    }
}

private struct SavedPublicPlaceRow: View {
    let place: IosSavedPublicPlace
    let strings: SavedPublicCollectionStrings

    var body: some View {
        HStack(spacing: 12) {
            SavedPublicCollectionImage(image: nil, url: place.photoUrl.flatMap(URL.init(string:)), fit: place.roaster)
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            VStack(alignment: .leading, spacing: 4) {
                Text(place.title).font(.headline)
                Text(place.address.isEmpty ? strings.text(place.roaster ? "public.roaster" : "public.shop") : place.address)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.forward")
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
        }
        .padding(12)
        .frame(minHeight: 76)
        .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

private struct SavedPublicCollectionCover: View {
    let collection: IosSavedPublicCollection
    let key: String
    @State private var image: UIImage?
    @State private var url: URL?
    @State private var observation: IosSavedPublicCollectionObservation?

    var body: some View {
        SavedPublicCollectionImage(image: image, url: url, fit: false)
            .onAppear {
                guard observation == nil else { return }
                observation = collection.loadCover(key: key) { data, publicURL in
                    image = data.flatMap { UIImage(data: $0 as Data) }
                    url = publicURL.flatMap(URL.init(string:))
                }
            }
            .onDisappear {
                observation?.cancel()
                observation = nil
                image = nil
                url = nil
            }
    }
}

private struct SavedPublicCollectionImage: View {
    let image: UIImage?
    let url: URL?
    let fit: Bool

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color(uiColor: .tertiarySystemFill)
                Image(systemName: "photo").foregroundStyle(.secondary)
                if let image {
                    rendered(Image(uiImage: image), size: geometry.size)
                } else if let url {
                    AsyncImage(url: url) { result in
                        if let image = result.image { rendered(image, size: geometry.size) }
                    }
                }
            }
        }
        .clipped()
        .accessibilityHidden(true)
    }

    private func rendered(_ image: Image, size: CGSize) -> some View {
        image.resizable()
            .aspectRatio(contentMode: fit ? .fit : .fill)
            .frame(width: size.width, height: size.height)
    }
}

private struct SavedPublicCollectionStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "SavedPublicCollection")
    }

    func count(_ value: Int) -> String { String(format: text("public.count"), Int32(clamping: value)) }
}

private var savedPublicCollectionPreview: some View {
    SavedPublicCollectionScreen(
        state: IosSavedPublicCollectionState(
            loading: false, failed: false, busy: false, preview: false,
            title: "Кофейный маршрут", description: "Прогулка по городу",
            coverKey: "preview", canShare: true,
            places: [IosSavedPublicPlace(key: "sample", title: "Кофейня", address: "Минск", roaster: false, photoUrl: nil)]
        ),
        strings: SavedPublicCollectionStrings(languageTag: "ru"),
        back: {}, retry: {}, share: {}, preview: {}, openPlace: { _ in }
    ) { _ in
        SavedPublicCollectionImage(image: nil, url: nil, fit: false)
    }
}

#Preview("Light") {
    savedPublicCollectionPreview.preferredColorScheme(.light)
}

#Preview("Dark") {
    savedPublicCollectionPreview.preferredColorScheme(.dark)
}
