import ComposeApp
import SwiftUI

struct ShopDetailNativeScreen: View {
    let state: ShopDetailNativeData
    let actions: ShopDetailNativeActions
    let topInset: CGFloat
    let bottomInset: CGFloat
    let owner: () -> AnyView
    let card: (ShopCheckInListRow) -> AnyView
    @State private var selectedPhoto = 0
    @State private var viewportWidth: CGFloat = 320
    @State private var visibleNames: Set<String> = []
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private func text(_ key: String) -> String { state.labels[key] ?? "" }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                if let details = state.details { content(details) } else { status }
                if state.details == nil { back.padding(.top, topInset + 12).padding(.leading, 12) }
                if let message = state.message {
                    VStack { Spacer(); ShopDetailToastView(message: message, dismissLabel: text("dismiss"), dismiss: actions.dismissMessage) }
                        .padding(.bottom, bottomInset + 80)
                }
            }
            .coordinateSpace(name: "shop-detail")
            .onAppear { viewportWidth = geometry.size.width }
            .onChange(of: geometry.size.width) { _, width in viewportWidth = width }
            .onPreferenceChange(ShopDetailExposureFrames.self) { frames in
                let viewport = CGRect(origin: .zero, size: geometry.size)
                let names = Set(frames.values.filter { item in
                    let area = item.rect.width * item.rect.height
                    let visible = item.rect.intersection(viewport)
                    return area > 0 && !visible.isNull && visible.width * visible.height / area >= 0.1
                }.map(\.feature))
                visibleNames = names
                actions.visible(names)
            }
        }
        .font(CoffeePeekNativeStyle.font(15)).foregroundStyle(CoffeePeekNativeStyle.foreground)
        .background(CoffeePeekNativeStyle.background).tint(CoffeePeekNativeStyle.accent)
        .allowsHitTesting(state.active)
        .sheet(isPresented: Binding(get: { state.priceVisible }, set: { if !$0 { actions.call("dismiss-price") } })) {
            priceInformation.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
        }
        .onAppear { actions.visible(visibleNames) }
        .onDisappear { actions.visible([]) }
    }

    private func content(_ details: IosShopDetails) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                hero(details)
                stats(details).padding(.horizontal, 16)
                if state.loading { ProgressView(text("refreshing")).frame(maxWidth: .infinity) }
                if state.failed { recovery }
                ShopDetailNativeInformation(details: details, scheduleExpanded: state.scheduleExpanded,
                    featuresExpanded: state.featuresExpanded, labels: state.labels, actions: actions)
                    .padding(.horizontal, 16)
                checkIns
                Button { actions.call("report-shop") } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(text("report")).font(CoffeePeekNativeStyle.font(16, weight: "SemiBold"))
                        Text(text("report-body")).foregroundStyle(CoffeePeekNativeStyle.secondary)
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(16)
                }.buttonStyle(.plain).padding(.horizontal, 16)
            }.padding(.bottom, bottomInset + 110)
        }
        .scrollIndicators(.hidden)
        .safeAreaInset(edge: .bottom, spacing: 0) { footer(details) }
    }

    private func hero(_ details: IosShopDetails) -> some View {
        ZStack(alignment: .bottomLeading) {
            if details.photos.isEmpty { CoffeePeekPhotoPlaceholder() }
            else {
                TabView(selection: $selectedPhoto) {
                    ForEach(Array(details.photos.enumerated()), id: \.element.id) { index, photo in
                        Button { actions.call("hero-photo", "", index) } label: {
                            CoffeePeekCardImage(url: photo.url, symbol: nil, loadImage: actions.image)
                        }.buttonStyle(.plain).accessibilityLabel(photo.label).tag(index)
                    }
                }.tabViewStyle(.page(indexDisplayMode: .never))
            }
            LinearGradient(colors: [.black.opacity(0.2), .clear, .black.opacity(0.8)], startPoint: .top, endPoint: .bottom)
                .allowsHitTesting(false)
            VStack(alignment: .leading, spacing: 8) {
                Text(details.title).font(CoffeePeekNativeStyle.font(28, weight: "Bold", relativeTo: .title)).lineLimit(3)
                    .accessibilityAddTraits(.isHeader)
                if let address = details.address, !address.isEmpty {
                    Button { actions.call("map") } label: { Label(address, systemImage: "mappin.and.ellipse") }
                        .buttonStyle(.plain).disabled(!details.canRoute).accessibilityLabel(text("map"))
                } else if details.canRoute {
                    Button(text("map")) { actions.call("map") }
                }
                HStack {
                    Text(details.kind).font(CoffeePeekNativeStyle.font(11, weight: "Bold"))
                    Spacer()
                    if details.photos.count > 1 { Text("\(min(selectedPhoto, details.photos.count - 1) + 1) / \(details.photos.count)") }
                }.font(CoffeePeekNativeStyle.font(12))
            }.foregroundStyle(.white).padding(16).padding(.top, topInset + 60)
            VStack { toolbar; Spacer() }
        }
        .frame(height: dynamicTypeSize.isAccessibilitySize ? 470 + topInset : 300 + topInset)
        .clipShape(UnevenRoundedRectangle(bottomLeadingRadius: 16, bottomTrailingRadius: 16))
        .shopDetailExposure("Photos", id: "hero", enabled: !details.photos.isEmpty)
        .onChange(of: details.photos.map(\.id)) { _, _ in selectedPhoto = min(selectedPhoto, max(0, details.photos.count - 1)) }
    }

    private var toolbar: some View {
        HStack(spacing: 8) {
            back
            Spacer(minLength: 0)
            owner().frame(width: 136, height: 48)
            Button { actions.call("favorite") } label: {
                Group {
                    if state.favoriteBusy { ProgressView() }
                    else { Image(systemName: state.favoriteSelection ? "bookmark" : state.favorite ? "heart.fill" : "heart") }
                }.font(.system(size: 21)).foregroundStyle(state.favorite ? CoffeePeekNativeStyle.accent : .white)
                    .frame(width: 48, height: 48).background(.black.opacity(0.5), in: Circle())
            }.buttonStyle(.plain).disabled(!state.favoriteEnabled || state.favoriteBusy)
                .accessibilityLabel(state.favoriteLabel).shopDetailExposure("Favorite", id: "favorite")
            actionButton("square.and.arrow.up", label: text("share"), name: "share")
                .shopDetailExposure("Share", id: "share")
        }.padding(.horizontal, 12).padding(.top, topInset + 8)
    }

    private var back: some View { actionButton("chevron.left", label: text("back"), name: "back") }

    private func actionButton(_ symbol: String, label: String, name: String) -> some View {
        Button { actions.call(name) } label: {
            Image(systemName: symbol).font(.system(size: 20)).foregroundStyle(.white)
                .frame(width: 48, height: 48).background(.black.opacity(0.5), in: Circle())
        }.buttonStyle(.plain).accessibilityLabel(label)
    }

    private func stats(_ details: IosShopDetails) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 8) { statistics(details) }
            VStack(spacing: 8) { statistics(details) }
        }
    }

    @ViewBuilder private func statistics(_ details: IosShopDetails) -> some View {
        stat(details.rating, subtitle: details.reviews, symbol: "star.fill", action: "check-ins")
        if let distance = details.distance { stat(distance, subtitle: text("distance"), symbol: "location", action: nil) }
        if let price = details.priceValue { stat(price, subtitle: text("price"), symbol: "banknote", action: "price") }
    }

    private func stat(_ value: String, subtitle: String, symbol: String, action: String?) -> some View {
        Button { if let action { actions.call(action) } } label: {
            VStack(alignment: .leading, spacing: 6) {
                Label(value, systemImage: symbol).font(CoffeePeekNativeStyle.font(18, weight: "SemiBold"))
                    .foregroundStyle(CoffeePeekNativeStyle.foreground)
                Text(subtitle).font(CoffeePeekNativeStyle.font(11)).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }.frame(maxWidth: .infinity, alignment: .leading).padding(12)
                .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                .overlay { RoundedRectangle(cornerRadius: 16).stroke(CoffeePeekNativeStyle.border, lineWidth: 1) }
        }.buttonStyle(.plain).disabled(action == nil)
    }

    private var status: some View {
        VStack(spacing: 16) {
            if state.loading { ProgressView(text("loading")) }
            else { Text(text("error")).font(CoffeePeekNativeStyle.font(20, weight: "SemiBold")); recovery }
        }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var recovery: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(text("error-body")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            CoffeePeekNativeActionButton(title: text("retry")) { actions.call("retry") }
        }.padding(.horizontal, 16)
    }

    private var checkIns: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(text("check-ins")).font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                Spacer()
                if state.checkInCount > 0 { Button(text("all-check-ins")) { actions.call("check-ins") } }
            }.padding(.horizontal, 16)
            if !state.rows.isEmpty {
                ShopCheckInListRatings(overall: state.overall, count: state.checkInCount, isSample: state.sample,
                    breakdown: state.breakdown, strings: ShopCheckInListStrings(languageTag: state.languageTag))
                    .padding(.horizontal, 16)
                checkInCarousel
            }
            if state.rows.isEmpty { Text(text("checkins-empty")).padding(.horizontal, 16) }
            if !state.loggedIn && !state.rows.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text(text("guest"))
                    Button(text("sign-in")) { actions.call("sign-in") }
                    HStack { Text(text("no-account")); Button(text("register")) { actions.call("register") } }
                }.padding(.horizontal, 16)
            }
        }
    }

    private var checkInWidth: CGFloat {
        let available = max(1, viewportWidth - 32)
        let multiple = state.rows.count + state.hiddenCheckIns > 1
        if !state.loggedIn && multiple { return min(320, max(1, available - 68)) }
        return multiple ? min(320, available) : available
    }

    private var checkInCarousel: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 12) {
                ForEach(state.rows) { row in card(row).frame(width: checkInWidth).fixedSize(horizontal: false, vertical: true) }
                ForEach(0..<state.hiddenCheckIns, id: \.self) { _ in
                    VStack { CoffeePeekPhotoPlaceholder(); Image(systemName: "lock.fill") }
                        .frame(width: 240, height: 220).clipShape(RoundedRectangle(cornerRadius: 16)).accessibilityHidden(true)
                }
            }.padding(.horizontal, 16)
        }.scrollIndicators(.hidden)
    }

    private func footer(_ details: IosShopDetails) -> some View {
        HStack(spacing: 12) {
            if details.canRoute {
                CoffeePeekNativeActionButton(title: text("route"), primary: false,
                    symbol: "arrow.triangle.turn.up.right.diamond") { actions.call("route") }
                    .shopDetailExposure("Directions", id: "directions")
            }
            CoffeePeekNativeActionButton(title: text("check-in"), symbol: "plus") { actions.call("check-in") }
                .shopDetailExposure("CheckIn", id: "check-in")
        }
        .font(CoffeePeekNativeStyle.font(16, weight: "SemiBold"))
        .frame(maxWidth: 352).padding(12).frame(maxWidth: .infinity).padding(.bottom, bottomInset)
        .background(.regularMaterial)
    }

    private var priceInformation: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(text("price")).font(CoffeePeekNativeStyle.font(22, weight: "SemiBold", relativeTo: .title2))
                Spacer()
                Button { actions.call("dismiss-price") } label: { Image(systemName: "xmark").frame(width: 44, height: 44) }
                    .accessibilityLabel(text("price-dismiss"))
            }
            ForEach(state.details?.priceTiers ?? [], id: \.count) { tier in
                HStack(spacing: 16) {
                    Text(Array(repeating: "Br", count: Int(tier.count)).joined(separator: " "))
                    Text(tier.label).frame(maxWidth: .infinity, alignment: .leading)
                    if tier.selected { Image(systemName: "checkmark").foregroundStyle(CoffeePeekNativeStyle.accent) }
                }.padding(12).background(tier.selected ? CoffeePeekNativeStyle.accent.opacity(0.15) : .clear,
                    in: RoundedRectangle(cornerRadius: 16))
                    .accessibilityValue(tier.selected ? text("price-selected") : "")
            }
            Spacer(minLength: 0)
        }.padding(20).background(CoffeePeekNativeStyle.surface).foregroundStyle(CoffeePeekNativeStyle.foreground)
    }
}

