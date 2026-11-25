import ComposeApp
import PhotosUI
import SwiftUI
import UIKit

@MainActor
func makeCollectionsWorkspaceController(workspace: IosCollectionsWorkspace, languageTag: String) -> UIViewController {
    let strings = CollectionsWorkspaceStrings(languageTag: languageTag)
    if workspace.current().mode == "Picker" {
        return CollectionsMembershipController(workspace: workspace, strings: strings)
    }
    return CollectionsWorkspaceHostingController(workspace: workspace, strings: strings)
}

@MainActor
private final class CollectionsMembershipController: UIHostingController<CollectionsMembershipHost>, UIAdaptivePresentationControllerDelegate {
    private let workspace: IosCollectionsWorkspace
    private let model: CollectionsWorkspaceModel

    init(workspace: IosCollectionsWorkspace, strings: CollectionsWorkspaceStrings) {
        self.workspace = workspace
        let model = CollectionsWorkspaceModel(workspace: workspace, strings: strings)
        self.model = model
        super.init(rootView: CollectionsMembershipHost(model: model))
        modalPresentationStyle = .pageSheet
        view.backgroundColor = .clear
        sheetPresentationController?.detents = [.medium(), .large()]
        sheetPresentationController?.prefersGrabberVisible = true
        sheetPresentationController?.prefersScrollingExpandsWhenScrolledToEdge = true
        presentationController?.delegate = self
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(workspace:strings:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        model.appear()
    }

    func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
        !workspace.current().busy
    }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        workspace.back()
    }
}

private struct CollectionsMembershipHost: View {
    @ObservedObject var model: CollectionsWorkspaceModel

    private var state: IosCollectionsWorkspaceState { model.state }
    private var enabled: Bool { state.signedIn && !state.loading && !state.busy && state.membershipsLoaded }

    var body: some View {
        CollectionsMembershipContent(strings: model.strings,
            items: state.matchingLists.map { CollectionMembershipItem(id: $0.id, title: $0.title,
                isPublic: $0.isPublic, count: $0.count, selected: $0.selected) },
            hasLists: !state.lists.isEmpty,
            query: Binding(get: { state.query }, set: { model.workspace.query(value: $0) }),
            loading: state.loading || state.refreshing, busy: state.busy,
            failure: state.failure, signedIn: state.signedIn, enabled: enabled,
            back: model.workspace.back, add: { model.workspace.quickTitle(value: "") },
            toggle: { model.workspace.toggleMembership(id: $0) },
            commit: model.workspace.commitMemberships, reload: model.workspace.reload,
            signIn: model.workspace.signIn) { item in
                WorkspaceCover(workspace: model.workspace, id: item.id, version: state.accountVersion)
                    .id(state.lists.first { $0.id == item.id }?.imageKey)
            }
            .interactiveDismissDisabled(state.busy)
            .sheet(isPresented: Binding(get: { state.quickTitle != nil }, set: { visible in
                if !visible { model.workspace.quickTitle(value: nil) }
            })) { quickCreate }
    }

    private var quickCreate: some View {
        CollectionsQuickCreateContent(strings: model.strings,
            title: Binding(get: { state.quickTitle ?? "" }, set: { model.workspace.quickTitle(value: $0) }),
            busy: state.busy, enabled: state.canCreateQuick, failure: state.failure,
            cancel: { model.workspace.quickTitle(value: nil) }, create: model.workspace.createQuick)
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .interactiveDismissDisabled(state.busy)
    }
}

private struct CollectionMembershipItem: Identifiable {
    let id: String
    let title: String
    let isPublic: Bool
    let count: Int32
    let selected: Bool
}

