import ComposeApp
import SwiftUI

struct DiscoveryMapControls: View {
    let state: IosDiscoveryState
    let actions: DiscoveryNativeActions
    let header: AnyView

    private func text(_ key: String) -> String { state.labels[key] ?? "" }

    var body: some View {
        VStack(spacing: 12) {
            header
            if state.truncated { notice(text("truncated")) }
            if state.viewportFailed && state.mapShops.isEmpty {
                VStack(spacing: 6) {
                    Text(text("map-failed"))
                    Button(text("retry")) { actions.call("retry-map") }
                }.padding(12).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
            }
            if state.showSuggestions { suggestions }
            Spacer(minLength: 8)
            HStack {
                Spacer()
                VStack(spacing: 8) {
                    control("plus", label: text("zoom-in"), action: "zoom-in")
                    control("minus", label: text("zoom-out"), action: "zoom-out")
                    control("flame", label: text(state.heatmapVisible ? "heatmap-hide" : "heatmap"), action: "heatmap", selected: state.heatmapVisible)
                        .overlay { if state.heatmapLoading { ProgressView().controlSize(.mini) } }
                    control("square.3.layers.3d", label: text(state.zonesVisible ? "zones-hide" : "zones-show"),
                        action: "zones", selected: state.zonesVisible)
                    control("location", label: text("location"), action: "location")
                }
            }.padding(.horizontal, 16)
            selection
        }
        .padding(.bottom, CGFloat(state.bottomClearance) + 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var suggestions: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if state.searchLoading { ProgressView(text("loading")).padding(12) }
                if state.searchFailed { Text(text("map-search-failed")).padding(12) }
                if !state.searchLoading && !state.searchFailed && state.suggestions.isEmpty {
                    Text(text("map-search-empty")).padding(12)
                }
                ForEach(state.suggestions, id: \.id) { shop in
                    Button { actions.call("suggestion", shop.id) } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(shop.title).font(CoffeePeekNativeStyle.font(15, weight: "SemiBold"))
                            if let address = shop.address { Text(address).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                        }.frame(maxWidth: .infinity, alignment: .leading).padding(12)
                    }.buttonStyle(.plain)
                }
            }
        }
        .frame(maxHeight: 220).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal, 16)
    }

    @ViewBuilder private var selection: some View {
        if let selectedZone = state.selectedZone {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(selectedZone.title).font(CoffeePeekNativeStyle.font(18, weight: "SemiBold"))
                    Spacer()
                    control("xmark", label: text("zone-close"), action: "zone-close")
                }
                if let description = selectedZone.summary, !description.isEmpty { Text(description) }
                Button { actions.call("zone-shops") } label: {
                    Text(text("zone-shops") + " (\(selectedZone.count))")
                }
            }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24)).padding(.horizontal, 16)
        } else if !state.mapShops.isEmpty {
            DiscoveryMapCarousel(shops: state.mapShops, selectedId: state.selectedShopId,
                labels: state.labels, actions: actions).id(state.mapShops.map(\.id))
        }
    }

    private func notice(_ message: String) -> some View {
        Text(message).font(CoffeePeekNativeStyle.font(12)).padding(10)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12)).padding(.horizontal, 16)
    }

    private func control(_ symbol: String, label: String, action: String, selected: Bool = false) -> some View {
        Button { actions.call(action) } label: {
            Image(systemName: symbol).font(.system(size: 20))
                .foregroundStyle(selected ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.foreground)
                .frame(width: 48, height: 48).background(.regularMaterial, in: Circle())
        }.buttonStyle(.plain).accessibilityLabel(label).accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct DiscoveryMapCarousel: View {
    let shops: [IosDiscoveryShop]
    let selectedId: String?
    let labels: [String: String]
    let actions: DiscoveryNativeActions
    @State private var page = 0
    @State private var initialized = false
    @State private var recentering = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        TabView(selection: $page) {
            ForEach(0..<(shops.count > 1 ? shops.count * 3 : 1), id: \.self) { index in
                let shop = shops[index % shops.count]
                DiscoveryShopCard(shop: shop, picker: false, compact: true, labels: labels, actions: actions,
                    action: shop.id == selectedId ? "map-open" : "map-select")
                    .padding(.horizontal, 20).tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never)).frame(height: dynamicTypeSize.isAccessibilitySize ? 240 : 126)
        .onAppear {
            page = (shops.count > 1 ? shops.count : 0) + (shops.firstIndex { $0.id == selectedId } ?? 0)
            DispatchQueue.main.async { initialized = true }
        }
        .onChange(of: page) { _, newPage in
            guard initialized else { return }
            if recentering { recentering = false; return }
            let shop = shops[newPage % shops.count]
            if shop.id != selectedId { actions.call("carousel", shop.id) }
            if shops.count > 1 && (newPage < shops.count || newPage >= shops.count * 2) {
                DispatchQueue.main.async {
                    recentering = true
                    page = shops.count + newPage % shops.count
                }
            }
        }
        .onChange(of: selectedId) { _, id in
            guard let index = shops.firstIndex(where: { $0.id == id }), page % shops.count != index else { return }
            withAnimation { page = (shops.count > 1 ? shops.count : 0) + index }
        }
    }
}


#Preview("Discovery map light") {
    DiscoveryMapControls(state: discoveryPreviewState(expanded: true), actions: DiscoveryNativeActions(),
        header: AnyView(Color.clear.frame(height: 100))).background(CoffeePeekNativeStyle.border).preferredColorScheme(.light)
}

#Preview("Discovery map dark") {
    DiscoveryMapControls(state: discoveryPreviewState(expanded: true), actions: DiscoveryNativeActions(),
        header: AnyView(Color.clear.frame(height: 100))).background(CoffeePeekNativeStyle.border).preferredColorScheme(.dark)
}
