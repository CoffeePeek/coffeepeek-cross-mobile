import ComposeApp
import SwiftUI
import UIKit

@MainActor
private final class ShelfNativeCameraModel: ObservableObject {
    let screen: IosShelfScreen
    @Published var camera: IosShelfNativeCamera?
    @Published var state: IosShelfCameraState?
    private var observation: IosShelfObservation?
    init(screen: IosShelfScreen) { self.screen = screen }
    func start() {
        guard camera == nil else { return }
        camera = screen.startCamera()
        state = camera?.current()
        observation = camera?.observe { [weak self] in self?.state = $0 }
    }
    func stop() { observation?.cancel(); observation = nil; screen.stopCamera(); camera = nil; state = nil }
    isolated deinit { observation?.cancel(); camera?.close() }
}

struct ShelfNativeCameraScreen: View {
    @ObservedObject var model: ShelfNativeModel
    @StateObject private var cameraModel: ShelfNativeCameraModel
    @Environment(\.scenePhase) private var phase
    private var strings: ShelfNativeStrings { model.strings }
    init(model: ShelfNativeModel) {
        self.model = model
        _cameraModel = StateObject(wrappedValue: ShelfNativeCameraModel(screen: model.screen))
    }
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { model.action("closeCamera") } label: {
                    Label(strings["shelf_add_close"], systemImage: "xmark").labelStyle(.iconOnly).frame(width: 44, height: 44)
                }
                Spacer()
                if cameraModel.state?.torchAvailable == true {
                    Button { cameraModel.camera?.torch() } label: {
                        Label(strings["shelf_camera_flash"], systemImage: cameraModel.state?.torch == true ? "bolt.fill" : "bolt.slash").labelStyle(.iconOnly).frame(width: 44, height: 44)
                    }
                }
            }.padding(.horizontal)
            GeometryReader { geometry in
                ZStack {
                    if let camera = cameraModel.camera { ShelfNativeViewfinder(camera: camera) }
                    RoundedRectangle(cornerRadius: 12).stroke(.white.opacity(0.8), style: StrokeStyle(lineWidth: 2, dash: [12]))
                        .padding(.horizontal, geometry.size.width * 0.15).padding(.vertical, geometry.size.height * 0.1)
                    if let state = cameraModel.state, let x = state.focusX, let y = state.focusY {
                        RoundedRectangle(cornerRadius: 4).stroke(state.sharp?.boolValue == false ? .white : .yellow, lineWidth: 2)
                            .frame(width: 56, height: 56).position(x: CGFloat(x.floatValue) * geometry.size.width, y: CGFloat(y.floatValue) * geometry.size.height)
                    }
                    if cameraModel.state?.status != "Ready" {
                        ShelfNativeCameraStatus(status: cameraModel.state?.status, strings: strings, allow: { cameraModel.camera?.allow() })
                    }
                    if cameraModel.state?.capturing == true { CoffeePeekNativeLoader(color: .white, size: 24) }
                }
                .contentShape(Rectangle())
                .onTapGesture(coordinateSpace: .local) { point in
                    guard geometry.size.width > 0, geometry.size.height > 0 else { return }
                    cameraModel.camera?.focus(x: Float(point.x / geometry.size.width), y: Float(point.y / geometry.size.height))
                }
            }
            Text(strings["shelf_scan_tip_whole"]).font(.footnote).multilineTextAlignment(.center).padding()
            HStack(spacing: 40) {
                Button { model.screen.gallery() } label: { Label(strings["shelf_scan_gallery"], systemImage: "photo.on.rectangle").labelStyle(.iconOnly).frame(width: 48, height: 48) }
                    .disabled(cameraModel.state?.capturing == true)
                Button { cameraModel.camera?.capture() } label: {
                    Circle().fill(.white).padding(5).overlay(Circle().stroke(.white, lineWidth: 3)).frame(width: 68, height: 68)
                }.accessibilityLabel(strings["shelf_camera_shutter"]).disabled(cameraModel.state?.status != "Ready" || cameraModel.state?.capturing == true)
                Color.clear.frame(width: 48, height: 48)
            }.padding(.bottom, 20)
        }
        .foregroundStyle(.white).tint(.white).background(.black)
        .onAppear(perform: cameraModel.start).onDisappear(perform: cameraModel.stop)
        .onChange(of: phase) { _, value in if value == .active { cameraModel.start() } else { cameraModel.stop() } }
    }
}

private struct ShelfNativeCameraStatus: View {
    let status: String?
    let strings: ShelfNativeStrings
    let allow: () -> Void
    @ViewBuilder var body: some View {
        if status == "Denied" {
            VStack(spacing: 12) {
                Text(strings["shelf_camera_denied"]).font(.headline)
                Text(strings["shelf_camera_denied_body"])
                Button(strings["shelf_camera_allow"], action: allow).buttonStyle(.bordered)
            }.padding().multilineTextAlignment(.center).background(.black.opacity(0.8))
        } else if status == "Unavailable" { Text(strings["shelf_camera_unavailable"]).padding() }
        else { CoffeePeekNativeLoader(color: .white, size: 24) }
    }
}

private struct ShelfNativeViewfinder: UIViewRepresentable {
    let camera: IosShelfNativeCamera
    func makeUIView(context: Context) -> UIView { camera.view() }
    func updateUIView(_ uiView: UIView, context: Context) {}
}

#Preview("Camera permission light") {
    ShelfNativeCameraStatus(status: "Denied", strings: ShelfNativeStrings("ru"), allow: {})
        .foregroundStyle(.white).background(.black).preferredColorScheme(.light)
}
#Preview("Camera permission dark") {
    ShelfNativeCameraStatus(status: "Denied", strings: ShelfNativeStrings("ru"), allow: {})
        .foregroundStyle(.white).background(.black).preferredColorScheme(.dark)
}
