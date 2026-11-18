import ComposeApp
import SwiftUI
import UIKit

struct CollectionPosterPreview: View {
    @ObservedObject var model: CollectionsWorkspaceModel
    let strings: CollectionsWorkspaceStrings
    @State private var page = 0
    @State private var expanded = false
    @State private var photos: [String: UIImage] = [:]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if model.state.loading { ProgressView(strings["collections_loading"]) }
                if model.state.failure != nil {
                    Text(strings["collections_load_failed"]).foregroundStyle(.red)
                    Button(strings["collections_retry"], action: model.workspace.reload)
                }
                if let poster = model.workspace.poster() {
                    Text(strings.count(Int32(model.state.places.count))).frame(maxWidth: .infinity, alignment: .leading)
                    Picker(strings["collections_picture"], selection: Binding(get: { model.state.story }, set: { value in
                        page = 0; model.workspace.format(story: value)
                    })) {
                        Text(strings["collections_format_chat"]).tag(false)
                        Text(strings["collections_format_story"]).tag(true)
                    }.pickerStyle(.segmented).disabled(model.state.busy)
                    posterImage(poster).onTapGesture { expanded = true }
                        .accessibilityLabel(poster.title)
                        .accessibilityHint(strings["collections_expand_poster"])
                    if poster.pages.count > 1 { pagination(poster.pages.count) }
                    if model.state.mode == "Snapshot" {
                        ForEach(model.state.places, id: \.key) { place in
                            Button { model.workspace.openPlace(key: place.key) } label: {
                                HStack {
                                    Text(place.title); Spacer(); Image(systemName: "chevron.forward")
                                }.frame(minHeight: 44)
                            }
                        }
                    }
                    if poster.pages.isEmpty { Text(strings["collections_unavailable"]) }
                }
            }.padding(16)
        }
        .safeAreaInset(edge: .top) {
            HStack {
                Button(action: model.workspace.back) {
                    Label(strings["collections_back"], systemImage: "chevron.backward").labelStyle(.iconOnly)
                        .frame(minWidth: 44, minHeight: 44)
                }
                Text(model.state.title).font(.headline).lineLimit(1); Spacer()
            }.padding(.horizontal, 8).background(.bar)
        }
        .safeAreaInset(edge: .bottom) {
            HStack {
                if model.state.canShare { Button(strings["collections_share_link"], action: model.workspace.shareLink) }
                Button(action: model.workspace.shareImages) {
                    if model.state.busy { ProgressView(strings["collections_preparing"]) }
                    else { Text(strings["collections_share_photo"]) }
                }.disabled(model.state.busy || model.state.places.isEmpty)
            }.frame(maxWidth: .infinity, minHeight: 44).padding(8).background(.bar)
        }
        .task(id: "\(model.state.id ?? ""):\(model.state.accountVersion):\(model.state.story)") {
            photos = [:]
            guard let poster = model.workspace.poster() else { return }
            let loaded = await loadCollectionPosterPhotos(poster, workspace: model.workspace)
            if !Task.isCancelled { photos = loaded }
        }
        .fullScreenCover(isPresented: $expanded) {
            if let poster = model.workspace.poster() {
                ScrollView([.vertical, .horizontal]) { posterImage(poster).frame(minWidth: 360) }
                    .safeAreaInset(edge: .top) {
                        Button(strings["collections_back"]) { expanded = false }.frame(minHeight: 44)
                    }
                    .background(CoffeePeekNativeStyle.background)
            }
        }
    }

    private func posterImage(_ poster: IosCollectionsPoster) -> some View {
        CollectionPosterCanvas(poster: poster, page: min(page, max(0, poster.pages.count - 1)), photos: photos, strings: strings)
            .frame(width: CGFloat(poster.width), height: CGFloat(poster.height))
            .modifier(CollectionPosterScale(width: CGFloat(poster.width), height: CGFloat(poster.height)))
    }

    private func pagination(_ count: Int) -> some View {
        HStack {
            Button { page -= 1 } label: {
                Label(strings["collections_previous_page"], systemImage: "chevron.backward").labelStyle(.iconOnly)
                    .frame(width: 44, height: 44)
            }.disabled(page == 0 || model.state.busy)
            Spacer()
            Text("\(page + 1) / \(count)")
            Spacer()
            Button { page += 1 } label: {
                Label(strings["collections_next_page"], systemImage: "chevron.forward").labelStyle(.iconOnly)
                    .frame(width: 44, height: 44)
            }.disabled(page + 1 >= count || model.state.busy)
        }
    }
}

