import ComposeApp
import Foundation
import CoreText
import MapLibre
import UIKit

/// Native rendering only: shared Kotlin owns map content, selection and navigation.
/// This file is used by both the Xcode and xtool application targets.
final class MapLibreMapProvider: NSObject, IosNativeMapProvider, MLNMapViewDelegate, UIGestureRecognizerDelegate {
    private static let lightStyle = URL(string: "https://tiles.openfreemap.org/styles/positron")!
    private static let darkStyle = URL(string: "https://tiles.openfreemap.org/styles/dark")!
    private static let zoneSource = "cp-zones"
    private static let labelSource = "cp-zone-labels"
    private static let zoneFill = "cp-zones-fill"
    private static let zoneLine = "cp-zones-line"
    private static let zoneLabels = "cp-zone-labels"
    private let images = NSCache<NSString, UIImage>()
    private var clusterFont: UIFont?

    func createMapView() -> UIView {
        let map = CoffeeMapView(frame: .zero, styleURL: Self.lightStyle)
        map.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        map.automaticallyAdjustsContentInset = false
        map.delegate = self
        map.showsCompassView = false
        map.showsLogoView = false
        map.showsAttributionButton = true
        map.setCenter(CLLocationCoordinate2D(latitude: 53.9045, longitude: 27.5615), zoomLevel: 12, animated: false)
        let tap = UITapGestureRecognizer(target: self, action: #selector(didTapZone(_:)))
        tap.delegate = self
        tap.cancelsTouchesInView = false
        for recognizer in map.gestureRecognizers ?? [] {
            if let other = recognizer as? UITapGestureRecognizer, other.numberOfTapsRequired > 1 {
                tap.require(toFail: other)
            }
        }
        map.addGestureRecognizer(tap)
        return map
    }

    func updateMapView(mapView: UIView, stateJson: String, showsUserLocation: Bool, callbacks: IosNativeMapCallbacks) {
        guard let map = mapView as? CoffeeMapView,
              let data = stateJson.data(using: .utf8),
              let state = try? JSONDecoder().decode(MapState.self, from: data) else { return }
        let previous = map.content
        map.callbacks = callbacks
        map.content = state
        if map.showsUserLocation != showsUserLocation {
            map.showsUserLocation = showsUserLocation
        }
        map.showsAttributionButton = state.showAttribution
        let desiredStyle = state.dark ? Self.darkStyle : Self.lightStyle
        if map.styleURL != desiredStyle {
            map.styleURL = desiredStyle
        }
        syncAnnotations(map, state: state, previous: previous)
        if let style = map.style, previous?.zones != state.zones || previous?.dark != state.dark {
            syncZones(map, style: style, state: state)
        }
        if previous == nil {
            DispatchQueue.main.async { [weak map] in map?.reportBounds() }
        }
    }

    func moveCamera(mapView: UIView, latitude: Double, longitude: Double, zoom: Float, animated: Bool) {
        guard let map = mapView as? CoffeeMapView else { return }
        // Compose may send a target before layout or before the style has loaded.
        map.pendingCamera = CameraTarget(latitude: latitude, longitude: longitude, zoom: Double(zoom), animated: animated)
        map.applyPendingCamera()
    }

    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
        guard let map = mapView as? CoffeeMapView else { return }
        let dark = map.content?.dark ?? false
        applyPalette(style, dark: dark)
        map.zoneImageNames.removeAll()
        if let state = map.content { syncZones(map, style: style, state: state) }
        map.applyPendingCamera()
        map.reportBounds()
    }

    private func syncAnnotations(_ map: CoffeeMapView, state: MapState, previous: MapState?) {
        let old = map.points.mapValues { snapshot($0, map: map) }
        let keys = Set(state.shops.map { "shop:" + $0.id } + state.clusters.map { "cluster:" + $0.id })
        let animate = previous != nil && !UIAccessibility.isReduceMotionEnabled
        for shop in state.shops { syncShop(shop, map: map, state: state, previous: previous, old: old, animate: animate) }
        for cluster in state.clusters { syncCluster(cluster, map: map, state: state, previous: previous, old: old, animate: animate) }
        for key in Array(map.points.keys) where !keys.contains(key) {
            retire(key, map: map, clusters: state.clusters, animate: animate)
        }
    }