struct ShopDetailExposureFrame: Equatable { let feature: String; let rect: CGRect }
struct ShopDetailExposureFrames: PreferenceKey {
    static var defaultValue: [String: ShopDetailExposureFrame] { [:] }
    static func reduce(value: inout [String: ShopDetailExposureFrame], nextValue: () -> [String: ShopDetailExposureFrame]) {
        value.merge(nextValue(), uniquingKeysWith: { _, next in next })
    }
}
extension View {
    func shopDetailExposure(_ feature: String, id: String, enabled: Bool = true) -> some View {
        background {
            if enabled {
                GeometryReader { geometry in
                    Color.clear.preference(key: ShopDetailExposureFrames.self,
                        value: [id: ShopDetailExposureFrame(feature: feature, rect: geometry.frame(in: .named("shop-detail")))])
                }
            }
        }
    }
}


func shopDetailPreviewData() -> ShopDetailNativeData {
    var state = ShopDetailNativeData()
    state.details = IosShopDetails(id: "preview", slug: "preview", title: "CoffeePeek Specialty",
        summary: "Кофе, свежая выпечка и спокойная атмосфера.", address: "Тихая улица, 12", distance: "350 м",
        kind: "SPECIALTY", rating: "4.8", reviews: "24 чекина", isOpen: true, canRoute: true, photos: [],
        roasters: [IosShopDetailRoaster(id: "preview-roaster", name: "Местная обжарка", photo: nil, label: "Открыть обжарщика")],
        beans: ["Эфиопия", "Бразилия"], equipment: ["V60", "Аэропресс"], scheduleStatus: "Открыто до 22:00",
        schedule: [IosShopDetailSchedule(day: "Понедельник", hours: "09:00 – 22:00", today: true)],
        menu: IosShopDetailMenu(groups: [[IosShopDetailMenuItem(id: "flat-white", title: "Флэт уайт", amount: "5,50", currency: "BYN", priceLabel: "5,50 белорусских рублей")]], photos: [], freshness: ["Актуально на 18.11.2026"]),
        contacts: [IosShopDetailContact(kind: "website", title: "coffeepeek.app", target: "https://coffeepeek.app", copyable: false, label: "Открыть сайт")],
        features: ["Wi-Fi", "Велопарковка"], featureToggle: "Показать все", priceValue: "8 BYN",
        priceTiers: [IosShopDetailPriceTier(count: 2, label: "Около 8 BYN", selected: true)])
    state.labels = ["back": "Назад", "share": "Поделиться", "description": "Описание", "roasters": "Обжарщики",
        "beans": "Кофе", "equipment": "Оборудование", "hours": "Часы работы", "menu": "Меню", "contacts": "Контакты",
        "features": "Особенности", "route": "Маршрут", "check-in": "Чекин", "check-ins": "Чекины", "price": "Средний чек",
        "distance": "От вас", "checkins-empty": "Станьте первым, кто оставит чекин", "report": "Заметили ошибку?",
        "report-body": "Помогите нам сделать CoffeePeek лучше"]
    return state
}

private func shopDetailPreview() -> some View {
    ShopDetailNativeScreen(state: shopDetailPreviewData(), actions: ShopDetailNativeActions(), topInset: 44, bottomInset: 20,
        owner: { AnyView(Color.clear.frame(width: 48, height: 48)) }, card: { _ in AnyView(EmptyView()) })
}

#Preview("Shop detail light") { shopDetailPreview().preferredColorScheme(.light) }
#Preview("Shop detail dark") { shopDetailPreview().preferredColorScheme(.dark) }
