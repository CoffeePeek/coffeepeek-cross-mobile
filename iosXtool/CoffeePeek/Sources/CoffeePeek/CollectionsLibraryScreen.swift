import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCollectionsLibraryController(library: IosCollectionsLibrary, languageTag: String) -> UIViewController {
    CollectionsLibraryHostingController(library: library, strings: CollectionsLibraryStrings(languageTag: languageTag))
}

@MainActor
private final class CollectionsLibraryHostingController: UIHostingController<CollectionsLibraryHost> {
    private let model: CollectionsLibraryModel
    private let library: IosCollectionsLibrary
    init(library: IosCollectionsLibrary, strings: CollectionsLibraryStrings) {
        self.library = library
        model = CollectionsLibraryModel(library: library)
        super.init(rootView: CollectionsLibraryHost(model: model, strings: strings))
    }
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(library:strings:)") }
    isolated deinit { library.close() }
}

@MainActor
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
        observation = library.observe { [weak self] state in
            self?.state = state
        }
        library.reload()
    }

    func disappear() {
        observation?.cancel()
        observation = nil
    }

    isolated deinit {
        observation?.cancel()
    }
}

private struct CollectionsLibraryHost: View {
    @ObservedObject var model: CollectionsLibraryModel
    let strings: CollectionsLibraryStrings

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
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }

    private var header: some View {
        CoffeePeekNativeNavigationHeader(title: strings.text("library.title"),
            backLabel: strings.text("library.back"), back: onBack)
        .overlay(alignment: .trailing) {
            Button { onEditor(nil) } label: {
                Label(strings.text("library.new"), systemImage: "plus")
                    .labelStyle(.iconOnly).frame(width: 44, height: 44)
            }
            .buttonStyle(.plain).shopDetailGlassCircle()
            .disabled(!state.signedIn)
            .padding(.trailing, 12)
        }
    }

    @ViewBuilder private var status: some View {
        if state.loading || state.refreshing {
            CoffeePeekNativeLoader(label: strings.text("library.loading"), size: 32, showsLabel: true).frame(maxWidth: .infinity)
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
            TextField(strings.text("library.search"), text: Binding(get: { state.query }, set: { onQuery($0) }))
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
        Array(repeating: GridItem(.flexible(minimum: 0), spacing: 16, alignment: .top), count: 2)
    }

    private var filteredItems: [IosCollectionsLibraryItem] {
        state.matchingItems
    }

    @ViewBuilder private var emptyState: some View {
        if !state.loading && !state.failed && state.items.isEmpty {
            Text(strings.text("library.empty"))
        } else if !state.items.isEmpty && filteredItems.isEmpty {
            Text(strings.text("library.noMatches"))
        }
    }

    private func card(_ item: IosCollectionsLibraryItem) -> some View {
        CollectionSummaryCard(title: item.title, countText: strings.count(item.count)) { cover(item) }
    }
}

private struct CollectionsLibraryCover: View {
    let library: IosCollectionsLibrary
    let item: IosCollectionsLibraryItem
    let accountVersion: Int32
    @State private var image: UIImage?
    @State private var observation: IosCollectionsLibraryObservation?

    var body: some View {
        ZStack {
            CoffeePeekPhotoPlaceholder()
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            }
        }
        .accessibilityHidden(true)
        .onAppear(perform: load)
        .onDisappear {
            observation?.cancel()
            observation = nil
        }
    }

    private func load() {
        guard observation == nil else { return }
        let state = library.current()
        guard state.accountVersion == accountVersion,
              state.items.contains(where: { $0.id == item.id && $0.imageKey == item.imageKey }) else {
            image = nil
            return
        }
        observation = library.loadCover(id: item.id, accountVersion: accountVersion) { data, _ in
            image = data.flatMap { coffeePeekNativeThumbnail($0 as Data) }
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
    let items = [
        IosCollectionsLibraryItem(id: "one", title: "Кофейни выходного дня", isPublic: true, count: 7, imageKey: "one"),
        IosCollectionsLibraryItem(id: "three", title: "Эспрессо и завтраки", isPublic: true, count: 4, imageKey: "three"),
        IosCollectionsLibraryItem(id: "four", title: "Новые кофейни у парка", isPublic: true, count: 2, imageKey: "four"),
        IosCollectionsLibraryItem(id: "two", title: "Избранное", isPublic: false, count: 3, imageKey: "two")
    ]
    return IosCollectionsLibraryState(
        accountVersion: 0,
        signedIn: true,
        loading: false,
        refreshing: false,
        failed: false,
        query: "",
        items: items, matchingItems: items
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
