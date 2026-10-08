import ComposeApp
import Foundation
import MapLibre
import UIKit

/// Native iOS map implementation. Compose remains responsible for state and
/// navigation; this class only renders the same OpenFreeMap style used by
/// Android and reports map events back through the Kotlin bridge.
final class MapLibreMapProvider: NSObject, IosNativeMapProvider, MLNMapViewDelegate {
    private static let lightStyle = URL(string: "https://tiles.openfreemap.org/styles/positron")!
    private static let darkStyle = URL(string: "https://tiles.openfreemap.org/styles/dark")!

    private var callbacksByMap: [ObjectIdentifier: IosNativeMapCallbacks] = [:]
    private var annotationKinds: [ObjectIdentifier: String] = [:]
    private var annotationIds: [ObjectIdentifier: String] = [:]
    private var annotationZoneColors: [ObjectIdentifier: (color: String, dark: Bool)] = [:]
    private var annotationImages: [String: UIImage] = [:]

    func createMapView() -> UIView {
        let map = MLNMapView(frame: .zero, styleURL: Self.lightStyle)
        map.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        map.delegate = self
        map.showsLogoView = false
        map.showsAttributionButton = true
        map.setCenter(
            CLLocationCoordinate2D(latitude: 53.9045, longitude: 27.5615),
            zoomLevel: 12,
            animated: false
        )
        return map
    }

    func updateMapView(
        mapView: UIView,
        stateJson: String,
        callbacks: IosNativeMapCallbacks
    ) {
        guard let map = mapView as? MLNMapView,
              let data = stateJson.data(using: .utf8),
              let state = try? JSONDecoder().decode(MapState.self, from: data) else {
            return
        }

        callbacksByMap[ObjectIdentifier(map)] = callbacks
        map.showsAttributionButton = state.showAttribution ?? true
        let desiredStyle = state.dark ? Self.darkStyle : Self.lightStyle
        if map.styleURL != desiredStyle {
            map.styleURL = desiredStyle
        }

        let oldAnnotations = map.annotations ?? []
        if !oldAnnotations.isEmpty {
            map.removeAnnotations(oldAnnotations)
        }
        annotationKinds.removeAll()
        annotationIds.removeAll()
        annotationZoneColors.removeAll()

        var annotations: [MLNPointAnnotation] = []
        for shop in state.shops {
            let annotation = CoffeeAnnotation(
                id: shop.id,
                kind: "shop",
                coordinate: CLLocationCoordinate2D(latitude: shop.lat, longitude: shop.lon)
            )
            annotation.title = shop.title
            annotations.append(annotation)
            annotationKinds[ObjectIdentifier(annotation)] = shop.selected ? "selected:" + shop.type : shop.type
            annotationIds[ObjectIdentifier(annotation)] = shop.id
        }
        for cluster in state.clusters {
            let annotation = CoffeeAnnotation(
                id: cluster.id,
                kind: "cluster",
                coordinate: CLLocationCoordinate2D(latitude: cluster.lat, longitude: cluster.lon)
            )
            annotation.title = String(cluster.count)
            annotations.append(annotation)
            annotationKinds[ObjectIdentifier(annotation)] = "cluster:\(cluster.count)"
            annotationIds[ObjectIdentifier(annotation)] = cluster.id
        }
        for zone in state.zones {
            let annotation = CoffeeAnnotation(
                id: zone.id,
                kind: "zone",
                coordinate: CLLocationCoordinate2D(latitude: zone.lat, longitude: zone.lon)
            )
            annotation.title = zone.name
            annotations.append(annotation)
            annotationKinds[ObjectIdentifier(annotation)] = "zone"
            annotationIds[ObjectIdentifier(annotation)] = zone.id
            annotationZoneColors[ObjectIdentifier(annotation)] = (zone.color, state.dark)
        }
        map.addAnnotations(annotations)
    }

    func moveCamera(
        mapView: UIView,
        latitude: Double,
        longitude: Double,
        zoom: Float,
        animated: Bool
    ) {
        guard let map = mapView as? MLNMapView else { return }
        map.setCenter(
            CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            zoomLevel: Double(zoom),
            animated: animated
        )
    }

    func mapView(
        _ mapView: MLNMapView,
        imageFor annotation: MLNAnnotation
    ) -> MLNAnnotationImage? {
        let key = ObjectIdentifier(annotation)
        guard let kind = annotationKinds[key] else { return nil }
        if kind == "cluster" || kind.hasPrefix("cluster:") {
            let count = Int(kind.split(separator: ":").last ?? "0") ?? 0
            return MLNAnnotationImage(
                image: clusterImage(count: count),
                reuseIdentifier: "coffee-cluster"
            )
        }
        if kind == "zone" {
            let appearance = annotationZoneColors[key] ?? (color: "#B07A45", dark: false)
            return MLNAnnotationImage(
                image: zoneImage(color: appearance.color, dark: appearance.dark),
                reuseIdentifier: "coffee-zone-\(appearance.color)-\(appearance.dark)"
            )
        }
        let image = mascotImage(for: kind) ?? UIImage(systemName: "mappin.circle.fill")!
        return MLNAnnotationImage(image: image, reuseIdentifier: "coffee-shop-" + kind)
    }

