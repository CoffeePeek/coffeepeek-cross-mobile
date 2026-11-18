import ComposeApp
import SwiftUI

struct HomeCheckInNativeBeanPicker: View {
    let state: IosHomeCheckInState
    let strings: CheckInNativeStrings
    let actions: HomeCheckInNativeActions
    @State private var customName = ""
    @State private var footerHeight: CGFloat = 180
    @FocusState private var searching: Bool
    private var bags: [IosHomeCheckInOption] {
        let query = state.catalog.beanQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return state.catalog.shelfBags }
        return state.catalog.shelfBags.filter {
            [$0.name, $0.detail].compactMap { $0 }.contains { $0.localizedCaseInsensitiveContains(query) }
        }
    }
    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 16) {
                header
                search
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 8) { results }
                }
                .scrollDismissesKeyboard(.interactively)
                .frame(maxHeight: .infinity)
                customBeanFooter(availableHeight: geometry.size.height)
            }
        }
        .padding(.horizontal, 16).disabled(!state.status.canEdit)
            .onAppear { customName = state.draft.customBeanName ?? "" }
    }

    private func customBeanFooter(availableHeight: CGFloat) -> some View {
        // Keep the action reachable above the keyboard, even with large text on a small phone.
        ScrollView {
            customBean.background {
                GeometryReader { geometry in
                    Color.clear.preference(key: HomeCheckInBeanFooterHeight.self, value: geometry.size.height)
                }
            }
        }
        .frame(height: min(footerHeight, max(44, availableHeight - 176)))
        .scrollDismissesKeyboard(.interactively)
        .onPreferenceChange(HomeCheckInBeanFooterHeight.self) { footerHeight = $0 }
    }
    private var header: some View {
        HStack {
            Button(action: actions.closeBeans) { Image(systemName: "chevron.left").frame(width: 48, height: 48) }
                .accessibilityLabel(strings["home_check_in_back_to_editor"])
            Text(strings["home_check_in_bean"]).font(CoffeePeekNativeStyle.font(22, weight: "Bold", relativeTo: .title2))
                .accessibilityAddTraits(.isHeader)
            Spacer()
        }.tint(CoffeePeekNativeStyle.foreground)
    }
    private var search: some View {
        HStack {
            Image(systemName: "magnifyingglass").accessibilityHidden(true)
            TextField(strings["home_check_in_bean_search"], text: Binding(get: { state.catalog.beanQuery }, set: actions.query))
                .submitLabel(.search).onSubmit(actions.search).focused($searching)
            if !state.catalog.beanQuery.isEmpty {
                Button { actions.query("") } label: { Image(systemName: "xmark.circle.fill").frame(width: 44, height: 44) }
                    .accessibilityLabel(strings["home_check_in_clear_search"])
            }
        }.padding(.horizontal, 16).frame(minHeight: 48)
            .background(CoffeePeekNativeStyle.surface, in: Capsule())
            .overlay(Capsule().stroke(CoffeePeekNativeStyle.border))
    }
    @ViewBuilder private var results: some View {
        if !bags.isEmpty {
            section("home_check_in_from_shelf")
            ForEach(bags, id: \.id) { bag in row(bag, shelf: true) }
        }
        if !state.catalog.beans.isEmpty {
            section("home_check_in_from_catalog")
            ForEach(state.catalog.beans, id: \.id) { bean in row(bean, shelf: false) }
        }
        if state.catalog.beansLoading { ProgressView(strings["home_check_in_beans_loading"]).padding() }
        else if let key = state.catalog.beansIssueKey {
            Text(strings[key]).foregroundStyle(.red)
            Button(strings["home_check_in_retry"], action: actions.search).frame(minHeight: 44)
        } else if bags.isEmpty && state.catalog.beans.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "magnifyingglass").font(.system(size: 40)).accessibilityHidden(true)
                Text(strings["home_check_in_beans_empty"]).font(CoffeePeekNativeStyle.font(17, weight: "Semibold"))
                Text(strings["home_check_in_beans_empty_hint"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }.frame(maxWidth: .infinity).multilineTextAlignment(.center).padding(.vertical, 24)
        }
    }
    private func section(_ key: String) -> some View {
        Text(strings[key]).font(CoffeePeekNativeStyle.font(15, weight: "Semibold"))
            .foregroundStyle(CoffeePeekNativeStyle.secondary).padding(.top, 8).accessibilityAddTraits(.isHeader)
    }
    private func row(_ option: IosHomeCheckInOption, shelf: Bool) -> some View {
        VStack(spacing: 0) {
            Button {
                searching = false
                if shelf { actions.shelfBag(option.id) } else { actions.bean(option.id) }
            } label: { rowLabel(option, shelf: shelf) }
                .buttonStyle(.plain)
            Divider().overlay(CoffeePeekNativeStyle.border)
        }
    }
    private func rowLabel(_ option: IosHomeCheckInOption, shelf: Bool) -> some View {
        HStack(spacing: 12) {
            if shelf {
                HomeCheckInNativeRemoteImage(url: option.imageUrl, load: actions.loadImage)
                    .frame(width: 44, height: 56).clipShape(RoundedRectangle(cornerRadius: 4))
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(option.name ?? strings["home_check_in_shelf_bag_unnamed"]).font(CoffeePeekNativeStyle.font(17))
                if let detail = option.detail { Text(detail).font(CoffeePeekNativeStyle.font(13)).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            }.frame(maxWidth: .infinity, alignment: .leading)
        }.padding(.horizontal, 8).padding(.vertical, shelf ? 8 : 12).contentShape(Rectangle())
    }
    private var customBean: some View {
        VStack(spacing: 12) {
            CheckInNativeField(title: strings["home_check_in_custom_bean"], style: .home) {
                CheckInNativeInput(title: strings["home_check_in_custom_bean_name"], placeholder: strings["home_check_in_custom_bean_hint"],
                    value: Binding(get: { customName }, set: { customName = String($0.prefix(250)) }), style: .home)
            }
            CheckInNativePrimaryButton(title: strings["home_check_in_use_custom_bean"],
                enabled: !customName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) {
                searching = false
                actions.customBean(customName.trimmingCharacters(in: .whitespacesAndNewlines))
            }
        }.padding(.vertical, 16)
    }
}

private struct HomeCheckInBeanFooterHeight: PreferenceKey {
    static let defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

#Preview("Home bean picker light") {
    HomeCheckInNativeBeanPicker(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("ru"), actions: .init())
        .background(CoffeePeekNativeStyle.background).preferredColorScheme(.light)
}
#Preview("Home bean picker dark") {
    HomeCheckInNativeBeanPicker(state: homeCheckInNativePreviewState(), strings: homeCheckInNativePreviewStrings("be"), actions: .init())
        .background(CoffeePeekNativeStyle.background).preferredColorScheme(.dark)
}
