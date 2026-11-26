import SwiftUI

struct CommunityReportContent: View {
    let state: CommunitySheetSnapshot
    let report: CommunityReportSnapshot
    let perform: (String, String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(state.title).font(CoffeePeekNativeStyle.font(22, weight: "Semibold", relativeTo: .title2))
            TextField(report.placeholder, text: Binding(get: { report.text }, set: {
                perform("text", String($0.prefix(report.limit)))
            }), axis: .vertical)
            .lineLimit(4...10)
            .padding(14)
            .background(CoffeePeekNativeStyle.foreground.opacity(0.04), in: RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(state.error == nil ? CoffeePeekNativeStyle.border : .red))
            .accessibilityLabel(report.placeholder)
            if let error = state.error { Text(error).foregroundStyle(.red) }
            CoffeePeekNativeActionButton(title: state.submitLabel, busy: state.busy, enabled: !state.busy) {
                perform("submit", "")
            }
            CoffeePeekNativeActionButton(title: state.cancelLabel, primary: false, enabled: !state.busy) {
                perform("dismiss", "")
            }
        }
        .disabled(state.busy)
    }
}

@MainActor
private func communityReportPreview(busy: Bool = false) -> some View {
    CommunityReportContent(state: CommunitySheetSnapshot(active: true, title: "Пожаловаться", busy: busy,
        submitLabel: busy ? "Отправка…" : "Отправить", cancelLabel: "Отмена", error: busy ? nil : "Проверьте текст жалобы"),
        report: CommunityReportSnapshot(text: "", placeholder: "Причина", limit: 2000), perform: { _, _ in })
    .padding(16).foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.surface)
}

#Preview("Community report light") { communityReportPreview().preferredColorScheme(.light) }
#Preview("Community report dark") { communityReportPreview().preferredColorScheme(.dark) }
#Preview("Community report sending") { communityReportPreview(busy: true).preferredColorScheme(.dark) }
