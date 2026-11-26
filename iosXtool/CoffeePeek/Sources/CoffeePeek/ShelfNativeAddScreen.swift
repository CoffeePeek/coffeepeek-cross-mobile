import ComposeApp
import SwiftUI

struct ShelfNativeAddScreen: View {
    @ObservedObject var model: ShelfNativeModel
    private var strings: ShelfNativeStrings { model.strings }
    private var add: IosShelfAddState? { model.state.add }

    var body: some View {
        Group {
            if let add, add.step == "Camera" { ShelfNativeCameraScreen(model: model) }
            else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        if let add { content(add) }
                        if let message = add?.message {
                            ShelfNativeStatus(message: message == "PhotoUnreadable" ? strings["shelf_scan_failed_photo"] : strings.issue(message),
                                retry: strings["shelf_sheet_close"]) { model.action("dismiss") }
                        }
                    }.padding(16)
                }
                .safeAreaInset(edge: .top) {
                    ShelfNativeHeader(title: strings["shelf_add_title"], back: strings["shelf_add_close"]) { model.action("close") }
                }
            }
        }
        .alert(strings["shelf_duplicate_title"], isPresented: Binding(get: { add?.duplicate == true }, set: { if !$0 { model.action("cancelDuplicate") } })) {
            Button(strings["shelf_duplicate_confirm"]) { model.action("duplicate") }
            Button(strings["shelf_cancel"], role: .cancel) { model.action("cancelDuplicate") }
        } message: { Text(strings["shelf_duplicate_message"]) }
    }

    @ViewBuilder private func content(_ add: IosShelfAddState) -> some View {
        switch add.step {
        case "Form": ShelfNativeBagForm(add: add, strings: strings, card: model.screen.photo(card: true), send: model.send)
        case "Preparing": preparation(add)
        case "Scanning", "Revealing": progress(add)
        case "Retake": retake(add)
        default: intro(add)
        }
    }

    private func intro(_ add: IosShelfAddState) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            Image(systemName: "camera.viewfinder").font(.system(size: 72)).frame(maxWidth: .infinity).padding(24).accessibilityHidden(true)
            Text(strings["shelf_scan_intro_title"]).font(.title2.weight(.semibold))
            Text(strings["shelf_scan_intro_body"]).foregroundStyle(CoffeePeekNativeStyle.secondary)
            tips
            if add.scanner == "Disabled" { Text(strings["shelf_scanner_disabled"]) }
            photoActions.disabled(add.scanner == "Disabled")
        }
    }

    private var tips: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(["whole", "straight", "clean"], id: \.self) { key in
                Label(strings["shelf_scan_tip_\(key)"], systemImage: "checkmark")
            }
        }
    }
    private var photoActions: some View {
        VStack(spacing: 12) {
            Button(strings["shelf_scan_camera"]) { model.action("camera") }.buttonStyle(.borderedProminent).foregroundStyle(.black)
            Button(strings["shelf_scan_gallery"]) { model.screen.gallery() }.buttonStyle(.bordered)
        }.frame(maxWidth: .infinity)
    }

    private func preparation(_ add: IosShelfAddState) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            photo(add)
            Text(strings["shelf_prepare_title"]).font(.title2.weight(.semibold))
            Text(strings["shelf_prepare_body"])
            if add.scanner == "Checking" { CoffeePeekNativeLoader(label: strings["shelf_scanner_checking"], size: 32, showsLabel: true) }
            else if add.scanner == "Downloading" {
                if add.downloadTotal > 0 {
                    VStack(spacing: 8) {
                        CoffeePeekNativeLoader(label: strings["shelf_scanner_downloading_unknown"], size: 32)
                        Text(min(1, max(0, Double(add.downloadDone) / Double(add.downloadTotal))), format: .percent)
                            .monospacedDigit()
                    }
                } else { CoffeePeekNativeLoader(label: strings["shelf_scanner_downloading_unknown"], size: 32, showsLabel: true) }
            } else {
                if add.scanner == "DownloadFailed" { Text(strings["shelf_scanner_download_failed"]) }
                Button(add.downloadBytes > 0 ? String(format: strings["shelf_scanner_prepare"], Int((add.downloadBytes + 1_048_575) / 1_048_576)) : strings["shelf_scanner_download"]) {
                    model.action("download")
                }.buttonStyle(.borderedProminent).foregroundStyle(.black)
            }
            photoActions
        }
    }

    private func progress(_ add: IosShelfAddState) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            photo(add)
            ForEach(add.stages, id: \.name) { stage in
                HStack {
                    Image(systemName: stage.status == "Done" ? "checkmark.circle.fill" : "circle")
                    Text(stageText(stage.name))
                }.foregroundStyle(stage.status == "Done" ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.secondary)
            }
            CoffeePeekNativeLoader(label: strings["shelf_scan_title"], size: 24).frame(maxWidth: .infinity)
        }
        .task(id: add.step) {
            if add.step == "Revealing" {
                try? await Task.sleep(for: .milliseconds(350))
                guard !Task.isCancelled else { return }
                model.action("revealed")
            }
        }
    }

    private func retake(_ add: IosShelfAddState) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            photo(add)
            Text(strings["shelf_retake_title"]).font(.title2.weight(.semibold))
            if let failure = add.failure {
                Text(strings[failure == "OutOfMemory" ? "shelf_scan_failed_memory" : failure == "ModelMissing" || failure == "ModelDownload" ? "shelf_scan_failed_model" : "shelf_scan_failed"])
            }
            ForEach(add.stages, id: \.name) { stage in
                VStack(alignment: .leading, spacing: 4) {
                    Label(stageText(stage.name), systemImage: stage.status == "Done" ? "checkmark.circle" : stage.status == "Skipped" ? "minus.circle" : "exclamationmark.circle")
                    ForEach(stage.reasons, id: \.self) { Text(strings.reason($0)).font(.footnote) }
                }
            }
            photoActions
        }
    }
    private func stageText(_ name: String) -> String {
        let keys = ["Segmentation": "found", "Front": "front", "Repair": "repair", "Soften": "soften"]
        return strings["shelf_scan_stage_\(keys[name] ?? "found")"]
    }
    @ViewBuilder private func photo(_ add: IosShelfAddState) -> some View {
        if let image = model.screen.photo(card: false) {
            Image(uiImage: image).resizable().scaledToFit().overlay {
                GeometryReader { geometry in
                    Path { path in
                        guard let first = add.frame.first else { return }
                        path.move(to: CGPoint(x: Double(first.x) * geometry.size.width, y: Double(first.y) * geometry.size.height))
                        for point in add.frame.dropFirst() { path.addLine(to: CGPoint(x: Double(point.x) * geometry.size.width, y: Double(point.y) * geometry.size.height)) }
                        path.closeSubpath()
                    }.stroke(CoffeePeekNativeStyle.accent, lineWidth: 2)
                }
            }.frame(maxHeight: 400).clipShape(RoundedRectangle(cornerRadius: 12)).accessibilityHidden(true)
        }
    }
}