    private func syncShop(_ shop: ShopState, map: CoffeeMapView, state: MapState,
                          previous: MapState?, old: [String: MarkerSnapshot], animate: Bool) {
        let key = "shop:" + shop.id
        let annotation = map.points[key] ?? CoffeeAnnotation(id: shop.id, cluster: false)
        annotation.title = shop.title
        annotation.type = shop.type
        annotation.dark = state.dark
        annotation.highlighted = shop.selected
        syncPoint(annotation, key: key, latitude: shop.lat, longitude: shop.lon, map: map,
                  previous: previous, old: old, animate: animate)
    }

    private func syncCluster(_ cluster: ClusterState, map: CoffeeMapView, state: MapState,
                             previous: MapState?, old: [String: MarkerSnapshot], animate: Bool) {
        let key = "cluster:" + cluster.id
        let annotation = map.points[key] ?? CoffeeAnnotation(id: cluster.id, cluster: true)
        annotation.count = cluster.count
        annotation.dark = state.dark
        annotation.title = String(cluster.count)
        syncPoint(annotation, key: key, latitude: cluster.lat, longitude: cluster.lon, map: map,
                  previous: previous, old: old, animate: animate)
    }

    private func syncPoint(_ annotation: CoffeeAnnotation, key: String, latitude: Double, longitude: Double,
                           map: CoffeeMapView, previous: MapState?, old: [String: MarkerSnapshot], animate: Bool) {
        let retained = map.points[key] != nil
        let changed = !retained || annotation.exiting || annotation.coordinate.latitude != latitude || annotation.coordinate.longitude != longitude
        let parent = markerParent(latitude: latitude, longitude: longitude, clusters: previous?.clusters ?? [], excluding: key)
        let from = old[key] ?? parent.flatMap { old["cluster:" + $0.id] }
        annotation.exiting = false
        annotation.generation += 1
        updateCoordinate(annotation, latitude: latitude, longitude: longitude)
        annotation.transition = animate && changed ? MarkerTransition(from: from, retained: retained) : nil
        if !retained {
            map.points[key] = annotation
            map.addAnnotation(annotation)
        }
        if let view = map.view(for: annotation) as? CoffeePinView {
            configure(view, annotation: annotation, animated: true)
            applyTransition(annotation, view: view, map: map)
        }
    }

    private func markerParent(latitude: Double, longitude: Double, clusters: [ClusterState], excluding key: String) -> ClusterState? {
        clusters.filter { "cluster:" + $0.id != key && $0.bounds.contains(latitude: latitude, longitude: longitude) }
            .min { $0.bounds.extent < $1.bounds.extent }
    }

    private func snapshot(_ annotation: CoffeeAnnotation, map: CoffeeMapView) -> MarkerSnapshot {
        let anchor = map.convert(annotation.coordinate, toPointTo: map)
        return (map.view(for: annotation) as? CoffeePinView)?.snapshot(anchor: anchor)
            ?? MarkerSnapshot(center: anchor, alpha: 1, scale: 1)
    }

    private func applyTransition(_ annotation: CoffeeAnnotation, view: CoffeePinView, map: CoffeeMapView) {
        view.isUserInteractionEnabled = true
        view.accessibilityElementsHidden = false
        if UIAccessibility.isReduceMotionEnabled { view.finishTransition() }
        guard let transition = annotation.transition else { return }
        annotation.transition = nil
        let anchor = map.convert(annotation.coordinate, toPointTo: map)
        let from = transition.from ?? MarkerSnapshot(center: anchor, alpha: 1, scale: 1)
        let initial = MarkerSnapshot(center: from.center, alpha: transition.retained ? from.alpha : 0,
                                     scale: transition.retained ? from.scale : 0.65)
        view.transition(from: initial, to: MarkerSnapshot(center: anchor, alpha: 1, scale: 1), anchor: anchor)
    }

