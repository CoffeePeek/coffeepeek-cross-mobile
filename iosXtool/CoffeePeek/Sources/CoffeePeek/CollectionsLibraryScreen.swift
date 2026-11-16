import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCollectionsLibraryController(
    library: IosCollectionsLibrary,
    languageTag: String
) -> UIViewController {
    UIHostingController(rootView: CollectionsLibraryHost(
        library: library,
        strings: CollectionsLibraryStrings(languageTag: languageTag)
    ))
}

private final class CollectionsLibraryModel: ObservableObject {
    let library: IosCollectionsLibrary
    @Published var state: IosCollectionsLibraryState
    private var observation: IosCollectionsLibraryObservation?

    init(library: IosCollectionsLibrary) {
        self.library = library
        state = library.current()
    }

    func appear() {
        guard observation == nil else { return }
        state = library.current()
        observation = library.observe { [weak self] state in self?.state = state }
        library.reload()
    }

    func disappear() {
        observation?.cancel()
        observation = nil
    }

    deinit {
        observation?.cancel()
        library.close()
    }
}

private struct CollectionsLibraryHost: View {
    @StateObject private var model: CollectionsLibraryModel
    let strings: CollectionsLibraryStrings

    init(library: IosCollectionsLibrary, strings: CollectionsLibraryStrings) {
        _model = StateObject(wrappedValue: CollectionsLibraryModel(library: library))
        self.strings = strings
    }

    var body: some View {
        CollectionsLibraryScreen(
            state: model.state,
            strings: strings,
            onQuery: { model.library.query(value: $0) },
            onReload: model.library.reload,
            onBack: model.library.back,
            onSignIn: model.library.signIn,
            onEditor: { model.library.openEditor(id: $0) }
        ) { item in
            CollectionsLibraryCover(
                library: model.library,
                item: item,
                accountVersion: model.state.accountVersion
            )
            .id(item.imageKey)
        }
        .onAppear(perform: model.appear)
        .onDisappear(perform: model.disappear)
    }
}

private struct CollectionsLibraryScreen<Cover: View>: View {
    let state: IosCollectionsLibraryState
    let strings: CollectionsLibraryStrings
    let onQuery: (String) -> Void
    let onReload: () -> Void
    let onBack: () -> Void
    let onSignIn: () -> Void
    let onEditor: (String?) -> Void
    @ViewBuilder let cover: (IosCollectionsLibraryItem) -> Cover
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                status
                if state.signedIn {
                    search
                    section(isPublic: true)
                    section(isPublic: false)
                    emptyState
                }
            }
            .padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private var header: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Label(strings.text("library.back"), systemImage: "chevron.backward")
                    .labelStyle(.iconOnly).frame(minWidth: 44, minHeight: 44)
            }
            Text(strings.text("library.title")).font(.headline).accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            Button { onEditor(nil) } label: {
                Label(strings.text("library.new"), systemImage: "plus")
                    .labelStyle(.iconOnly).frame(minWidth: 44, minHeight: 44)
            }
            .disabled(!state.signedIn)
        }
        .padding(.horizontal, 12)
        .background(.bar)
    }

    @ViewBuilder private var status: some View {
        if state.loading || state.refreshing {
            ProgressView(strings.text("library.loading")).frame(maxWidth: .infinity)
        }
        if !state.signedIn {
            Text(strings.text("library.signInHint"))
            Button(strings.text("library.signIn"), action: onSignIn).buttonStyle(.borderedProminent)
        }
        if state.failed {
            Text(strings.text("library.error")).foregroundStyle(.red)
            Button(strings.text("library.retry"), action: onReload).disabled(state.loading)
        }
    }

    private var search: some View {
        HStack {
            Image(systemName: "magnifyingglass").accessibilityHidden(true)
            TextField(strings.text("library.search"), text: Binding(get: { state.query }, set: onQuery))
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
            if !state.query.isEmpty {
                Button { onQuery("") } label: {
                    Label(strings.text("library.clearSearch"), systemImage: "xmark.circle.fill").labelStyle(.iconOnly)
                }
                .frame(minWidth: 44, minHeight: 44)
            }
        }
        .padding(.horizontal, 12).frame(minHeight: 48)
        .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder private func section(isPublic: Bool) -> some View {
        let items = filteredItems.filter { $0.isPublic == isPublic }
        if !items.isEmpty {
            Text(strings.text(isPublic ? "library.public" : "library.private"))
                .font(.headline).accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: columns, spacing: 16) {
                ForEach(items, id: \.id) { item in
                    Button { onEditor(item.id) } label: { card(item) }.buttonStyle(.plain)
                }
            }
        }
    }

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: dynamicTypeSize.isAccessibilitySize ? 280 : 150), spacing: 16)]
    }

    private var filteredItems: [IosCollectionsLibraryItem] {
        state.items.filter { state.query.isEmpty || $0.title.localizedCaseInsensitiveContains(state.query) }
    }

    @ViewBuilder private var emptyState: some View {
        if !state.loading && !state.failed && state.items.isEmpty {
            Text(strings.text("library.empty"))
        } else if !state.items.isEmpty && filteredItems.isEmpty {
            Text(strings.text("library.noMatches"))
        }
    }

    private func card(_ item: IosCollectionsLibraryItem) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            cover(item).frame(height: dynamicTypeSize.isAccessibilitySize ? 160 : 128).clipped()
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title).font(.headline).foregroundStyle(.primary)
                Text(strings.count(item.count)).font(.subheadline).foregroundStyle(.secondary)
            }
            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

