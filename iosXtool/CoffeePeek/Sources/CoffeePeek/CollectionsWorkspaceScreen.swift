import ComposeApp
import ImageIO
import PhotosUI
import SwiftUI
import UIKit

@MainActor
func makeCollectionsWorkspaceController(workspace: IosCollectionsWorkspace, languageTag: String) -> UIViewController {
    let controller = UIHostingController(rootView: CollectionsWorkspaceHost(workspace: workspace,
        strings: CollectionsWorkspaceStrings(languageTag: languageTag)))
    controller.view.backgroundColor = .clear
    return controller
}

@MainActor
final class CollectionsWorkspaceModel: ObservableObject {
    let workspace: IosCollectionsWorkspace
    let strings: CollectionsWorkspaceStrings
    @Published var state: IosCollectionsWorkspaceState
    private var observation: IosCollectionsWorkspaceObservation?
    private var exportTask: Task<Void, Never>?

    init(workspace: IosCollectionsWorkspace, strings: CollectionsWorkspaceStrings) {
        self.workspace = workspace
        self.strings = strings
        state = workspace.current()
    }

    func appear() {
        guard observation == nil else { return }
        state = workspace.current()
        observation = workspace.observe(onState: { [weak self] in
            guard let self else { return }
            let next = workspace.current()
            if next.accountVersion != state.accountVersion || !next.busy { exportTask?.cancel() }
            state = next
        }, onExport: { [weak self] poster, epoch in self?.export(poster, epoch: epoch) })
        if ["Owner", "Preference", "Featured"].contains(state.mode) { workspace.reload() }
    }

    func disappear() {
        observation?.cancel()
        observation = nil
        exportTask?.cancel()
    }

    private func export(_ poster: IosCollectionsPoster, epoch: Int32) {
        exportTask?.cancel()
        exportTask = Task { [weak self] in
            guard let self else { return }
            let images = await renderCollectionPosters(poster, workspace: workspace, strings: strings)
            guard !Task.isCancelled else { return }
            workspace.finishExport(epoch: epoch, id: poster.id, images: images.map { $0 as NSData })
        }
    }

    deinit {
        observation?.cancel()
        exportTask?.cancel()
        workspace.close()
    }
}

private struct CollectionsWorkspaceHost: View {
    @StateObject private var model: CollectionsWorkspaceModel
    let strings: CollectionsWorkspaceStrings
    @State private var selectingCover = false
    @State private var coverItem: PhotosPickerItem?
    @State private var coverVersion: Int32 = 0
    @State private var deleting = false
    @State private var choosingFeatured = false
    @State private var reloading = false
    @Environment(\.dismiss) private var dismiss

    init(workspace: IosCollectionsWorkspace, strings: CollectionsWorkspaceStrings) {
        _model = StateObject(wrappedValue: CollectionsWorkspaceModel(workspace: workspace, strings: strings))
        self.strings = strings
    }