    private func retire(_ key: String, map: CoffeeMapView, clusters: [ClusterState], animate: Bool) {
        guard let annotation = map.points[key] else { return }
        if annotation.exiting && animate { return }
        guard animate, let view = map.view(for: annotation) as? CoffeePinView else {
            map.points.removeValue(forKey: key)
            map.removeAnnotation(annotation)
            return
        }
        annotation.exiting = true
        let generation = annotation.generation
        let anchor = map.convert(annotation.coordinate, toPointTo: map)
        let parent = markerParent(latitude: annotation.coordinate.latitude, longitude: annotation.coordinate.longitude, clusters: clusters, excluding: key)
        let destination = parent.map { map.convert(CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon), toPointTo: map) } ?? anchor
        view.isUserInteractionEnabled = false
        view.accessibilityElementsHidden = true
        view.transition(from: view.snapshot(anchor: anchor), to: MarkerSnapshot(center: destination, alpha: 0, scale: 0.5), anchor: anchor) { [weak map] in
            guard let map, annotation.exiting, annotation.generation == generation, map.points[key] === annotation else { return }
            map.points.removeValue(forKey: key)
            map.removeAnnotation(annotation)
        }
    }

    private func updateCoordinate(_ annotation: CoffeeAnnotation, latitude: Double, longitude: Double) {
        if annotation.coordinate.latitude != latitude || annotation.coordinate.longitude != longitude {
            annotation.coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        }
    }

    func mapView(_ mapView: MLNMapView, viewFor annotation: MLNAnnotation) -> MLNAnnotationView? {
        guard let annotation = annotation as? CoffeeAnnotation else { return nil }
        let view = (mapView.dequeueReusableAnnotationView(withIdentifier: "coffee-pin") as? CoffeePinView)
            ?? CoffeePinView(reuseIdentifier: "coffee-pin")
        configure(view, annotation: annotation, animated: false)
        if let map = mapView as? CoffeeMapView { applyTransition(annotation, view: view, map: map) }
        return view
    }

    private func configure(_ view: CoffeePinView, annotation: CoffeeAnnotation, animated: Bool) {
        let image = annotation.cluster && annotation.count != 1
            ? clusterImage(count: annotation.count, dark: annotation.dark)
            : pinImage(type: annotation.cluster ? "specialty" : annotation.type, selected: annotation.highlighted, dark: annotation.dark)
        view.configure(image: image, highlighted: annotation.highlighted, animated: animated)
        view.accessibilityLabel = annotation.title
    }

    func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
        guard let map = mapView as? CoffeeMapView, let point = annotation as? CoffeeAnnotation else { return }
        // Shared selection is authoritative; don't let MapLibre's own selection pan the camera.
        map.deselectAnnotation(annotation, animated: false)
        guard !point.exiting else { return }
        if point.cluster {
            if let cluster = map.content?.clusters.first(where: { $0.id == point.id }) {
                zoomToCluster(cluster, map: map)
            }
        } else {
            map.callbacks?.onShopClick(shopId: point.id)
        }
    }

    func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
        if let map = mapView as? CoffeeMapView { map.reportBounds() }
    }

    // Zone polygons and labels are style layers below the native pin views.
    private func syncZones(_ map: CoffeeMapView, style: MLNStyle, state: MapState) {
        let shapes = state.zones.compactMap { zone -> MLNPolygonFeature? in
            var coordinates = zone.polygon.compactMap { pair -> CLLocationCoordinate2D? in
                guard pair.count == 2 else { return nil }
                let coordinate = CLLocationCoordinate2D(latitude: pair[0], longitude: pair[1])
                return CLLocationCoordinate2DIsValid(coordinate) ? coordinate : nil
            }
            guard coordinates.count >= 4 else { return nil }
            let count = UInt(coordinates.count)
            let polygon = MLNPolygonFeature(coordinates: &coordinates, count: count)
            polygon.attributes = ["zoneId": zone.id, "zoneColor": zone.color]
            return polygon
        }
        let labels = state.zones.map { zone -> MLNPointFeature in
            let key = "zone-\(state.dark)-\(zone.color)-\(zone.name)-\(zone.count)"
            style.setImage(zoneImage(zone, dark: state.dark), forName: key)
            let label = MLNPointFeature()
            label.coordinate = CLLocationCoordinate2D(latitude: zone.lat, longitude: zone.lon)
            label.attributes = ["zoneId": zone.id, "icon": key]
            return label
        }
        let zoneSource = updateSource(Self.zoneSource, shapes: shapes, style: style)
        let labelSource = updateSource(Self.labelSource, shapes: labels, style: style)
        if style.layer(withIdentifier: Self.zoneFill) == nil {
            let fill = MLNFillStyleLayer(identifier: Self.zoneFill, source: zoneSource)
            fill.fillColor = NSExpression(forKeyPath: "zoneColor")
            fill.fillOpacity = NSExpression(forConstantValue: state.dark ? 0.16 : 0.18)
            style.addLayer(fill)
            let line = MLNLineStyleLayer(identifier: Self.zoneLine, source: zoneSource)
            line.lineColor = NSExpression(forKeyPath: "zoneColor")
            line.lineWidth = NSExpression(forConstantValue: 2)
            line.lineOpacity = NSExpression(forConstantValue: state.dark ? 0.75 : 0.80)
            style.addLayer(line)
            let label = MLNSymbolStyleLayer(identifier: Self.zoneLabels, source: labelSource)
            label.iconImageName = NSExpression(forKeyPath: "icon")
            label.iconAllowsOverlap = NSExpression(forConstantValue: true)
            label.iconIgnoresPlacement = NSExpression(forConstantValue: true)
            label.maximumZoomLevel = 15
            style.addLayer(label)
        }
        // Remove stale label sprites after replacing source data (counts can change on refresh).
        let imageNames = Set(state.zones.map { "zone-\(state.dark)-\($0.color)-\($0.name)-\($0.count)" })
        for name in map.zoneImageNames.subtracting(imageNames) { style.removeImage(forName: name) }
        map.zoneImageNames = imageNames
    }

    private func updateSource(_ id: String, shapes: [MLNShape & MLNFeature], style: MLNStyle) -> MLNShapeSource {
        // A geometry collection drops feature attributes; a feature collection keeps
        // zoneId, zoneColor and icon available for styling and tap hit testing.
        let collection = MLNShapeCollectionFeature(shapes: shapes)
        if let source = style.source(withIdentifier: id) as? MLNShapeSource {
            source.shape = collection
            return source
        }
        let source = MLNShapeSource(identifier: id, shape: collection, options: nil)
        style.addSource(source)
        return source
    }

    @objc private func didTapZone(_ recognizer: UITapGestureRecognizer) {
        guard recognizer.state == .ended, let map = recognizer.view as? CoffeeMapView, map.style != nil else { return }
        let point = recognizer.location(in: map)
        // An overlapping shop always wins over the zone below it.
        if (map.visibleAnnotations ?? []).contains(where: { annotation in
            guard let view = map.view(for: annotation) else { return false }
            return view.bounds.contains(view.convert(point, from: map))
        }) { return }
        let features = map.visibleFeatures(at: point, styleLayerIdentifiers: [Self.zoneLabels, Self.zoneFill])
        if let id = features.first?.attribute(forKey: "zoneId") as? String { map.callbacks?.onZoneClick(zoneId: id) }
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        otherGestureRecognizer is UITapGestureRecognizer
    }

    private func zoomToCluster(_ cluster: ClusterState, map: CoffeeMapView) {
        // Match the web: each tap advances at least one zoom level, including large bounds.
        let bounds = cluster.bounds
        if bounds.minLat == bounds.maxLat && bounds.minLon == bounds.maxLon {
            moveCamera(mapView: map, latitude: bounds.minLat, longitude: bounds.minLon,
                       zoom: Float(min(map.zoomLevel + 1, 22)), animated: !UIAccessibility.isReduceMotionEnabled)
            return
        }
        let padding = min(map.bounds.width, map.bounds.height) * 0.25
        let insets = UIEdgeInsets(top: padding, left: padding, bottom: padding, right: padding)
        let camera = map.cameraThatFitsCoordinateBounds(MLNCoordinateBounds(
            sw: CLLocationCoordinate2D(latitude: bounds.minLat, longitude: bounds.minLon),
            ne: CLLocationCoordinate2D(latitude: bounds.maxLat, longitude: bounds.maxLon)
        ), edgePadding: insets)
        let fitted = MLNZoomLevelForAltitude(camera.altitude, camera.pitch, camera.centerCoordinate.latitude, map.bounds.size)
        let zoom = min(22, max(map.zoomLevel + 1, min(fitted, map.zoomLevel + 2)))
        camera.altitude = MLNAltitudeForZoomLevel(zoom, camera.pitch, camera.centerCoordinate.latitude, map.bounds.size)
        map.setCamera(camera, withDuration: UIAccessibility.isReduceMotionEnabled ? 0 : 0.35,
                      animationTimingFunction: CAMediaTimingFunction(name: .easeInEaseOut), completionHandler: nil)
    }

    private func pinImage(type: String, selected: Bool, dark: Bool) -> UIImage {
        let key = "pin-\(type)-\(selected)-\(dark)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let size: CGFloat = selected ? 41 : 31
        let pad: CGFloat = selected ? 13.5 : 4
        let side = size + pad * 2
        let rect = CGRect(x: pad, y: pad, width: size, height: size)
        let image = UIGraphicsImageRenderer(size: CGSize(width: side, height: side)).image { context in
            drawPinBackground(context.cgContext, rect: rect, selected: selected, dark: dark)
            drawPinIcon(type: type, rect: rect, dark: dark)
            drawPinRing(context.cgContext, rect: rect, selected: selected, dark: dark)
        }
        images.setObject(image, forKey: key)
        return image
    }

    private func drawPinBackground(_ cg: CGContext, rect: CGRect, selected: Bool, dark: Bool) {
        cg.saveGState()
        cg.setShadow(offset: CGSize(width: 0, height: selected ? 2 : 1), blur: selected ? 8 : 3,
                     color: UIColor(hex: "#1A1412").withAlphaComponent(0.35).cgColor)
        UIColor(hex: dark ? "#2D241F" : "#FFFFFF").setFill()
        cg.fillEllipse(in: rect)
        cg.restoreGState()
    }

    private func drawPinIcon(type: String, rect: CGRect, dark: Bool) {
        let name = type == "specialty" ? "map_coffeepeek_bean" : (type == "cafe" ? "map_fork_knife" : "map_cup_fill")
        let tint = UIColor(hex: dark ? "#FFFFFF" : "#2D241F")
        bundleImage(named: name)?.withTintColor(tint, renderingMode: .alwaysOriginal)
            .draw(in: rect.insetBy(dx: rect.width * 0.20, dy: rect.height * 0.20))
    }

    private func drawPinRing(_ cg: CGContext, rect: CGRect, selected: Bool, dark: Bool) {
        let border = dark ? "#3D2F28" : "#E7E5E4"
        UIColor(hex: selected ? "#D4A84B" : border).setStroke()
        cg.setLineWidth(selected ? 2 : 1.1)
        let inset: CGFloat = selected ? -2.5 : 0.55
        cg.strokeEllipse(in: rect.insetBy(dx: inset, dy: inset))
    }

    private func bundleImage(named name: String) -> UIImage? {
        let key = "asset-\(name)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let file = name + ".png"
        let root = Bundle.main.resourcePath
        let candidates = [
            Bundle.main.path(forResource: name, ofType: "png"),
            root.map { "\($0)/compose-resources/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
            root.map { "\($0)/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
            root.map { "\($0)/Frameworks/ComposeApp.framework/composeResources/coffeepeek.composeapp.generated.resources/drawable/\(file)" },
        ].compactMap { $0 }
        let image = candidates.lazy.compactMap(UIImage.init(contentsOfFile:)).first
        if let image { images.setObject(image, forKey: key) }
        return image
    }

    private func clusterImage(count: Int, dark: Bool) -> UIImage {
        let label = count > 99 ? "99+" : String(count)
        let key = "cluster-\(label)-\(dark)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let size: CGFloat = 34.1
        let side = size + 8
        let image = UIGraphicsImageRenderer(size: CGSize(width: side, height: side)).image { context in
            let rect = CGRect(x: 4, y: 4, width: size, height: size)
            drawPinBackground(context.cgContext, rect: rect, selected: false, dark: dark)
            drawPinRing(context.cgContext, rect: rect, selected: false, dark: dark)
            drawClusterCount(label, side: side, dark: dark)
        }
        images.setObject(image, forKey: key)
        return image
    }

    private func clusterTypeface() -> UIFont {
        if let clusterFont { return clusterFont }
        let root = Bundle.main.bundlePath
        let directories = [root + "/compose-resources", root, root + "/Frameworks/ComposeApp.framework"]
        let paths = directories.map { $0 + "/composeResources/coffeepeek.composeapp.generated.resources/font/manrope_extrabold.ttf" }
        var loaded: UIFont?
        if let path = paths.first(where: { FileManager.default.fileExists(atPath: $0) }),
           let provider = CGDataProvider(url: URL(fileURLWithPath: path) as CFURL), let font = CGFont(provider),
           let name = font.postScriptName {
            CTFontManagerRegisterGraphicsFont(font, nil)
            loaded = UIFont(name: name as String, size: 12)
        }
        let result = loaded ?? UIFont.systemFont(ofSize: 12, weight: .heavy)
        clusterFont = result
        return result
    }

    private func drawClusterCount(_ label: String, side: CGFloat, dark: Bool) {
        let attributes: [NSAttributedString.Key: Any] = [
            .font: clusterTypeface(),
            .foregroundColor: UIColor(hex: dark ? "#FFFFFF" : "#2D241F"), .kern: -0.24,
        ]
        let text = label as NSString
        let measured = text.size(withAttributes: attributes)
        text.draw(at: CGPoint(x: (side - measured.width) / 2, y: (side - measured.height) / 2), withAttributes: attributes)
    }

    private func zoneImage(_ zone: ZoneState, dark: Bool) -> UIImage {
        let shortName = zone.name.count > 18 ? String(zone.name.prefix(17)) + "…" : zone.name
        let text = "\(shortName) · \(zone.count)" as NSString
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.boldSystemFont(ofSize: 12),
            .foregroundColor: UIColor(hex: dark ? "#E6DDD3" : "#4A4038"),
        ]
        let measured = text.size(withAttributes: attributes)
        let rect = CGRect(x: 5, y: 5, width: ceil(measured.width) + 16, height: ceil(measured.height) + 8)
        return UIGraphicsImageRenderer(size: CGSize(width: rect.width + 10, height: rect.height + 10)).image { context in
            let path = UIBezierPath(roundedRect: rect, cornerRadius: rect.height / 2)
            context.cgContext.setShadow(offset: CGSize(width: 0, height: 1), blur: 3, color: UIColor.black.withAlphaComponent(0.12).cgColor)
            UIColor(hex: dark ? "#2A221D" : "#FFFFFF").withAlphaComponent(0.92).setFill()
            path.fill()
            context.cgContext.setShadow(offset: .zero, blur: 0, color: nil)
            UIColor(hex: zone.color).withAlphaComponent(0.8).setStroke()
            path.lineWidth = 1.5
            path.stroke()
            text.draw(at: CGPoint(x: rect.minX + 8, y: rect.midY - measured.height / 2), withAttributes: attributes)
        }
    }

    /// Match the existing Android CoffeePeek palette instead of unmodified OpenFreeMap black.
    private func applyPalette(_ style: MLNStyle, dark: Bool) {
        func color(_ light: String, _ night: String) -> NSExpression {
            NSExpression(forConstantValue: UIColor(hex: dark ? night : light))
        }
        let noise = ["house-number", "house_number", "transit_stop", "transit-stop", "bus_stop", "bus-stop", "ferry_terminal", "ferry-terminal", "aerodrome_label", "aerodrome-label", "airport_label", "airport-label"]
        for layer in style.layers {
            let id = layer.identifier.lowercased()
            let segments = id.split(whereSeparator: { "-_.".contains($0) })
            if segments.contains(where: { ["poi", "housenumber", "oneway"].contains(String($0)) }) || noise.contains(where: { id.contains($0) }) {
                layer.isVisible = false
                continue
            }
            if id.contains("highway_path") { layer.minimumZoomLevel = max(layer.minimumZoomLevel, 15) }
            if let background = layer as? MLNBackgroundStyleLayer {
                background.backgroundColor = color("#F8F6F3", "#1A1412")
            } else if let fill = layer as? MLNFillStyleLayer {
                if id == "water" || id.hasPrefix("water_") {
                    fill.fillColor = color("#D3DEE1", "#26343A")
                    fill.fillOutlineColor = color("#B2C5CA", "#42545B")
                } else if id == "park" || id.contains("landcover_wood") || id.contains("landuse_park") {
                    fill.fillColor = color("#E4E8DF", "#25231D")
                } else if id.contains("building") {
                    fill.fillColor = color("#E3DDD7", "#302722")
                    fill.fillOutlineColor = color("#D6CEC7", "#332A26")
                } else if id.contains("residential") { fill.fillColor = color("#EFEAE5", "#211B18") }
            } else if let line = layer as? MLNLineStyleLayer {
                if id.contains("waterway") { line.lineColor = color("#B2C5CA", "#42545B") }
                else if id.contains("boundary") { line.lineColor = color("#AA9D94", "#66564D") }
                else if ["highway", "road", "bridge", "tunnel"].contains(where: { id.contains($0) }) {
                    if id.contains("casing") { line.lineColor = color("#D6CEC7", "#332A26") }
                    else if ["inner", "motorway", "major"].contains(where: { id.contains($0) }) { line.lineColor = color("#FFFFFF", "#4B403A") }
                    else { line.lineColor = color("#E8E2DC", "#3D342F") }
                }
            } else if let symbol = layer as? MLNSymbolStyleLayer {
                symbol.textColor = id.contains("water") ? color("#687E84", "#9AAFB5") : color("#625A55", "#C8BEB7")
                symbol.textHaloColor = color("#FAF8F5", "#1A1412")
                symbol.textHaloWidth = NSExpression(forConstantValue: 1.2)
            }
        }
    }
}

