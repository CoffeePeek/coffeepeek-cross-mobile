import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeShelfArchiveController(archive: IosShelfArchive, languageTag: String) -> UIViewController {
    UIHostingController(rootView: ShelfArchiveHost(archive: archive, strings: ShelfArchiveStrings(languageTag)))
}

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

    deinit {
        observation?.cancel()
        archive.close()
    }
}

private struct ShelfArchiveHost: View {
    @StateObject private var model: ShelfArchiveModel
    let strings: ShelfArchiveStrings

    init(archive: IosShelfArchive, strings: ShelfArchiveStrings) {
        _model = StateObject(wrappedValue: ShelfArchiveModel(archive: archive))
        self.strings = strings
    }

    var body: some View {
        ShelfArchiveScreen(
            state: model.state, strings: strings,
            onQuery: { model.archive.query(value: $0) }, onBack: model.archive.back,
            onRetry: model.archive.retry, onMore: model.archive.more, onBag: { model.archive.openBag(id: $0) }
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
    @ViewBuilder let cover: (IosShelfArchiveRow) -> Cover

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 8) {
                if state.signedIn {
                    archiveContent
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .background(CoffeePeekNativeStyle.background)
        .foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Button(action: onBack) {
                    Label(strings.text("archive.back"), systemImage: "chevron.backward")
                        .labelStyle(.iconOnly)
                        .frame(minWidth: 44, minHeight: 44)
                }
                Text(strings.text("archive.title"))
                    .font(CoffeePeekNativeStyle.font(18, weight: "SemiBold", relativeTo: .headline))
                    .accessibilityAddTraits(.isHeader)
            }
            if state.signedIn { search }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.background)
    }

    private var search: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").accessibilityHidden(true)
            TextField(strings.text("archive.search"), text: Binding(get: { state.query }, set: onQuery))
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
            ProgressView().frame(maxWidth: .infinity).accessibilityLabel(strings.text("archive.loading"))
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
        ProgressView(strings.text("archive.loading"))
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
    @State private var image: UIImage?
    @State private var load: IosShelfArchiveObservation?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFit()
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(CoffeePeekNativeStyle.surface)
                    .overlay(Image(systemName: "bag").foregroundStyle(CoffeePeekNativeStyle.secondary))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .onAppear {
            if item.hasPhoto && load == nil {
                load = archive.loadCover(id: item.id) { image = $0 }
            }
        }
        .onDisappear {
            load?.cancel()
            load = nil
            image = nil
        }
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

private func shelfArchivePreview() -> some View {
    let row = IosShelfArchiveRow(id: "sample", title: "Santa Rosa", roaster: "Sample Roaster",
        days: nil, daysKey: nil, rating: nil, hasPhoto: false)
    let section = IosShelfArchiveSection(id: "sample-month", month: 10, earlierYear: nil, items: [row])
    let state = IosShelfArchiveState(signedIn: true, query: "", loading: false, loadingMore: false,
        loadFailed: false, loadMoreFailed: false, hasMore: false, sections: [section])
    return ShelfArchiveScreen(state: state, strings: ShelfArchiveStrings("ru"),
        onQuery: { _ in }, onBack: {}, onRetry: {}, onMore: {}, onBag: { _ in }) { _ in
        RoundedRectangle(cornerRadius: 8).fill(CoffeePeekNativeStyle.surface)
    }
}

#Preview("Archive light") { shelfArchivePreview().preferredColorScheme(.light) }
#Preview("Archive dark") { shelfArchivePreview().preferredColorScheme(.dark) }