    var body: some View {
        Group {
            if model.state.preview { CollectionPosterPreview(model: model, strings: strings) }
            else { content }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
        .onAppear(perform: model.appear).onDisappear(perform: model.disappear)
        .onChange(of: model.state.accountVersion) { _ in
            coverItem = nil; selectingCover = false; choosingFeatured = false; deleting = false
        }
        .photosPicker(isPresented: $selectingCover, selection: $coverItem, matching: .images)
        .onChange(of: coverItem) { item in
            guard let item else { return }
            let version = coverVersion
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    model.workspace.coverPicked(data: data as NSData, version: version)
                }
                coverItem = nil
            }
        }
        .confirmationDialog(strings["collections_delete_title"], isPresented: $deleting, titleVisibility: .visible) {
            Button(strings["collections_delete_list"], role: .destructive, action: model.workspace.delete)
            Button(strings["collections_cancel"], role: .cancel) {}
        } message: { Text(strings["collections_delete_message"]) }
        .confirmationDialog(strings["collections_reload_title"], isPresented: $reloading, titleVisibility: .visible) {
            Button(strings["collections_reload_confirm"], role: .destructive, action: model.workspace.reload)
            Button(strings["collections_cancel"], role: .cancel) {}
        } message: { Text(strings["collections_reload_message"]) }
        .sheet(isPresented: $choosingFeatured) { featuredChoices }
    }

    private var state: IosCollectionsWorkspaceState { model.state }
    private var content: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                status
                switch state.mode {
                case "Editor": if state.signedIn && !state.loading { editor }
                case "Preference": preferences
                case "Owner": owner
                case "Featured": featured
                default: publicCollection
                }
            }.padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .safeAreaInset(edge: .bottom, spacing: 0) { bottomAction }
        .refreshable { model.workspace.reload() }
    }

    @ViewBuilder private var header: some View {
        if ["Editor", "Public"].contains(state.mode) {
            HStack(spacing: 8) {
                Button(action: model.workspace.back) {
                    Label(strings["collections_back"], systemImage: "chevron.backward")
                        .labelStyle(.iconOnly).frame(minWidth: 44, minHeight: 44)
                }
                Text(strings[state.mode == "Editor" ? (state.id == nil ? "collections_new_list" : "collections_edit_list") :
                    "collections_library"])
                    .font(CoffeePeekNativeStyle.font(18, weight: "SemiBold")).accessibilityAddTraits(.isHeader)
                Spacer()
                if state.mode == "Editor" && state.id != nil {
                    Menu {
                        if state.canShare { Button(strings["collections_share_link"], action: model.workspace.shareLink) }
                        if state.canPreview { Button(strings["collections_share_photo"], action: model.workspace.preview) }
                        Button(strings["collections_delete_list"], role: .destructive) { deleting = true }
                    } label: { Image(systemName: "ellipsis").frame(minWidth: 44, minHeight: 44) }
                    .disabled(state.busy).accessibilityLabel(strings["collections_more_actions"])
                }
            }.padding(.horizontal, 8).background(.bar)
        }
    }

    @ViewBuilder private var status: some View {
        if state.loading || state.refreshing || state.preparingCover {
            ProgressView(strings["collections_loading"]).frame(maxWidth: .infinity)
        }
        if !state.signedIn && ["Editor", "Owner", "Preference"].contains(state.mode) {
            Text(strings["collections_sign_in_hint"])
            Button(strings["collections_sign_in"], action: model.workspace.signIn).buttonStyle(.borderedProminent)
        }
        if let failure = state.failure {
            Text(strings[failureKey(failure)]).foregroundStyle(.red)
            if failure == "Load" || failure == "Conflict" {
                Button(strings["collections_retry"]) {
                    if failure == "Conflict" { reloading = true } else { model.workspace.reload() }
                }.disabled(state.loading || state.busy)
            }
        }
    }

    private var editor: some View {
        VStack(alignment: .leading, spacing: 20) {
            CollectionEditorContent(title: state.title, description: state.description, isPublic: state.isPublic,
                enabled: !state.busy, strings: strings, onTitle: model.workspace.title,
                onDescription: model.workspace.editDescription, onVisibility: model.workspace.visibility) { coverEditor }
            if state.id != nil {
                Text(strings["collections_manage_places"]).font(.headline)
                if state.places.isEmpty { Text(strings["collections_saved_empty"]) }
                ForEach(state.places, id: \.key) { place in editorPlace(place) }
            }
        }
    }

    private var coverEditor: some View {
        VStack {
            WorkspaceCover(workspace: model.workspace, id: state.id ?? "draft", version: state.accountVersion)
                .id(state.coverKey).frame(width: 108, height: 108).clipped()
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(alignment: .bottomTrailing) {
                    Button { coverVersion = state.accountVersion; selectingCover = true } label: {
                        Image(systemName: "pencil").frame(width: 44, height: 44)
                            .background(CoffeePeekNativeStyle.surface, in: Circle())
                    }.accessibilityLabel(strings["collections_change_cover"])
                        .disabled(state.busy || state.preparingCover)
                }
            if state.hasCover {
                Button(strings["collections_remove_cover"], action: model.workspace.removeCover)
                    .font(.caption).disabled(state.busy)
            }
        }
    }

    private func editorPlace(_ place: IosCollectionsWorkspacePlace) -> some View {
        HStack(spacing: 12) {
            Button { model.workspace.togglePlace(key: place.key) } label: {
                Image(systemName: place.selected ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(place.selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
                    .frame(width: 44, height: 44)
            }.accessibilityLabel(place.title)
                .accessibilityValue(String(format: strings["collections_selected"], Int32(place.selected ? 1 : 0)))
            placeRow(place)
            Menu {
                Button(strings["collections_move_up"]) { move(place, offset: -1) }.disabled(neighbor(place, offset: -1) == nil)
                Button(strings["collections_move_down"]) { move(place, offset: 1) }.disabled(neighbor(place, offset: 1) == nil)
            } label: { Image(systemName: "arrow.up.arrow.down").frame(width: 44, height: 44) }
                .accessibilityLabel(strings["collections_more_actions"])
        }.disabled(state.busy)
    }

    private func neighbor(_ place: IosCollectionsWorkspacePlace, offset: Int) -> IosCollectionsWorkspacePlace? {
        let group = state.places.filter { $0.roaster == place.roaster }
        guard let index = group.firstIndex(where: { $0.key == place.key }), group.indices.contains(index + offset) else { return nil }
        return group[index + offset]
    }

    private func move(_ place: IosCollectionsWorkspacePlace, offset: Int) {
        if let target = neighbor(place, offset: offset) {
            model.workspace.movePlace(key: place.key, target: target.key, after: offset > 0)
        }
    }

    @ViewBuilder private var bottomAction: some View {
        if state.mode == "Editor" {
            Button(action: model.workspace.save) {
                HStack { if state.busy { ProgressView() }; Text(strings["collections_save"]) }.frame(maxWidth: .infinity, minHeight: 44)
            }.buttonStyle(.borderedProminent).tint(CoffeePeekNativeStyle.accent).foregroundStyle(.black)
                .disabled(!state.canSave).padding(16).background(.bar)
        } else if state.mode == "Public" && state.id != nil {
            HStack {
                if state.canShare { Button(strings["collections_share_link"], action: model.workspace.shareLink) }
                if state.canPreview { Button(strings["collections_share_photo"], action: model.workspace.preview) }
            }.frame(maxWidth: .infinity, minHeight: 44).padding(8).background(.bar)
        }
    }

    private var owner: some View {
        VStack(alignment: .leading, spacing: 16) {
            ForEach([true, false], id: \.self) { isPublic in
                let lists = state.lists.filter { $0.isPublic == isPublic }
                if !lists.isEmpty {
                    Text(strings[isPublic ? "collections_public_lists" : "collections_private_lists"]).font(.headline)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 145))], spacing: 16) {
                        ForEach(lists, id: \.id) { item in
                            Button { model.workspace.openCollection(id: item.id) } label: { collectionCard(item) }.buttonStyle(.plain)
                        }
                    }
                }
            }
            if state.signedIn && !state.loading && state.lists.isEmpty && state.failure == nil {
                Text(strings["collections_library_empty"])
            }
        }
    }

    private func collectionCard(_ item: IosCollectionsWorkspaceList) -> some View {
        CollectionSummaryCard(title: item.title, count: item.count, strings: strings) {
            WorkspaceCover(workspace: model.workspace, id: item.id, version: state.accountVersion).id(item.imageKey)
        }
    }

    @ViewBuilder private var featured: some View {
        if let id = state.id {
            Button { model.workspace.openCollection(id: id) } label: {
                VStack(alignment: .leading, spacing: 8) {
                    WorkspaceCover(workspace: model.workspace, id: id, version: state.accountVersion)
                        .id(state.coverKey).aspectRatio(1.7, contentMode: .fit).clipped().clipShape(RoundedRectangle(cornerRadius: 20))
                    Text(state.title).font(.headline)
                    Text(strings.count(Int32(state.places.count))).font(.caption)
                }
            }.buttonStyle(.plain)
        }
    }

    private var preferences: some View {
        CollectionProfilePreferenceContent(visible: state.featuredId != nil,
            selectedTitle: state.lists.first { $0.id == state.featuredId }?.title,
            enabled: !state.loading && !state.busy && state.failure != "Load", strings: strings,
            onVisibility: { showing in
                if !state.signedIn { model.workspace.signIn() }
                else if showing { choosingFeatured = true } else { model.workspace.feature(id: nil) }
            }, onChoose: { choosingFeatured = true }, onDone: { dismiss() })
    }

    private var featuredChoices: some View {
        NavigationStack {
            List {
                Text(strings["collections_profile_choice_hint"]).font(.caption)
                choice(title: strings["collections_profile_none"], id: nil)
                ForEach(state.lists.filter(\.isPublic), id: \.id) { item in choice(title: item.title, id: item.id) }
                if !state.lists.contains(where: \.isPublic) { Text(strings["collections_profile_no_public"]) }
            }.navigationTitle(strings["collections_profile_list"]).navigationBarTitleDisplayMode(.inline)
                .toolbar { Button(strings["collections_cancel"]) { choosingFeatured = false } }
        }.presentationDetents([.medium, .large])
    }

    private func choice(title: String, id: String?) -> some View {
        Button {
            model.workspace.feature(id: id); choosingFeatured = false
        } label: {
            HStack { Text(title); Spacer(); if state.featuredId == id { Image(systemName: "checkmark") } }
        }
    }

    private var publicCollection: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let id = state.id {
                WorkspaceCover(workspace: model.workspace, id: id, version: state.accountVersion)
                    .id(state.coverKey).aspectRatio(1.6, contentMode: .fit).clipped().clipShape(RoundedRectangle(cornerRadius: 20))
                Text(state.title).font(.title2)
                if !state.description.isEmpty { Text(state.description) }
                Text(strings.count(Int32(state.places.count))).foregroundStyle(CoffeePeekNativeStyle.secondary)
                ForEach(state.places, id: \.key) { place in
                    Button { model.workspace.openPlace(key: place.key) } label: { placeRow(place) }.buttonStyle(.plain)
                }
                if state.places.isEmpty { Text(strings["collections_saved_empty"]) }
            }
        }
    }

    private func placeRow(_ place: IosCollectionsWorkspacePlace) -> some View {
        HStack(spacing: 12) {
            WorkspacePhoto(workspace: model.workspace, url: place.photoUrl)
                .frame(width: 52, height: 52).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 4) {
                Text(place.title).font(.headline)
                if !place.address.isEmpty { Text(place.address).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            }.frame(maxWidth: .infinity, alignment: .leading)
        }.frame(minHeight: 60)
    }

    private func failureKey(_ value: String) -> String {
        switch value {
        case "Load": "collections_saved_load_failed"
        case "Conflict": "collections_conflict"
        case "Cover": "collections_cover_failed"
        case "Membership": "collections_membership_failed"
        case "Delete": "collections_delete_failed"
        default: "collections_saved_save_failed"
        }
    }
}