private struct CollectionsLibraryCover: View {
    let library: IosCollectionsLibrary
    let item: IosCollectionsLibraryItem
    let accountVersion: Int32
    @State private var image: UIImage?
    @State private var photoURL: URL?
    @State private var observation: IosCollectionsLibraryObservation?

    var body: some View {
        ZStack {
            Color(uiColor: .tertiarySystemFill)
            Image(systemName: "photo").foregroundStyle(.secondary)
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else if let photoURL {
                AsyncImage(url: photoURL) { result in
                    if let image = result.image { image.resizable().scaledToFill() }
                }
            }
        }
        .accessibilityHidden(true)
        .onAppear(perform: load)
        .onDisappear {
            observation?.cancel()
            observation = nil
            image = nil
            photoURL = nil
        }
    }

    private func load() {
        guard observation == nil else { return }
        observation = library.loadCover(id: item.id, accountVersion: accountVersion) { data, url in
            image = data.flatMap { UIImage(data: $0 as Data) }
            photoURL = url.flatMap(URL.init(string:))
        }
    }
}

private struct CollectionsLibraryStrings {
    private let bundle: Bundle

    init(languageTag: String) {
        let localization = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = localization.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: "CollectionsLibrary")
    }

    func count(_ value: Int32) -> String { String(format: text("library.count"), value) }
}

private var libraryPreviewState: IosCollectionsLibraryState {
    IosCollectionsLibraryState(
        accountVersion: 0,
        signedIn: true,
        loading: false,
        refreshing: false,
        failed: false,
        query: "",
        items: [
            IosCollectionsLibraryItem(
                id: "one", title: "Кофейни выходного дня", isPublic: true, count: 7, imageKey: "one"
            ),
            IosCollectionsLibraryItem(
                id: "two", title: "Избранное", isPublic: false, count: 3, imageKey: "two"
            )
        ]
    )
}

#Preview("Library Light") {
    CollectionsLibraryScreen(
        state: libraryPreviewState,
        strings: CollectionsLibraryStrings(languageTag: "ru"),
        onQuery: { _ in },
        onReload: {},
        onBack: {},
        onSignIn: {},
        onEditor: { _ in }
    ) { _ in Color.gray.opacity(0.2) }
    .preferredColorScheme(.light)
}

#Preview("Library Dark") {
    CollectionsLibraryScreen(
        state: libraryPreviewState,
        strings: CollectionsLibraryStrings(languageTag: "be"),
        onQuery: { _ in },
        onReload: {},
        onBack: {},
        onSignIn: {},
        onEditor: { _ in }
    ) { _ in Color.gray.opacity(0.2) }
    .preferredColorScheme(.dark)
    .environment(\.dynamicTypeSize, .accessibility2)
}