private struct CollectionsMembershipContent<Cover: View>: View {
    let strings: CollectionsWorkspaceStrings
    let items: [CollectionMembershipItem]
    let hasLists: Bool
    @Binding var query: String
    let loading: Bool
    let busy: Bool
    let failure: String?
    let signedIn: Bool
    let enabled: Bool
    let back: () -> Void
    let add: () -> Void
    let toggle: (String) -> Void
    let commit: () -> Void
    let reload: () -> Void
    let signIn: () -> Void
    @ViewBuilder let cover: (CollectionMembershipItem) -> Cover

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                status
                HStack {
                    Image(systemName: "magnifyingglass").accessibilityHidden(true)
                    TextField(strings["collections_search_lists"], text: $query)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .submitLabel(.search).padding(.vertical, 12)
                    if !query.isEmpty {
                        Button { query = "" } label: {
                            Image(systemName: "xmark.circle.fill").frame(width: 44, height: 44)
                        }.buttonStyle(.plain).foregroundStyle(CoffeePeekNativeStyle.secondary)
                            .accessibilityLabel(strings["collections_clear_search"])
                    }
                }.padding(.leading, 14).padding(.trailing, query.isEmpty ? 14 : 2)
                    .disabled(busy).background(CoffeePeekNativeStyle.surface, in: Capsule())
                ForEach([true, false], id: \.self) { isPublic in
                    let matching = items.filter { $0.isPublic == isPublic }
                    if !matching.isEmpty {
                        Text(strings[isPublic ? "collections_public_lists" : "collections_private_lists"])
                            .font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
                        ForEach(matching) { item in membershipRow(item) }
                    }
                }
                if !loading && failure != "Load" && !hasLists {
                    Text(strings["collections_library_empty"])
                } else if hasLists && items.isEmpty { Text(strings["collections_no_matches"]) }
            }.padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            CoffeePeekNativeActionButton(title: strings["collections_done"], busy: busy,
                enabled: enabled, action: commit).padding(16).background(CoffeePeekNativeStyle.background)
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
    }

    private var header: some View {
        HStack(spacing: 12) {
            CoffeePeekNativeChevronButton(label: strings["collections_cancel"], symbol: "xmark", action: back)
                .disabled(busy)
            Text(strings["collections_save_to_list"]).font(CoffeePeekNativeStyle.font(18, weight: "Semibold"))
                .frame(maxWidth: .infinity, alignment: .leading)
            CoffeePeekNativeChevronButton(label: strings["collections_new_list"], symbol: "plus", action: add)
                .disabled(!enabled)
        }.padding(16).background(CoffeePeekNativeStyle.background)
    }

    @ViewBuilder private var status: some View {
        if loading { CoffeePeekNativeLoader(label: strings["collections_loading"]).frame(maxWidth: .infinity) }
        if !signedIn {
            Text(strings["collections_sign_in_hint"])
            CoffeePeekNativeActionButton(title: strings["collections_sign_in"], action: signIn)
        }
        if let failure {
            Text(strings[collectionWorkspaceFailureKey(failure)])
                .foregroundStyle(.red)
            if failure == "Load" { Button(strings["collections_retry"], action: reload).disabled(busy) }
        }
    }

    private func membershipRow(_ item: CollectionMembershipItem) -> some View {
        Button { toggle(item.id) } label: {
            HStack(spacing: 12) {
                cover(item).frame(width: 52, height: 52).clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                    Text(strings.count(item.count)).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
                }.frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: item.selected ? "checkmark.circle.fill" : "circle")
                    .font(.title3).foregroundStyle(item.selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
            }.padding(12).contentShape(Rectangle())
        }.buttonStyle(.plain).disabled(!enabled)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 20))
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(item.selected ? [.isSelected] : [])
    }
}

