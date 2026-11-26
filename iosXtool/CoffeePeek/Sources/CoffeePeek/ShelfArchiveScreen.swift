import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShelfArchiveController(archive: IosShelfArchive, languageTag: String) -> UIViewController {
    UIHostingController(rootView: ShelfArchiveHost(archive: archive, languageTag: languageTag))
}

@MainActor
private final class ShelfArchiveModel: ObservableObject {
    let archive: IosShelfArchive
    @Published private(set) var state: IosShelfArchiveState
    private var observation: IosShelfArchiveObservation?

    init(archive: IosShelfArchive) {
        self.archive = archive
        state = archive.current()
    }

    func appear() {
        guard observation == nil else { return }
        state = archive.current()
        observation = archive.observe { [weak self] in self?.state = $0 }
    }

    func disappear() {
        observation?.cancel()
        observation = nil
    }

    isolated deinit {
        observation?.cancel()
        archive.close()
    }
}

struct ShelfArchiveHost: View {
    @StateObject private var model: ShelfArchiveModel
    private let strings: ShelfArchiveStrings
    let settings: AnyView?

    init(archive: IosShelfArchive, languageTag: String, settings: AnyView? = nil) {
        _model = StateObject(wrappedValue: ShelfArchiveModel(archive: archive))
        strings = ShelfArchiveStrings(languageTag)
        self.settings = settings
    }

    var body: some View {
        ShelfArchiveScreen(
            state: model.state, strings: strings,
            onQuery: { model.archive.query(value: $0) }, onBack: model.archive.back,
            onRetry: model.archive.retry, onMore: model.archive.more, onBag: { model.archive.openBag(id: $0) },
            settings: settings
        ) { item in
            ShelfArchiveCover(archive: model.archive, item: item)
        }
        .onAppear(perform: model.appear)
        .onDisappear(perform: model.disappear)
    }
}

