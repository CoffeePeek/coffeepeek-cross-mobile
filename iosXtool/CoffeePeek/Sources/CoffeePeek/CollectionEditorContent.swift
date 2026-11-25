import SwiftUI

struct CollectionEditorContent<Cover: View>: View {
    let title: String
    let description: String
    let isPublic: Bool
    let enabled: Bool
    let strings: CollectionsWorkspaceStrings
    let onTitle: (String) -> Void
    let onDescription: (String) -> Void
    let onVisibility: (Bool) -> Void
    @ViewBuilder let cover: () -> Cover

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(alignment: .top, spacing: 16) {
                cover()
                TextField(strings["collections_name"], text: Binding(get: { title }, set: { onTitle($0) }))
                    .textFieldStyle(.roundedBorder).disabled(!enabled)
            }
            Text(strings["collections_description"]).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            TextField(strings["collections_description_placeholder"],
                text: Binding(get: { description }, set: { onDescription($0) }), axis: .vertical)
                .lineLimit(3...6).textFieldStyle(.roundedBorder).disabled(!enabled)
            Toggle(strings["collections_public_switch"], isOn: Binding(get: { isPublic }, set: { onVisibility($0) }))
                .tint(CoffeePeekNativeStyle.accent).disabled(!enabled)
            Text(strings[isPublic ? "collections_public_hint" : "collections_private_hint"])
                .font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }
}

struct CollectionProfilePreferenceContent: View {
    let visible: Bool
    let selectedTitle: String?
    let enabled: Bool
    let strings: CollectionsWorkspaceStrings
    let onVisibility: (Bool) -> Void
    let onChoose: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(strings["collections_settings"]).font(.title2).accessibilityAddTraits(.isHeader)
            Toggle(strings["collections_profile_show"], isOn: Binding(get: { visible }, set: { onVisibility($0) }))
                .tint(CoffeePeekNativeStyle.accent).disabled(!enabled)
            Text(strings["collections_profile_choice_hint"]).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if visible {
                Button(action: onChoose) {
                    HStack {
                        Text(selectedTitle ?? strings["collections_profile_choose"])
                        Spacer()
                        Image(systemName: "chevron.forward")
                    }
                }.disabled(!enabled)
            }
            Button(strings["collections_done"], action: onDone).frame(maxWidth: .infinity, minHeight: 44)
        }
    }
}

struct CollectionSummaryCard<Cover: View>: View {
    let title: String
    let count: Int32
    let strings: CollectionsWorkspaceStrings
    @ViewBuilder let cover: () -> Cover

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            cover()
            LinearGradient(stops: [.init(color: .clear, location: 0.3),
                .init(color: .black.opacity(0.85), location: 1)], startPoint: .top, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline).lineLimit(2)
                Text(strings.count(count)).font(.caption)
            }.foregroundStyle(.white).padding(12)
        }.aspectRatio(1, contentMode: .fit).clipped().clipShape(RoundedRectangle(cornerRadius: 16))
            .accessibilityElement(children: .combine)
    }
}

@MainActor
private func collectionContentSample() -> some View {
    ScrollView {
        VStack(alignment: .leading, spacing: 24) {
            CollectionEditorContent(title: "Кофейные выходные", description: "Любимые места", isPublic: true,
                enabled: true, strings: CollectionsWorkspaceStrings(languageTag: "ru"),
                onTitle: { _ in }, onDescription: { _ in }, onVisibility: { _ in }) {
                    CoffeePeekPhotoPlaceholder().frame(width: 108, height: 108).clipShape(RoundedRectangle(cornerRadius: 16))
                }
            CollectionProfilePreferenceContent(visible: true, selectedTitle: "Кофейные выходные", enabled: true,
                strings: CollectionsWorkspaceStrings(languageTag: "ru"), onVisibility: { _ in }, onChoose: {}, onDone: {})
            CollectionSummaryCard(title: "Кофейные выходные", count: 7,
                strings: CollectionsWorkspaceStrings(languageTag: "ru")) { CoffeePeekPhotoPlaceholder() }
        }.padding(16)
    }.font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background)
}

#Preview("Collection workspace light") {
    collectionContentSample().preferredColorScheme(.light)
}
#Preview("Collection workspace dark") {
    collectionContentSample().preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility2)
}