private struct CollectionPosterScale: ViewModifier {
    let width: CGFloat
    let height: CGFloat
    func body(content: Content) -> some View {
        GeometryReader { geometry in
            content.scaleEffect(geometry.size.width / width, anchor: .topLeading)
        }.aspectRatio(width / height, contentMode: .fit)
    }
}

/// Uses the feature's shared pagination/coordinates for the screen and every exported PNG.
struct CollectionPosterCanvas: View {
    let poster: IosCollectionsPoster
    let page: Int
    let photos: [String: UIImage]
    let strings: CollectionsWorkspaceStrings

    var body: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(colors: [Color(red: 0.24, green: 0.18, blue: 0.16), Color(red: 0.1, green: 0.08, blue: 0.07)],
                startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 12) {
                Text(poster.title).font(.system(size: 44, weight: .bold)).lineLimit(2)
                Text(strings.count(Int32(poster.pages.flatMap(\.cards).count))).font(.system(size: 24)).foregroundStyle(.gray)
            }.frame(width: 1016, alignment: .leading).offset(x: 32, y: 66)
            if poster.pages.indices.contains(page) {
                ForEach(poster.pages[page].cards, id: \.place.key) { card in
                    cardView(card).frame(width: CGFloat(card.width), height: CGFloat(card.height))
                        .offset(x: CGFloat(card.left), y: CGFloat(card.top))
                }
            }
            brand.offset(y: 0)
            brand.offset(y: CGFloat(poster.height) - 44)
            if poster.pages.count > 1 {
                Text("\(page + 1) / \(poster.pages.count)").font(.system(size: 22)).foregroundStyle(.gray)
                    .offset(x: 32, y: CGFloat(poster.height) - 82)
            }
        }.foregroundStyle(.white).environment(\.dynamicTypeSize, .medium).environment(\.colorScheme, .dark)
    }

    private var brand: some View {
        HStack(spacing: 24) {
            ForEach(0..<7) { _ in Text(strings["collections_brand"]).font(.system(size: 22, weight: .semibold)) }
        }.foregroundStyle(CoffeePeekNativeStyle.accent)
            .frame(width: CGFloat(poster.width), height: 44).clipped().background(Color.black.opacity(0.25))
    }

    private func cardView(_ card: IosCollectionsPosterCard) -> some View {
        HStack(spacing: card.place.roaster ? 16 : 24) {
            posterPhoto(card.place).frame(width: card.place.roaster ? 128 : 296, height: CGFloat(card.height) - 24)
                .clipped().clipShape(RoundedRectangle(cornerRadius: 20))
            VStack(alignment: .leading, spacing: 8) {
                Text(card.place.title).font(.system(size: card.place.roaster ? 30 : 38, weight: .bold)).lineLimit(2)
                if !card.place.address.isEmpty {
                    Text(card.place.address).font(.system(size: 22)).foregroundStyle(.gray).lineLimit(2)
                }
            }.frame(maxWidth: .infinity, alignment: .leading)
            if !card.place.roaster, let value = card.place.rating?.doubleValue, value > 0 && value <= 5 {
                Label(String(format: "%.1f", value), systemImage: "star.fill").font(.system(size: 28, weight: .bold))
                    .labelStyle(.titleAndIcon).foregroundStyle(CoffeePeekNativeStyle.accent)
            }
            Image(systemName: "chevron.forward").font(.system(size: 24)).foregroundStyle(.gray)
        }.padding(12)
            .background(LinearGradient(colors: [Color.white.opacity(0.12), Color.white.opacity(0.06)],
                startPoint: .topLeading, endPoint: .bottomTrailing), in: RoundedRectangle(cornerRadius: 26))
            .overlay { RoundedRectangle(cornerRadius: 26).strokeBorder(Color.white.opacity(0.18), lineWidth: 1) }
    }

    @ViewBuilder private func posterPhoto(_ place: IosCollectionsWorkspacePlace) -> some View {
        if let image = photos[place.key] {
            if place.roaster { Image(uiImage: image).resizable().scaledToFit().padding(10).background(.white) }
            else { Image(uiImage: image).resizable().scaledToFill() }
        } else { CoffeePeekPhotoPlaceholder() }
    }
}

