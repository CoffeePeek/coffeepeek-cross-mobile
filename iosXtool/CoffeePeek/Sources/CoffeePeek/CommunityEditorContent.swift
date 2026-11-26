import SwiftUI
import UIKit

/// All validation, catalogs and mutations are supplied by the feature's Post/Community ViewModel.
struct CommunityEditorContent: View {
    let state: CommunitySheetSnapshot
    let editor: CommunityEditorSnapshot
    let perform: (String, String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(state.title).font(CoffeePeekNativeStyle.font(24, weight: "Semibold", relativeTo: .title2))
            Text(editor.shopName).font(CoffeePeekNativeStyle.font(18, weight: "Semibold"))
            Text(editor.keptText).font(CoffeePeekNativeStyle.font(13)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            ratingSection
            noteSection
            drinkSection
            if let error = state.error { Text(error).foregroundStyle(.red).accessibilityAddTraits(.isStaticText) }
            CoffeePeekNativeActionButton(title: state.submitLabel, busy: state.busy, enabled: !state.busy) {
                perform("submit", "")
            }
            CoffeePeekNativeActionButton(title: state.cancelLabel, primary: false, enabled: !state.busy) {
                perform("dismiss", "")
            }
        }
        .disabled(state.busy)
    }

    private var ratingSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(editor.ratingsTitle).font(CoffeePeekNativeStyle.font(18, weight: "Bold"))
            Text(editor.ratingsHint).font(CoffeePeekNativeStyle.font(12)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            ForEach(editor.ratings) { rating in CommunityEditorRatingCard(rating: rating, perform: perform) }
        }
    }

    private var noteSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(editor.noteLabel).font(CoffeePeekNativeStyle.font(14, weight: "Semibold"))
            TextField(editor.notePlaceholder, text: Binding(get: { editor.text }, set: {
                perform("text", String($0.prefix(editor.limit)))
            }), axis: .vertical)
            .lineLimit(3...8)
            .padding(14)
            .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(state.error == nil ? CoffeePeekNativeStyle.border : .red))
            .accessibilityLabel(editor.noteLabel)
        }
    }

    private var drinkSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Picker(editor.drink.label, selection: Binding(get: { editor.drink.selected }, set: { perform("drink", $0) })) {
                ForEach(editor.drink.options) { option in Text(option.label).tag(option.key) }
            }
            .pickerStyle(.menu)
            .tint(CoffeePeekNativeStyle.foreground)
            .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
            if let failure = editor.drink.catalogError {
                Text(failure).foregroundStyle(.red)
                Button(editor.drink.retryLabel) { perform("retry", "") }.frame(minHeight: 44)
            }
            if let custom = editor.drink.custom {
                TextField(editor.drink.customLabel, text: Binding(get: { custom }, set: {
                    perform("customDrink", String($0.prefix(editor.drink.customLimit)))
                }))
                .padding(14)
                .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: Capsule())
                .overlay(Capsule().stroke(CoffeePeekNativeStyle.border))
                .accessibilityLabel(editor.drink.customLabel)
            }
        }
    }
}

private struct CommunityEditorRatingCard: View {
    let rating: CommunityRatingSnapshot
    let perform: (String, String) -> Void
    @ScaledMetric(relativeTo: .body) private var starSize = 24.0
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 12) {
            if let url = URL(string: rating.imageUri), url.isFileURL, let image = UIImage(contentsOfFile: url.path) {
                Image(uiImage: image).resizable().scaledToFill().frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 8)).accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(rating.label).font(CoffeePeekNativeStyle.font(14, weight: "Semibold"))
                stars
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(CoffeePeekNativeStyle.border))
    }

    private var stars: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            starButtons
        }
    }

    private var starButtons: some View {
        HStack(spacing: 2) {
            ForEach(1...5, id: \.self) { value in
                Button { perform(rating.key, String(value)) } label: {
                    Image(systemName: value <= rating.value ? "star.fill" : "star")
                        .foregroundStyle(value <= rating.value ? CoffeePeekNativeStyle.accent : inactiveStarColor)
                        .frame(minWidth: 44, minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(rating.descriptions.indices.contains(value - 1) ? rating.descriptions[value - 1] : rating.label)
                .accessibilityAddTraits(value == rating.value ? .isSelected : [])
            }
        }
        .font(.system(size: min(starSize, 32)))
    }

    private var inactiveStarColor: Color {
        colorScheme == .light ? CoffeePeekNativeStyle.foreground : CoffeePeekNativeStyle.secondary
    }
}

@MainActor
private func communityEditorPreview() -> some View {
    let editor = CommunityEditorSnapshot(shopName: "CoffeePeek", keptText: "Фотографии и дата сохранятся",
        ratingsTitle: "Оценки", ratingsHint: "Выберите оценку", ratings: [
            CommunityRatingSnapshot(key: "coffee", label: "Кофе", value: 4, imageUri: "", descriptions: []),
            CommunityRatingSnapshot(key: "service", label: "Сервис", value: 5, imageUri: "", descriptions: []),
            CommunityRatingSnapshot(key: "atmosphere", label: "Атмосфера", value: 4, imageUri: "", descriptions: [])
        ], noteLabel: "Заметка", notePlaceholder: "Впечатления", text: "Очень яркая чашка", limit: 1000,
        drink: CommunityDrinkSnapshot(label: "Напиток", selected: "drink:other", custom: "Фильтр",
            customLabel: "Название", customLimit: 100, catalogError: "Каталог недоступен",
            retryLabel: "Повторить", options: [CommunityDrinkOption(key: "drink:other", label: "Другой")]))
    return ScrollView {
        CommunityEditorContent(state: CommunitySheetSnapshot(active: true, title: "Редактировать чекин",
            submitLabel: "Сохранить", cancelLabel: "Отмена"), editor: editor, perform: { _, _ in }).padding(16)
    }
    .foregroundStyle(CoffeePeekNativeStyle.foreground)
    .background(CoffeePeekNativeStyle.surface)
}

#Preview("Community editor light") { communityEditorPreview().preferredColorScheme(.light) }
#Preview("Community editor dark") { communityEditorPreview().preferredColorScheme(.dark) }