private struct ShelfArchiveScreen<Cover: View>: View {
    let state: IosShelfArchiveState
    let strings: ShelfArchiveStrings
    let onQuery: (String) -> Void
    let onBack: () -> Void
    let onRetry: () -> Void
    let onMore: () -> Void
    let onBag: (String) -> Void
    var settings: AnyView? = nil
    @ViewBuilder let cover: (IosShelfArchiveRow) -> Cover

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 8) {
                if let settings {
                    settings
                    Divider().padding(.vertical, 12)
                    Text(strings.text("archive.title")).font(.title3.weight(.semibold)).accessibilityAddTraits(.isHeader)
                    if state.signedIn { search }
                }
                if state.signedIn {
                    archiveContent
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .safeAreaInset(edge: .top, spacing: 0) { if settings == nil { header } }
        .scrollDismissesKeyboard(.interactively)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea())
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            CoffeePeekNativeNavigationHeader(title: strings.text("archive.title"),
                backLabel: strings.text("archive.back"), back: onBack)
            if state.signedIn { search.padding(.horizontal, 16) }
        }
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.background)
    }

    private var search: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").accessibilityHidden(true)
            TextField(strings.text("archive.search"), text: Binding(get: { state.query }, set: { onQuery($0) }))
                .disableAutocorrection(true)
            if !state.query.isEmpty {
                Button { onQuery("") } label: {
                    Label(strings.text("archive.clear"), systemImage: "xmark.circle.fill")
                        .labelStyle(.iconOnly)
                        .frame(minWidth: 44, minHeight: 44)
                }
            }
        }
        .padding(.horizontal, 12)
        .frame(minHeight: 44)
        .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder private var archiveContent: some View {
        if state.loadFailed {
            error("archive.failed")
        } else if state.loading && state.sections.isEmpty {
            loader
        } else if state.sections.isEmpty {
            empty
        }
        if state.loading && !state.sections.isEmpty {
            CoffeePeekNativeLoader(size: 24).frame(maxWidth: .infinity).accessibilityLabel(strings.text("archive.loading"))
        }
        ForEach(state.sections, id: \.id) { section in
            Text(strings.month(section))
                .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold", relativeTo: .headline))
                .padding(.top, 12)
                .accessibilityAddTraits(.isHeader)
            ForEach(section.items, id: \.id) { item in
                row(item)
            }
        }
        if state.loadMoreFailed {
            error("archive.failedMore")
        } else if state.loadingMore {
            loader
        } else if state.hasMore && !state.loading {
            loader.onAppear(perform: onMore)
        }
    }

    private var loader: some View {
        CoffeePeekNativeLoader(label: strings.text("archive.loading"), size: 32, showsLabel: true)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
    }

    private var empty: some View {
        VStack(spacing: 8) {
            Text(strings.text(state.query.isEmpty ? "archive.empty" : "archive.noResults"))
                .font(.headline)
            if state.query.isEmpty {
                Text(strings.text("archive.emptyHint")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(24)
    }

    private func error(_ key: String) -> some View {
        VStack(spacing: 8) {
            Text(strings.text(key)).multilineTextAlignment(.center)
            Button(strings.text("archive.retry"), action: onRetry).buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity)
        .padding(16)
    }

    private func row(_ item: IosShelfArchiveRow) -> some View {
        Button { onBag(item.id) } label: {
            HStack(spacing: 12) {
                cover(item).frame(width: 56, height: 72).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title ?? strings.text("archive.untitled"))
                        .font(CoffeePeekNativeStyle.font(15, weight: "SemiBold"))
                    if let subtitle = strings.subtitle(item) {
                        Text(subtitle).font(.subheadline).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }
                    if let rating = item.rating {
                        ratingView(Int(rating.intValue))
                    }
                }
                Spacer(minLength: 4)
                Image(systemName: "chevron.forward")
                    .font(.footnote)
                    .foregroundStyle(CoffeePeekNativeStyle.secondary)
                    .accessibilityHidden(true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
        .accessibilityHint(strings.text("archive.open"))
    }

    private func ratingView(_ rating: Int) -> some View {
        HStack(spacing: 3) {
            ForEach(1...5, id: \.self) { index in
                Image(systemName: index <= rating ? "star.fill" : "star")
                    .font(.caption)
                    .foregroundStyle(CoffeePeekNativeStyle.accent)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: strings.text("archive.rating"), rating))
    }
}

private struct ShelfArchiveCover: View {
    let archive: IosShelfArchive
    let item: IosShelfArchiveRow
    var body: some View {
        CoffeePeekCardImage(url: item.hasPhoto ? item.id : nil, symbol: nil, fits: true, loadImage: { id, receive in
            let request = archive.loadCover(id: id, receive: receive)
            return CoffeePeekCardImageRequest { request.cancel() }
        }).clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct ShelfArchiveStrings {
    private let bundle: Bundle

    init(_ languageTag: String) {
        let url = Bundle.main.url(forResource: languageTag, withExtension: "lproj")
        bundle = url.flatMap { Bundle(url: $0) } ?? .main
    }

    func text(_ key: String) -> String { bundle.localizedString(forKey: key, value: nil, table: "NativeShelfArchive") }

    func month(_ section: IosShelfArchiveSection) -> String {
        let name = text("archive.month.\(section.month)")
        guard let year = section.earlierYear else { return name }
        return String(format: text("archive.monthYear"), name, Int(year.intValue))
    }

    func subtitle(_ item: IosShelfArchiveRow) -> String? {
        let days = item.days.flatMap { days in
            item.daysKey.map { String(format: text($0), Int(days.intValue)) }
        }
        let values = [item.roaster, days].compactMap { $0 }.filter { !$0.isEmpty }
        return values.isEmpty ? nil : values.joined(separator: " · ")
    }
}

@MainActor
private func shelfArchivePreview(settings: Bool = false, loading: Bool = false) -> some View {
    let row = IosShelfArchiveRow(id: "sample", title: "Santa Rosa", roaster: "Sample Roaster",
        days: nil, daysKey: nil, rating: nil, hasPhoto: false)
    let section = IosShelfArchiveSection(id: "sample-month", month: 10, earlierYear: nil, items: [row])
    let state = IosShelfArchiveState(signedIn: true, query: "", loading: loading, loadingMore: false,
        loadFailed: false, loadMoreFailed: false, hasMore: false, sections: loading ? [] : [section])
    return ShelfArchiveScreen(state: state, strings: ShelfArchiveStrings("ru"),
        onQuery: { _ in }, onBack: {}, onRetry: {}, onMore: {}, onBag: { _ in },
        settings: settings ? AnyView(Text(ShelfNativeStrings("ru")["shelf_settings"]).font(.headline).padding(.vertical, 16)) : nil) { _ in
        RoundedRectangle(cornerRadius: 8).fill(CoffeePeekNativeStyle.surface)
    }
}

#Preview("Archive light") { shelfArchivePreview().preferredColorScheme(.light) }
#Preview("Archive dark") { shelfArchivePreview().preferredColorScheme(.dark) }
#Preview("Settings archive loading light") {
    shelfArchivePreview(settings: true, loading: true).frame(height: 420, alignment: .top).preferredColorScheme(.light)
}
#Preview("Settings archive loading dark") {
    shelfArchivePreview(settings: true, loading: true).frame(height: 420, alignment: .top).preferredColorScheme(.dark)
}
#Preview("Settings archive content light") {
    shelfArchivePreview(settings: true).frame(height: 420, alignment: .top).preferredColorScheme(.light)
}
#Preview("Settings archive content dark") {
    shelfArchivePreview(settings: true).frame(height: 420, alignment: .top).preferredColorScheme(.dark)
}