@MainActor
func renderCollectionPosters(_ poster: IosCollectionsPoster, workspace: IosCollectionsWorkspace, strings: CollectionsWorkspaceStrings) async -> [Data] {
    let photos = await loadCollectionPosterPhotos(poster, workspace: workspace)
    guard !Task.isCancelled else { return [] }
    var result: [Data] = []
    for page in poster.pages.indices {
        guard !Task.isCancelled else { return [] }
        let view = CollectionPosterCanvas(poster: poster, page: page, photos: photos, strings: strings)
            .frame(width: CGFloat(poster.width), height: CGFloat(poster.height))
        let renderer = ImageRenderer(content: view)
        renderer.scale = 1
        guard let image = renderer.uiImage, let png = image.pngData() else { return [] }
        result.append(png)
    }
    return result
}

@MainActor
private func loadCollectionPosterPhotos(_ poster: IosCollectionsPoster, workspace: IosCollectionsWorkspace) async -> [String: UIImage] {
    let places = poster.pages.flatMap(\.cards).map(\.place)
    var result: [String: UIImage] = [:]
    let deadline = Date().addingTimeInterval(12)
    for start in stride(from: 0, to: places.count, by: 4) {
        guard !Task.isCancelled && Date() < deadline else { break }
        let batch = Array(places[start..<min(start + 4, places.count)])
        let loaded = await withTaskGroup(of: (String, UIImage?).self) { group in
            for place in batch {
                group.addTask {
                    guard let url = place.photoUrl else { return (place.key, nil) }
                    let request = await CollectionPosterPhotoRequest()
                    return (place.key, await request.load(workspace: workspace, url: url))
                }
            }
            var values: [(String, UIImage?)] = []
            for await value in group { values.append(value) }
            return values
        }
        for (key, image) in loaded { result[key] = image }
    }
    return result
}

@MainActor
private final class CollectionPosterPhotoRequest {
    private var observation: IosCollectionsWorkspaceObservation?
    private var continuation: CheckedContinuation<UIImage?, Never>?
    private var timeout: Task<Void, Never>?

    func load(workspace: IosCollectionsWorkspace, url: String) async -> UIImage? {
        await withTaskCancellationHandler {
            await withCheckedContinuation { continuation in
                self.continuation = continuation
                let observation = workspace.loadPhoto(url: url) { [weak self] data in
                    self?.finish(data.flatMap { UIImage(data: $0 as Data) })
                }
                if self.continuation == nil { observation.cancel() } else { self.observation = observation }
                timeout = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 4_000_000_000)
                    self?.finish(nil)
                }
                if Task.isCancelled { finish(nil) }
            }
        } onCancel: { Task { @MainActor [weak self] in self?.finish(nil) } }
    }

    private func finish(_ image: UIImage?) {
        guard let continuation else { return }
        self.continuation = nil
        observation?.cancel()
        timeout?.cancel()
        continuation.resume(returning: image)
    }
}

private var collectionPosterSample: IosCollectionsPoster {
    let place = IosCollectionsWorkspacePlace(key: "shop", title: "Любимая кофейня", address: "Минск · Кофейная, 1",
        roaster: false, photoUrl: nil, selected: true, rating: nil, checkIns: nil)
    let card = IosCollectionsPosterCard(place: place, left: 32, top: 178, width: 1016, height: 216)
    return IosCollectionsPoster(id: "preview", title: "Кофейные выходные", url: "https://coffeepeek.by", width: 1080,
        height: 1350, pages: [IosCollectionsPosterPage(cards: [card])])
}

#Preview("Collection poster light") {
    CollectionPosterCanvas(poster: collectionPosterSample, page: 0, photos: [:], strings: CollectionsWorkspaceStrings(languageTag: "ru"))
        .frame(width: 1080, height: 1350).scaleEffect(0.3, anchor: .topLeading).preferredColorScheme(.light)
}
#Preview("Collection poster dark") {
    CollectionPosterCanvas(poster: collectionPosterSample, page: 0, photos: [:], strings: CollectionsWorkspaceStrings(languageTag: "ru"))
        .frame(width: 1080, height: 1350).scaleEffect(0.3, anchor: .topLeading).preferredColorScheme(.dark)
}