struct CollectionsWorkspaceStrings {
    private let bundle: Bundle
    init(languageTag: String) {
        let url = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = url.flatMap(Bundle.init(url:)) ?? .main
    }
    subscript(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "CollectionsWorkspace") }
    func count(_ value: Int32) -> String { String(format: self["collections_saved_count"], value) }
}

struct WorkspaceCover: View {
    let workspace: IosCollectionsWorkspace
    let id: String
    let version: Int32
    @State private var image: UIImage?
    @State private var url: String?
    @State private var observation: IosCollectionsWorkspaceObservation?
    var body: some View {
        ZStack {
            CoffeePeekPhotoPlaceholder()
            if let image { Image(uiImage: image).resizable().scaledToFill() }
            else if let url { WorkspacePhoto(workspace: workspace, url: url) }
        }.accessibilityHidden(true)
            .onAppear {
                guard observation == nil else { return }
                observation = workspace.loadCover(id: id, version: version) { data, remote in
                    image = data.flatMap { UIImage(data: $0 as Data) }; url = remote
                }
            }.onDisappear { observation?.cancel(); observation = nil; image = nil; url = nil }
    }
}

struct WorkspacePhoto: View {
    let workspace: IosCollectionsWorkspace
    let url: String?
    @State private var image: UIImage?
    @State private var observation: IosCollectionsWorkspaceObservation?
    var body: some View {
        ZStack {
            CoffeePeekPhotoPlaceholder()
            if let image { Image(uiImage: image).resizable().scaledToFill() }
        }.accessibilityHidden(true)
            .task(id: url) {
                observation?.cancel(); image = nil
                guard let url, !url.isEmpty else { return }
                observation = workspace.loadPhoto(url: url) { data in image = data.flatMap { collectionWorkspaceImage($0 as Data) } }
            }.onDisappear { observation?.cancel(); observation = nil; image = nil }
    }
}

func collectionWorkspaceImage(_ data: Data) -> UIImage? {
    guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
    let options: [CFString: Any] = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceThumbnailMaxPixelSize: 640,
        kCGImageSourceShouldCacheImmediately: true
    ]
    guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
    return UIImage(cgImage: image)
}