private struct CollectionsQuickCreateContent: View {
    let strings: CollectionsWorkspaceStrings
    @Binding var title: String
    let busy: Bool
    let enabled: Bool
    let failure: String?
    let cancel: () -> Void
    let create: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                TextField(strings["collections_name"], text: $title)
                    .padding(16).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                    .focused($focused).disabled(busy).submitLabel(.done)
                    .onSubmit { if enabled { create() } }
                Text(strings["collections_private_hint"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
                if let failure { Text(strings[collectionWorkspaceFailureKey(failure)]).foregroundStyle(.red) }
            }.padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            HStack {
                Text(strings["collections_new_list"]).font(CoffeePeekNativeStyle.font(20, weight: "Semibold"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                CoffeePeekNativeChevronButton(label: strings["collections_cancel"], symbol: "xmark", action: cancel)
                    .disabled(busy)
            }.padding(16).background(CoffeePeekNativeStyle.background)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            CoffeePeekNativeActionButton(title: strings["collections_create"], busy: busy,
                enabled: enabled, action: create).padding(16).background(CoffeePeekNativeStyle.background)
        }
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
        .onAppear { focused = true }
    }
}

#Preview("Membership picker light") {
    CollectionsMembershipContent(strings: CollectionsWorkspaceStrings(languageTag: "ru"),
        items: [CollectionMembershipItem(id: "weekend", title: "Выходные с кофе", isPublic: true, count: 5, selected: true)],
        hasLists: true,
        query: .constant(""), loading: false, busy: false, failure: nil, signedIn: true, enabled: true,
        back: {}, add: {}, toggle: { _ in }, commit: {}, reload: {}, signIn: {}) { _ in CoffeePeekPhotoPlaceholder() }
        .preferredColorScheme(.light)
}
#Preview("Membership picker dark") {
    CollectionsMembershipContent(strings: CollectionsWorkspaceStrings(languageTag: "ru"), items: [],
        hasLists: false,
        query: .constant(""), loading: false, busy: false, failure: "Load", signedIn: true, enabled: false,
        back: {}, add: {}, toggle: { _ in }, commit: {}, reload: {}, signIn: {}) { _ in CoffeePeekPhotoPlaceholder() }
        .preferredColorScheme(.dark)
}
#Preview("Quick collection light") {
    CollectionsQuickCreateContent(strings: CollectionsWorkspaceStrings(languageTag: "ru"), title: .constant("Выходные"),
        busy: false, enabled: true, failure: nil, cancel: {}, create: {}).preferredColorScheme(.light)
}
#Preview("Quick collection dark") {
    CollectionsQuickCreateContent(strings: CollectionsWorkspaceStrings(languageTag: "ru"), title: .constant(""),
        busy: false, enabled: false, failure: "Save", cancel: {}, create: {}).preferredColorScheme(.dark)
}

@MainActor
private final class CollectionsWorkspaceHostingController: UIHostingController<CollectionsWorkspaceHost>, CoffeePeekNativeContentSizing {
    private let model: CollectionsWorkspaceModel
    private let workspace: IosCollectionsWorkspace
    init(workspace: IosCollectionsWorkspace, strings: CollectionsWorkspaceStrings) {
        self.workspace = workspace
        model = CollectionsWorkspaceModel(workspace: workspace, strings: strings)
        super.init(rootView: CollectionsWorkspaceHost(model: model, strings: strings))
        view.backgroundColor = .clear
        if ["Owner", "Featured"].contains(workspace.current().mode) { sizingOptions = .intrinsicContentSize }
    }
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(workspace:strings:)") }
    func contentSize(width: CGFloat) -> CGSize {
        sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
    }
    isolated deinit { workspace.close() }
}

@MainActor
final class CollectionsWorkspaceModel: ObservableObject {
    let workspace: IosCollectionsWorkspace
    let strings: CollectionsWorkspaceStrings
    @Published var state: IosCollectionsWorkspaceState
    @Published var exportRequest: CollectionsPosterExport?
    private var observation: IosCollectionsWorkspaceObservation?

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
            if next.accountVersion != state.accountVersion || !next.busy { exportRequest = nil }
            state = next
        }, onExport: { [weak self] poster, epoch in
            self?.exportRequest = CollectionsPosterExport(poster: poster, epoch: epoch.int32Value)
        })
        if ["Owner", "Preference", "Featured"].contains(state.mode) { workspace.reload() }
    }

    isolated deinit {
        observation?.cancel()
    }
}

