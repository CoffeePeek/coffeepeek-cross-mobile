import ComposeApp
import CoreLocation
import SwiftUI
import UIKit

struct ContributionMapPicker: View {
    @ObservedObject var model: ContributionFormModel
    var body: some View {
        ContributionMapContent(state: model.state.map, strings: model.strings,
            onCancel: { model.form.control(action: "dismissMap") },
            onLocate: { model.requestLocation(map: true) },
            onConfirm: { model.form.control(action: "confirmMap") }) {
                ContributionMapCanvas(source: model.form.map, state: model.state.map)
            }
    }
}

struct ContributionMapContent<Map: View>: View {
    let state: ContributionMapSnapshot?
    let strings: ContributionsStrings
    let onCancel: () -> Void
    let onLocate: () -> Void
    let onConfirm: () -> Void
    @ViewBuilder let map: () -> Map

    var body: some View {
        ZStack {
            map().overlay {
                Image(systemName: "mappin.circle.fill").font(.system(size: 40, weight: .semibold))
                    .foregroundStyle(CoffeePeekNativeStyle.accent)
                    .shadow(color: .black.opacity(0.5), radius: 3, y: 1)
                    .offset(y: -20)
                    .allowsHitTesting(false)
                    .accessibilityLabel(strings.text("map_pin"))
            }.ignoresSafeArea()
            VStack {
                HStack {
                    mapControl("xmark", label: strings.text("map_cancel"), action: onCancel)
                    Spacer()
                }
                Spacer()
                HStack {
                    Spacer()
                    mapControl("location.fill", label: strings.text("map_my_location"), action: onLocate)
                }.padding(.bottom, 8)
                VStack(alignment: .leading, spacing: 12) {
                    Text(strings.text("map_hint")).font(CoffeePeekNativeStyle.font(16, weight: "SemiBold", relativeTo: .headline))
                    if let state {
                        if state.resolving { ProgressView(strings.text("map_resolving")) }
                        else if state.failed { Text(strings.text("map_failed")).foregroundStyle(.red) }
                        else { Text(state.address ?? strings.text("map_move")).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                        CoffeePeekNativeActionButton(title: strings.text("map_choose"), enabled: state.canConfirm, action: onConfirm)
                    }
                }.padding(16).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 24))
                    .overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
                    .padding(.bottom, 12)
            }.padding(16)
        }.font(CoffeePeekNativeStyle.font(15))
            .tint(CoffeePeekNativeStyle.foreground).foregroundStyle(CoffeePeekNativeStyle.foreground)
    }

    private func mapControl(_ symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 18, weight: .semibold))
                .frame(width: 48, height: 48)
        }
        .buttonStyle(.plain)
        .shopDetailGlassCircle()
        .accessibilityLabel(label)
    }
}

private struct ContributionMapCanvas: UIViewRepresentable {
    let source: IosContributionMap
    let state: ContributionMapSnapshot?
    @Environment(\.colorScheme) private var colorScheme
    func makeUIView(context: Context) -> UIView { source.createView() }
    func updateUIView(_ view: UIView, context: Context) { source.update(view: view, dark: colorScheme == .dark) }
}

@MainActor
final class ContributionLocationPermission: NSObject, @MainActor CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var completion: ((Bool) -> Void)?

    func request(completion: @escaping (Bool) -> Void) {
        self.completion = completion
        manager.delegate = self
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse: finish(true)
        case .denied, .restricted: finish(false)
        case .notDetermined: manager.requestWhenInUseAuthorization()
        @unknown default: finish(false)
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse: finish(true)
        case .denied, .restricted: finish(false)
        default: break
        }
    }

    private func finish(_ granted: Bool) {
        let completion = completion
        self.completion = nil
        completion?(granted)
    }
}



@MainActor
private func contributionMapSample() -> some View {
    ContributionMapContent(state: ContributionMapSnapshot(latitude: 53.9, longitude: 27.5,
        cameraLatitude: nil, cameraLongitude: nil, address: "Sandbox street, 1",
        resolving: false, failed: false, canConfirm: true), strings: ContributionsStrings(languageTag: "ru"),
        onCancel: {}, onLocate: {}, onConfirm: {}) {
            LinearGradient(colors: [.gray.opacity(0.2), .green.opacity(0.1)], startPoint: .topLeading, endPoint: .bottomTrailing)
        }
}
#Preview("Contribution map light") { contributionMapSample().preferredColorScheme(.light) }
#Preview("Contribution map dark") { contributionMapSample().preferredColorScheme(.dark) }