// State belongs to an individual map (full-screen and detail maps can coexist).
private final class CoffeeMapView: MLNMapView {
    var callbacks: IosNativeMapCallbacks?
    var content: MapState?
    var points: [String: CoffeeAnnotation] = [:]
    var zoneImageNames: Set<String> = []
    var pendingCamera: CameraTarget?
    private var reportedSize: CGSize = .zero

    override func layoutSubviews() {
        super.layoutSubviews()
        applyPendingCamera()
        if reportedSize != bounds.size {
            reportedSize = bounds.size
            DispatchQueue.main.async { [weak self] in self?.reportBounds() }
        }
    }

    func reportBounds() {
        guard bounds.width > 0, bounds.height > 0, style != nil, pendingCamera == nil else { return }
        let visible = visibleCoordinateBounds
        callbacks?.onBoundsChanged(minLat: visible.sw.latitude, minLon: visible.sw.longitude,
                                   maxLat: visible.ne.latitude, maxLon: visible.ne.longitude, zoom: Float(zoomLevel))
        let center = centerCoordinate
        callbacks?.onCameraCenterChanged(latitude: center.latitude, longitude: center.longitude)
    }

    func applyPendingCamera() {
        guard style != nil, bounds.width > 0, bounds.height > 0, let target = pendingCamera else { return }
        pendingCamera = nil
        let targetCamera = camera
        targetCamera.centerCoordinate = CLLocationCoordinate2D(latitude: target.latitude, longitude: target.longitude)
        targetCamera.altitude = MLNAltitudeForZoomLevel(target.zoom, targetCamera.pitch, target.latitude, bounds.size)
        setCamera(targetCamera, withDuration: target.animated && !UIAccessibility.isReduceMotionEnabled ? 0.45 : 0,
                  animationTimingFunction: CAMediaTimingFunction(name: .easeInEaseOut), completionHandler: nil)
    }
}