@MainActor
struct CollectionsPosterExport: Identifiable {
    let id = UUID()
    let poster: IosCollectionsPoster
    let epoch: Int32
}

private struct CollectionsWorkspaceHost: View {
    @ObservedObject var model: CollectionsWorkspaceModel
    let strings: CollectionsWorkspaceStrings
    @State private var selectingCover = false
    @State private var coverItem: PhotosPickerItem?
    @State private var coverVersion: Int32 = 0
    @State private var deleting = false
    @State private var choosingFeatured = false
    @State private var reloading = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if model.state.preview { CollectionPosterPreview(model: model, strings: strings) }
            else { content }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
        .tint(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
        .background(CoffeePeekLocalBackProtection(protected:
            model.state.preview && model.state.mode != "Snapshot" ||
            model.state.mode == "Editor" && model.state.hasEdits || model.state.busy))
        .onAppear(perform: model.appear)
        .task(id: model.exportRequest?.id) {
            guard let request = model.exportRequest else { return }
            let images = await renderCollectionPosters(request.poster, workspace: model.workspace, strings: strings)
            guard !Task.isCancelled, model.exportRequest?.id == request.id else { return }
            model.workspace.finishExport(epoch: request.epoch, id: request.poster.id, images: images)
            if model.exportRequest?.id == request.id { model.exportRequest = nil }
        }
        .onChange(of: model.state.accountVersion) { _, _ in
            coverItem = nil; selectingCover = false; choosingFeatured = false; deleting = false
        }
        .photosPicker(isPresented: $selectingCover, selection: $coverItem, matching: .images)
        .task(id: coverItem) {
            guard let item = coverItem else { return }
            let version = coverVersion
            if let data = try? await item.loadTransferable(type: Data.self),
               !Task.isCancelled, coverItem == item {
                model.workspace.coverPicked(data: data, version: version)
            }
            if !Task.isCancelled, coverItem == item { coverItem = nil }
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
    @ViewBuilder private var content: some View {
        if ["Owner", "Featured"].contains(state.mode) { rows }
        else { scrollingContent }
    }

    private var rows: some View {
        VStack(alignment: .leading, spacing: 16) { rowContents }.padding(16)
    }

    @ViewBuilder private var rowContents: some View {
        status
        switch state.mode {
        case "Editor": if state.signedIn && !state.loading { editor }
        case "Preference": preferences
        case "Owner": owner
        case "Featured": featured
        default: publicCollection
        }
    }

    private var scrollingContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) { rowContents }.padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .safeAreaInset(edge: .bottom, spacing: 0) { bottomAction }
        .refreshable { model.workspace.reload() }
    }

    @ViewBuilder private var header: some View {
        if ["Editor", "Public"].contains(state.mode) {
            CollectionsWorkspaceHeader(title: strings[state.mode == "Editor" ?
                (state.id == nil ? "collections_new_list" : "collections_edit_list") : "collections_library"],
                backLabel: strings["collections_back"], back: model.workspace.back) {
                if state.mode == "Editor" && state.id != nil {
                    Menu {
                        if state.canShare { Button(strings["collections_share_link"], action: model.workspace.shareLink) }
                        if state.canPreview { Button(strings["collections_share_photo"], action: model.workspace.preview) }
                        Button(strings["collections_delete_list"], role: .destructive) { deleting = true }
                    } label: { Image(systemName: "ellipsis").frame(minWidth: 44, minHeight: 44) }
                    .disabled(state.busy).accessibilityLabel(strings["collections_more_actions"])
                }
            }
        }
    }

