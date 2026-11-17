import SwiftUI

struct CheckInDetailNativePresentation {
    var active = true
    var loadingLabel: String?
    var errorText: String?
    var retryLabel: String?
    var notice: String?
    var dismissAllowed = true
    var card: CoffeePeekCheckInCardData?
    var confirmation: CheckInDetailNativeConfirmation?
}

struct CheckInDetailNativeConfirmation {
    let title: String
    let body: String
    let confirmLabel: String
    let cancelLabel: String
}

/// Root UIKit owns the navigation bar and back policy. Both check-in kinds share this body.
struct CheckInDetailNativeContent: View {
    let state: CheckInDetailNativePresentation
    let loadImage: CoffeePeekCardImageLoader
    let perform: (String) -> Void
    let openPhoto: (Int) -> Void

    var body: some View {
        Group {
            if state.active {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        status.padding(.horizontal, 16)
                        if let notice = state.notice, !notice.isEmpty { noticeView(notice) }
                        if let card = state.card {
                            CoffeePeekCheckInCard(data: card, loadImage: loadImage, perform: perform, openPhoto: openPhoto)
                        }
                    }
                    .padding(.top, 16).padding(.bottom, 32)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else { Color.clear }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .font(CoffeePeekNativeStyle.font(16)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background.ignoresSafeArea()).tint(CoffeePeekNativeStyle.accent)
        .interactiveDismissDisabled(!state.dismissAllowed)
        .alert(state.confirmation?.title ?? "", isPresented: confirmationPresented) {
            if let confirmation = state.confirmation {
                Button(confirmation.confirmLabel, role: .destructive) { perform("confirmDelete") }
                Button(confirmation.cancelLabel, role: .cancel) { perform("cancelDelete") }
            }
        } message: { Text(state.confirmation?.body ?? "") }
    }

    @ViewBuilder private var status: some View {
        if state.loadingLabel != nil || state.errorText != nil {
            VStack(alignment: .leading, spacing: 12) {
                if let loading = state.loadingLabel {
                    ProgressView(loading).tint(CoffeePeekNativeStyle.foreground).frame(maxWidth: .infinity)
                }
                if let message = state.errorText {
                    Text(message).foregroundStyle(CoffeePeekNativeStyle.secondary).fixedSize(horizontal: false, vertical: true)
                }
                if let retry = state.retryLabel {
                    Button(retry) { perform("retry") }.frame(minHeight: 44)
                }
            }.accessibilityElement(children: .contain)
        }
    }

    private func noticeView(_ notice: String) -> some View {
        Text(notice).font(CoffeePeekNativeStyle.font(15)).fixedSize(horizontal: false, vertical: true)
            .padding(14).frame(maxWidth: .infinity, alignment: .leading)
            .background(CoffeePeekNativeStyle.accent.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 16)
    }

    private var confirmationPresented: Binding<Bool> {
        Binding(get: { state.active && state.confirmation != nil }, set: { _ in })
    }
}

private func checkInDetailPreview(home: Bool) -> CheckInDetailNativePresentation {
    var card = CoffeePeekCheckInCardData(id: "preview", name: "CoffeePeek", avatar: nil, date: "17 ноября · 10:30")
    card.home = home
    card.title = home ? "" : "CoffeePeek"
    card.bean = home ? "Эфиопия" : ""
    card.recipe = home ? "V60 · 18 г · 3 мин" : ""
    card.note = "Ягодные ноты и сладкое послевкусие."
    card.rating = 4
    card.ratingLabel = "4 из 5"
    card.drink = "Фильтр"
    card.menu = [.init(id: "edit", label: "Редактировать"), .init(id: "delete", label: "Удалить", destructive: true)]
    card.actions = ["edit", "delete"]
    return CheckInDetailNativePresentation(card: card)
}

#Preview("Home detail light") {
    CheckInDetailNativeContent(state: checkInDetailPreview(home: true), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.light)
}
#Preview("Home detail dark") {
    CheckInDetailNativeContent(state: checkInDetailPreview(home: true), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.dark)
}
#Preview("Shop detail light") {
    CheckInDetailNativeContent(state: checkInDetailPreview(home: false), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.light)
}
#Preview("Shop detail dark") {
    CheckInDetailNativeContent(state: checkInDetailPreview(home: false), loadImage: { _, _ in nil }, perform: { _ in }, openPhoto: { _ in })
        .preferredColorScheme(.dark)
}