    func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
        let key = ObjectIdentifier(annotation)
        guard let callback = callbacksByMap[ObjectIdentifier(mapView)],
              let id = annotationIds[key] else { return }
        let kind = annotationKinds[key] ?? ""
        if kind == "zone" {
            callback.onZoneClick(zoneId: id)
        } else if kind == "cluster" || kind.hasPrefix("cluster:") {
            // Cluster taps are intentionally left to the map camera; the
            // server-provided cluster bounds are not part of the callback ABI.
        } else {
            callback.onShopClick(shopId: id)
        }
    }

    func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
        guard let callback = callbacksByMap[ObjectIdentifier(mapView)] else { return }
        let bounds = mapView.visibleCoordinateBounds
        let zoom = Float(mapView.zoomLevel)
        callback.onBoundsChanged(
            minLat: bounds.sw.latitude,
            minLon: bounds.sw.longitude,
            maxLat: bounds.ne.latitude,
            maxLon: bounds.ne.longitude,
            zoom: zoom
        )
    }

    private func mascotImage(for type: String) -> UIImage? {
        let selected = type.hasPrefix("selected:")
        let baseType = selected ? String(type.dropFirst("selected:".count)) : type
        let name: String
        switch baseType {
        case "specialty": name = "maskot_with_bean"
        case "cafe": name = "maskot_with_dessert"
        default: name = "maskot_with_cup"
        }
        let cacheKey = name + (selected ? "-selected" : "")
        if let cached = annotationImages[cacheKey] { return cached }
        let image = bundleImage(named: name).flatMap { resize($0, side: selected ? 50 : 44) }
        if let image { annotationImages[cacheKey] = image }
        return image
    }

    private func bundleImage(named name: String) -> UIImage? {
        let file = name + ".png"
        let root = Bundle.main.resourcePath
        let candidates = [
            Bundle.main.path(forResource: name, ofType: "png"),
            root.map { "\($0)/compose-resources/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
            root.map { "\($0)/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
            root.map { "\($0)/Frameworks/ComposeApp.framework/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
        ].compactMap { $0 }
        return candidates.compactMap(UIImage.init(contentsOfFile:)).first
    }

    private func resize(_ image: UIImage, side: CGFloat) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: side, height: side))
        return renderer.image { _ in image.draw(in: CGRect(x: 0, y: 0, width: side, height: side)) }
    }

    private func clusterImage(count: Int) -> UIImage {
        let side: CGFloat = count > 99 ? 52 : 44
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: side, height: side))
        return renderer.image { context in
            UIColor.systemYellow.setFill()
            context.cgContext.fillEllipse(in: CGRect(x: 1, y: 1, width: side - 2, height: side - 2))
            UIColor.white.setStroke()
            context.cgContext.setLineWidth(2)
            context.cgContext.strokeEllipse(in: CGRect(x: 2, y: 2, width: side - 4, height: side - 4))
            let text = "\(count)" as NSString
            text.draw(
                in: CGRect(x: 0, y: (side - 18) / 2, width: side, height: 18),
                withAttributes: [
                    .font: UIFont.boldSystemFont(ofSize: 13),
                    .foregroundColor: UIColor.white,
                    .paragraphStyle: centeredParagraphStyle(),
                ]
            )
        }
    }

    private func zoneImage(color: String, dark: Bool) -> UIImage {
        let cacheKey = "zone-\(color)-\(dark)"
        if let cached = annotationImages[cacheKey] { return cached }
        let rgb = Int(String(color.dropFirst()), radix: 16) ?? 0xB07A45
        let tint = UIColor(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 34, height: 34))
        let image = renderer.image { context in
            tint.withAlphaComponent(0.9).setFill()
            context.cgContext.fillEllipse(in: CGRect(x: 2, y: 2, width: 30, height: 30))
            (dark ? UIColor.white : UIColor.black.withAlphaComponent(0.65)).setStroke()
            context.cgContext.setLineWidth(2)
            context.cgContext.strokeEllipse(in: CGRect(x: 3, y: 3, width: 28, height: 28))
        }
        annotationImages[cacheKey] = image
        return image
    }

    private func centeredParagraphStyle() -> NSParagraphStyle {
        let style = NSMutableParagraphStyle()
        style.alignment = .center
        return style
    }
}

private final class CoffeeAnnotation: MLNPointAnnotation {
    let id: String
    let kind: String

    init(id: String, kind: String, coordinate: CLLocationCoordinate2D) {
        self.id = id
        self.kind = kind
        super.init()
        self.coordinate = coordinate
    }

    required init?(coder: NSCoder) {
        self.id = ""
        self.kind = ""
        super.init(coder: coder)
    }
}

private struct MapState: Decodable {
    let dark: Bool
    let showAttribution: Bool?
    let shops: [ShopState]
    let clusters: [ClusterState]
    let zones: [ZoneState]
}

private struct ShopState: Decodable {
    let id: String
    let title: String
    let lat: Double
    let lon: Double
    let type: String
    let selected: Bool
}

private struct ClusterState: Decodable {
    let id: String
    let lat: Double
    let lon: Double
    let count: Int
}

private struct ZoneState: Decodable {
    let id: String
    let name: String
    let color: String
    let lat: Double
    let lon: Double
    let radius: Double
    let polygon: [[Double]]
}