private struct CameraTarget {
    let latitude: Double
    let longitude: Double
    let zoom: Double
    let animated: Bool
}

private final class CoffeeAnnotation: MLNPointAnnotation {
    let id: String
    let cluster: Bool
    var type = "coffee_bar"
    var dark = false
    var count = 0
    var highlighted = false
    var exiting = false
    var generation = 0
    var transition: MarkerTransition?

    init(id: String, cluster: Bool) {
        self.id = id
        self.cluster = cluster
        super.init()
    }

    required init?(coder: NSCoder) {
        id = ""
        cluster = false
        super.init(coder: coder)
    }
}

private final class CoffeePinView: MLNAnnotationView {
    private let motion = UIView()
    private var markerAnimator: UIViewPropertyAnimator?
    private let picture = UIImageView()
    private let pulse = CAShapeLayer()
    private var highlighted = false

    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        bounds = CGRect(x: 0, y: 0, width: 44, height: 44)
        scalesWithViewingDistance = false
        motion.frame = bounds
        motion.isUserInteractionEnabled = false
        addSubview(motion)
        motion.layer.addSublayer(pulse)
        motion.addSubview(picture)
        isAccessibilityElement = true
        accessibilityTraits = .button
        NotificationCenter.default.addObserver(self, selector: #selector(updatePulse), name: UIAccessibility.reduceMotionStatusDidChangeNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    deinit { NotificationCenter.default.removeObserver(self) }

    override func prepareForReuse() {
        super.prepareForReuse()
        highlighted = false
        markerAnimator?.stopAnimation(true)
        markerAnimator = nil
        motion.layer.removeAllAnimations()
        motion.transform = .identity
        motion.alpha = 1
        isUserInteractionEnabled = true
        accessibilityElementsHidden = false
        picture.layer.removeAllAnimations()
        picture.transform = .identity
        pulse.removeAllAnimations()
        pulse.isHidden = true
    }

    func snapshot(anchor: CGPoint) -> MarkerSnapshot {
        let layer = motion.layer.presentation() ?? motion.layer
        let transform = layer.affineTransform()
        return MarkerSnapshot(center: CGPoint(x: anchor.x + transform.tx, y: anchor.y + transform.ty),
                              alpha: CGFloat(layer.opacity), scale: hypot(transform.a, transform.b))
    }

    func transition(from: MarkerSnapshot, to: MarkerSnapshot, anchor: CGPoint, completion: @escaping () -> Void = {}) {
        markerAnimator?.stopAnimation(true)
        motion.layer.removeAllAnimations()
        motion.transform = from.transform(anchor: anchor)
        motion.alpha = from.alpha
        let timing = UICubicTimingParameters(controlPoint1: CGPoint(x: 0.22, y: 1), controlPoint2: CGPoint(x: 0.36, y: 1))
        let animator = UIViewPropertyAnimator(duration: UIAccessibility.isReduceMotionEnabled ? 0 : 0.38, timingParameters: timing)
        animator.addAnimations { [weak self] in
            guard let self else { return }
            self.motion.transform = to.transform(anchor: anchor)
            self.motion.alpha = to.alpha
        }
        animator.addCompletion { [weak self] _ in
            self?.markerAnimator = nil
            completion()
        }
        markerAnimator = animator
        animator.startAnimation()
    }

    func finishTransition() {
        guard let animator = markerAnimator else { return }
        animator.stopAnimation(false)
        animator.finishAnimation(at: .end)
        markerAnimator = nil
    }

    func configure(image: UIImage, highlighted: Bool, animated: Bool) {
        let changed = self.highlighted != highlighted
        self.highlighted = highlighted
        picture.image = image
        picture.bounds = CGRect(origin: .zero, size: image.size)
        picture.center = CGPoint(x: bounds.midX, y: bounds.midY)
        layer.zPosition = highlighted ? 1 : 0
        if changed {
            picture.layer.removeAllAnimations()
            picture.transform = .identity
            if animated && !UIAccessibility.isReduceMotionEnabled {
                let scale: CGFloat = highlighted ? 31 / 41 : 41 / 31
                picture.transform = CGAffineTransform(scaleX: scale, y: scale)
                UIView.animate(withDuration: 0.28, delay: 0, usingSpringWithDamping: 0.78, initialSpringVelocity: 0,
                               options: [.beginFromCurrentState, .allowUserInteraction]) { self.picture.transform = .identity }
            }
            updatePulse()
        }
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        updatePulse()
    }

    @objc private func updatePulse() {
        if UIAccessibility.isReduceMotionEnabled { finishTransition() }
        pulse.removeAllAnimations()
        pulse.isHidden = !highlighted || window == nil
        guard highlighted, window != nil else { return }
        let reduced = UIAccessibility.isReduceMotionEnabled
        let radius: CGFloat = reduced ? 26 : 20
        pulse.frame = bounds
        pulse.path = UIBezierPath(ovalIn: CGRect(x: bounds.midX - radius, y: bounds.midY - radius, width: radius * 2, height: radius * 2)).cgPath
        pulse.fillColor = UIColor(hex: "#EAB308").withAlphaComponent(reduced ? 0.18 : 0.22).cgColor
        pulse.strokeColor = UIColor(hex: "#CA8A04").withAlphaComponent(reduced ? 0.35 : 0.6).cgColor
        pulse.lineWidth = 1.5
        guard !reduced else { return }
        let scale = CABasicAnimation(keyPath: "transform.scale")
        scale.fromValue = 1
        scale.toValue = 1.9
        scale.timingFunction = CAMediaTimingFunction(controlPoints: 0.215, 0.61, 0.355, 1)
        let fade = CABasicAnimation(keyPath: "opacity")
        fade.fromValue = 1
        fade.toValue = 0
        let group = CAAnimationGroup()
        group.animations = [scale, fade]
        group.duration = 1.6
        group.repeatCount = .infinity
        pulse.add(group, forKey: "selection-pulse")
    }
}

private extension UIColor {
    convenience init(hex: String) {
        let rgb = Int(hex.trimmingCharacters(in: CharacterSet(charactersIn: "#")), radix: 16) ?? 0xB07A45
        self.init(red: CGFloat((rgb >> 16) & 255) / 255, green: CGFloat((rgb >> 8) & 255) / 255,
                  blue: CGFloat(rgb & 255) / 255, alpha: 1)
    }
}

private struct MarkerSnapshot {
    let center: CGPoint
    let alpha: CGFloat
    let scale: CGFloat

    func transform(anchor: CGPoint) -> CGAffineTransform {
        CGAffineTransform(translationX: center.x - anchor.x, y: center.y - anchor.y).scaledBy(x: scale, y: scale)
    }
}

private struct MarkerTransition {
    let from: MarkerSnapshot?
    let retained: Bool
}

private struct MapState: Decodable {
    let dark: Bool
    let showAttribution: Bool
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
    let bounds: BoundsState
}

private struct BoundsState: Decodable {
    let minLat: Double
    let minLon: Double
    let maxLat: Double
    let maxLon: Double
    var extent: Double { maxLat - minLat + maxLon - minLon }

    func contains(latitude: Double, longitude: Double) -> Bool {
        latitude >= minLat && latitude <= maxLat && longitude >= minLon && longitude <= maxLon
    }
}

private struct ZoneState: Decodable, Equatable {
    let id: String
    let name: String
    let color: String
    let lat: Double
    let lon: Double
    let count: Int
    let polygon: [[Double]]
}
