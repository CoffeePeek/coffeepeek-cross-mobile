import SwiftUI

/// The same guest entry card is used by native Settings and Profile.
struct CoffeePeekGuestAccountCard: View {
    let prompt: String
    let loginTitle: String
    let noAccountPrompt: String
    let registerTitle: String
    let login: () -> Void
    let register: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(prompt)
                .font(CoffeePeekNativeStyle.font(15))
                .foregroundStyle(CoffeePeekNativeStyle.foreground)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            loginButton
            registrationButton
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CoffeePeekNativeStyle.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(CoffeePeekNativeStyle.border, lineWidth: 1))
    }

    private var loginButton: some View {
        Button(action: login) {
            Text(loginTitle)
                .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold"))
                .foregroundStyle(CoffeePeekNativeStyle.color(0x1A1412))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 16).padding(.vertical, 12)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(CoffeePeekNativeStyle.accent, in: RoundedRectangle(cornerRadius: 24))
                .contentShape(RoundedRectangle(cornerRadius: 24))
        }
        .buttonStyle(.plain)
    }

    private var registrationButton: some View {
        Button(action: register) {
            (Text(noAccountPrompt)
                .font(CoffeePeekNativeStyle.font(15)).foregroundColor(CoffeePeekNativeStyle.secondary)
             + Text(" ")
             + Text(registerTitle)
                .font(CoffeePeekNativeStyle.font(17, weight: "SemiBold")).foregroundColor(CoffeePeekNativeStyle.foreground))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview("Guest account card light") {
    CoffeePeekGuestAccountCard(
        prompt: "Присоединяйтесь к сообществу, чтобы сохранять любимые места и делиться впечатлениями.",
        loginTitle: "Войти", noAccountPrompt: "Нет аккаунта?", registerTitle: "Зарегистрироваться", login: {}, register: {}
    )
    .padding(16).background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}

#Preview("Guest account card dark") {
    CoffeePeekGuestAccountCard(
        prompt: "Далучайцеся да супольнасці, каб захоўваць любімыя месцы і дзяліцца ўражаннямі.",
        loginTitle: "Увайсці", noAccountPrompt: "Няма ўліковага запісу?", registerTitle: "Зарэгістравацца", login: {}, register: {}
    )
    .padding(16).background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}