    @ViewBuilder private var status: some View {
        if state.loading || state.refreshing || state.preparingCover {
            CoffeePeekNativeLoader(label: strings["collections_loading"], size: 32, showsLabel: true).frame(maxWidth: .infinity)
        }
        if !state.signedIn && ["Editor", "Owner", "Preference"].contains(state.mode) {
            Text(strings["collections_sign_in_hint"])
            Button(strings["collections_sign_in"], action: model.workspace.signIn).buttonStyle(.borderedProminent)
        }
        if let failure = state.failure {
            Text(strings[collectionWorkspaceFailureKey(failure)]).foregroundStyle(.red)
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
                HStack { if state.busy { CoffeePeekNativeLoader(color: .black, size: 24) }; Text(strings["collections_save"]) }.frame(maxWidth: .infinity, minHeight: 44)
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
            LazyVGrid(columns: collectionColumns, spacing: 16) {
                ForEach(state.lists, id: \.id) { item in
                    Button { model.workspace.openCollection(id: item.id) } label: {
                        collectionCard(item)
                    }.buttonStyle(.plain)
                }
            }
            if state.signedIn && !state.loading && state.lists.isEmpty && state.failure == nil {
                Text(strings["collections_library_empty"])
            }
        }
    }

    private var collectionColumns: [GridItem] {
        Array(repeating: GridItem(.flexible(minimum: 0), spacing: 16, alignment: .top), count: 2)
    }

    private func collectionCard(_ item: IosCollectionsWorkspaceList) -> some View {
        CollectionSummaryCard(title: item.title, count: item.count, strings: strings) {
            WorkspaceCover(workspace: model.workspace, id: item.id, version: state.accountVersion).id(item.imageKey)
        }
    }

    @ViewBuilder private var featured: some View {
        if let id = state.id {
            Text(state.title).font(.title2)
            LazyVGrid(columns: collectionColumns, spacing: 16) {
                Button { model.workspace.openCollection(id: id) } label: {
                    CollectionSummaryCard(title: state.title, count: Int32(state.places.count), strings: strings) {
                        WorkspaceCover(workspace: model.workspace, id: id, version: state.accountVersion).id(state.coverKey)
                    }
                }.buttonStyle(.plain)
            }
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

}

private func collectionWorkspaceFailureKey(_ value: String) -> String {
    switch value {
    case "Load": "collections_saved_load_failed"
    case "Conflict": "collections_conflict"
    case "Cover": "collections_cover_failed"
    case "Membership": "collections_membership_failed"
    case "Delete": "collections_delete_failed"
    default: "collections_saved_save_failed"
    }
}

private struct CollectionsWorkspaceHeader<Actions: View>: View {
    let title: String
    let backLabel: String
    let back: () -> Void
    @ViewBuilder let actions: Actions

    var body: some View {
        CoffeePeekNativeNavigationHeader(title: title, backLabel: backLabel, back: back)
            .overlay(alignment: .trailing) { actions.padding(.trailing, 12) }
    }
}

#Preview("Collection header light") {
    CollectionsWorkspaceHeader(title: "Новая подборка", backLabel: "Назад", back: {}) { EmptyView() }
        .preferredColorScheme(.light)
}
#Preview("Collection header dark") {
    CollectionsWorkspaceHeader(title: "Подборка", backLabel: "Назад", back: {}) {
        Image(systemName: "ellipsis").frame(width: 44, height: 44)
    }.preferredColorScheme(.dark)
}

struct CollectionsWorkspaceStrings {
    private let bundle: Bundle
    init(languageTag: String) {
        let url = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = url.flatMap(Bundle.init(url:)) ?? .main
    }
    subscript(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "CollectionsWorkspace") }
    func count(_ value: Int32) -> String { String(format: self["collections_saved_count"], value) }
    func checkIns(_ value: Int32) -> String {
        let suffix = (11...14).contains(value % 100) ? "many" :
            value % 10 == 1 ? "one" : (2...4).contains(value % 10) ? "few" : "many"
        return String(format: self["collections_checkins_\(suffix)"], value)
    }
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
                observation = workspace.loadPhoto(url: url) { data in image = data.flatMap { coffeePeekNativeThumbnail($0 as Data) } }
            }.onDisappear { observation?.cancel(); observation = nil; image = nil }
    }
}

