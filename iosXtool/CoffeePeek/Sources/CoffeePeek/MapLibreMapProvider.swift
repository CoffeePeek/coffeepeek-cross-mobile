import ComposeApp
import Foundation
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

    func createMapView() -> UIView {
        let map = CoffeeMapView(frame: .zero, styleURL: Self.lightStyle)
        map.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        map.automaticallyAdjustsContentInset = false
        map.delegate = self
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
        syncAnnotations(map, state: state)
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

    private func syncAnnotations(_ map: CoffeeMapView, state: MapState) {
        let keys = Set(state.shops.map { "shop:" + $0.id } + state.clusters.map { "cluster:" + $0.id })
        for key in Array(map.points.keys) where !keys.contains(key) {
            if let annotation = map.points.removeValue(forKey: key) { map.removeAnnotation(annotation) }
        }
        for shop in state.shops {
            let key = "shop:" + shop.id
            let annotation = map.points[key] ?? CoffeeAnnotation(id: shop.id, cluster: false)
            annotation.title = shop.title
            annotation.type = shop.type
            annotation.highlighted = shop.selected
            updateCoordinate(annotation, latitude: shop.lat, longitude: shop.lon)
            if map.points[key] == nil {
                map.points[key] = annotation
                map.addAnnotation(annotation)
            }
            if let view = map.view(for: annotation) as? CoffeePinView {
                configure(view, annotation: annotation, animated: true)
            }
        }
        for cluster in state.clusters {
            let key = "cluster:" + cluster.id
            let annotation = map.points[key] ?? CoffeeAnnotation(id: cluster.id, cluster: true)
            annotation.count = cluster.count
            annotation.title = String(cluster.count)
            updateCoordinate(annotation, latitude: cluster.lat, longitude: cluster.lon)
            if map.points[key] == nil {
                map.points[key] = annotation
                map.addAnnotation(annotation)
            }
            if let view = map.view(for: annotation) as? CoffeePinView {
                configure(view, annotation: annotation, animated: false)
            }
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
        return view
    }

    private func configure(_ view: CoffeePinView, annotation: CoffeeAnnotation, animated: Bool) {
        let image = annotation.cluster && annotation.count != 1
            ? clusterImage(count: annotation.count)
            : pinImage(type: annotation.cluster ? "specialty" : annotation.type, selected: annotation.highlighted)
        view.configure(image: image, highlighted: annotation.highlighted, animated: animated)
        view.accessibilityLabel = annotation.title
    }

    func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
        guard let map = mapView as? CoffeeMapView, let point = annotation as? CoffeeAnnotation else { return }
        // Shared selection is authoritative; don't let MapLibre's own selection pan the camera.
        map.deselectAnnotation(annotation, animated: false)
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
        // Same 25% viewport padding, +2 zoom cap and maximum 17 as Android.
        let bounds = cluster.bounds
        if bounds.minLat == bounds.maxLat && bounds.minLon == bounds.maxLon {
            moveCamera(mapView: map, latitude: bounds.minLat, longitude: bounds.minLon,
                       zoom: Float(min(map.zoomLevel + 2, 17)), animated: true)
            return
        }
        let insets = UIEdgeInsets(top: map.bounds.height * 0.125, left: map.bounds.width * 0.125,
                                  bottom: map.bounds.height * 0.125, right: map.bounds.width * 0.125)
        let camera = map.cameraThatFitsCoordinateBounds(MLNCoordinateBounds(
            sw: CLLocationCoordinate2D(latitude: bounds.minLat, longitude: bounds.minLon),
            ne: CLLocationCoordinate2D(latitude: bounds.maxLat, longitude: bounds.maxLon)
        ), edgePadding: insets)
        let fitted = MLNZoomLevelForAltitude(camera.altitude, camera.pitch, camera.centerCoordinate.latitude, map.bounds.size)
        let zoom = min(fitted, min(map.zoomLevel + 2, 17))
        camera.altitude = MLNAltitudeForZoomLevel(zoom, camera.pitch, camera.centerCoordinate.latitude, map.bounds.size)
        map.setCamera(camera, withDuration: UIAccessibility.isReduceMotionEnabled ? 0 : 0.35,
                      animationTimingFunction: CAMediaTimingFunction(name: .easeInEaseOut), completionHandler: nil)
    }

    private func pinImage(type: String, selected: Bool) -> UIImage {
        let key = "pin-\(type)-\(selected)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let specialty = type == "specialty"
        let cafe = type == "cafe"
        let name = specialty ? "maskot_with_bean" : (cafe ? "maskot_with_dessert" : "maskot_with_cup")
        let fill = UIColor(hex: specialty ? "#EAB308" : (cafe ? "#78716C" : "#1A1412"))
        let ring = UIColor(hex: specialty ? "#CA8A04" : (cafe ? "#4A3D35" : "#3D2F28"))
        let size: CGFloat = selected ? 41 : 31
        let pad: CGFloat = selected ? 13.5 : 4
        let side = size + pad * 2
        let rect = CGRect(x: pad, y: pad, width: size, height: size)
        let mascot = bundleImage(named: name)
        let image = UIGraphicsImageRenderer(size: CGSize(width: side, height: side)).image { context in
            let cg = context.cgContext
            cg.saveGState()
            cg.setShadow(offset: CGSize(width: 0, height: selected ? 2 : 1), blur: selected ? 8 : 3,
                         color: UIColor(hex: "#1A1412").withAlphaComponent(0.35).cgColor)
            fill.setFill()
            cg.fillEllipse(in: rect)
            cg.restoreGState()
            cg.saveGState()
            cg.addEllipse(in: rect)
            cg.clip()
            let drawSize = size * 1.72
            mascot?.draw(in: CGRect(x: side / 2 - drawSize / 2, y: side / 2 - drawSize * 0.58 + size * 0.08,
                                   width: drawSize, height: drawSize))
            cg.restoreGState()
            UIColor.white.setStroke()
            cg.setLineWidth(2.5)
            cg.strokeEllipse(in: rect.insetBy(dx: 1.25, dy: 1.25))
            let ringWidth: CGFloat = selected ? 1.6 : 1.1
            ring.withAlphaComponent(selected ? 0.95 : 0.7).setStroke()
            cg.setLineWidth(ringWidth)
            cg.strokeEllipse(in: rect.insetBy(dx: ringWidth / 2, dy: ringWidth / 2))
            if selected {
                UIColor(hex: "#EAB308").withAlphaComponent(0.6).setStroke()
                cg.setLineWidth(2)
                cg.strokeEllipse(in: rect.insetBy(dx: -2.5, dy: -2.5))
            }
        }
        images.setObject(image, forKey: key)
        return image
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

    private func clusterImage(count: Int) -> UIImage {
        let label = count > 99 ? "99+" : String(count)
        let key = "cluster-\(label)" as NSString
        if let cached = images.object(forKey: key) { return cached }
        let size: CGFloat = count >= 100 ? 60 : (count >= 10 ? 55 : 49)
        let side = size + 24
        let image = UIGraphicsImageRenderer(size: CGSize(width: side, height: side)).image { context in
            let cg = context.cgContext
            cg.setShadow(offset: CGSize(width: 0, height: 2), blur: 10, color: UIColor.black.withAlphaComponent(0.35).cgColor)
            UIColor(hex: "#EAB308").withAlphaComponent(0.92).setFill()
            cg.fillEllipse(in: CGRect(x: 12, y: 12, width: size, height: size))
            cg.setShadow(offset: .zero, blur: 0, color: nil)
            UIColor.white.setStroke()
            cg.setLineWidth(2)
            cg.strokeEllipse(in: CGRect(x: 13, y: 13, width: size - 2, height: size - 2))
            let attributes: [NSAttributedString.Key: Any] = [.font: UIFont.boldSystemFont(ofSize: 13), .foregroundColor: UIColor(hex: "#1A1412")]
            let text = label as NSString
            let measured = text.size(withAttributes: attributes)
            text.draw(at: CGPoint(x: (side - measured.width) / 2, y: (side - measured.height) / 2), withAttributes: attributes)
        }
        images.setObject(image, forKey: key)
        return image
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
    var count = 0
    var highlighted = false

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
    private let picture = UIImageView()
    private let pulse = CAShapeLayer()
    private var highlighted = false

    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        bounds = CGRect(x: 0, y: 0, width: 44, height: 44)
        scalesWithViewingDistance = false
        layer.addSublayer(pulse)
        addSubview(picture)
        isAccessibilityElement = true
        accessibilityTraits = .button
        NotificationCenter.default.addObserver(self, selector: #selector(updatePulse), name: UIAccessibility.reduceMotionStatusDidChangeNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    deinit { NotificationCenter.default.removeObserver(self) }

    override func prepareForReuse() {
        super.prepareForReuse()
        highlighted = false
        picture.layer.removeAllAnimations()
        picture.transform = .identity
        pulse.removeAllAnimations()
        pulse.isHidden = true
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
