import ComposeApp
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct ContributionPhotoPicker: UIViewControllerRepresentable {
    let request: ContributionMediaRequest
    let onFinish: ([Data]) -> Void
    let onCamera: (UIImage?) -> Void
    let onStart: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish, onCamera: onCamera, onStart: onStart) }

    func makeUIViewController(context: Context) -> UIViewController {
        if request.camera {
            let picker = UIImagePickerController()
            picker.sourceType = .camera
            picker.delegate = context.coordinator
            return picker
        }
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = request.limit
        configuration.preferredAssetRepresentationMode = .current
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    final class Coordinator: NSObject, PHPickerViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onFinish: ([Data]) -> Void
        let onCamera: (UIImage?) -> Void
        let onStart: () -> Void
        private var finished = false
        init(onFinish: @escaping ([Data]) -> Void, onCamera: @escaping (UIImage?) -> Void, onStart: @escaping () -> Void) {
            self.onFinish = onFinish
            self.onCamera = onCamera
            self.onStart = onStart
        }
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard !finished else { return }
            finished = true
            guard !results.isEmpty else { onFinish([]); return }
            onStart()
            let group = DispatchGroup()
            let lock = NSLock()
            var values = [Data?](repeating: nil, count: results.count)
            for (index, result) in results.enumerated() {
                group.enter()
                result.itemProvider.loadDataRepresentation(forTypeIdentifier: UTType.image.identifier) { data, _ in
                    lock.lock(); values[index] = data; lock.unlock()
                    group.leave()
                }
            }
            group.notify(queue: .main) { self.onFinish(values.compactMap { $0 }) }
        }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            guard !finished else { return }
            finished = true
            onCamera(info[.originalImage] as? UIImage)
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            guard !finished else { return }
            finished = true
            onCamera(nil)
        }
    }
}

struct ContributionFormPhotoView: View {
    let form: IosContributionForm
    let photo: ContributionFormPhoto
    let version: Int
    @State private var image: UIImage?
    @State private var observation: IosContributionObservation?

    var body: some View {
        ZStack {
            CoffeePeekPhotoPlaceholder()
            if let image { Image(uiImage: image).resizable().scaledToFill() }
        }
        .accessibilityHidden(true)
        .task(id: "\(version):\(photo.key)") {
            observation?.cancel(); image = nil
            if let url = photo.url {
                observation = form.loadImage(url: url, version: Int32(version)) { image = $0 }
            } else {
                observation = form.loadPhoto(key: photo.key, version: Int32(version)) { data in
                    image = data.flatMap { coffeePeekNativeThumbnail($0 as Data) }
                }
            }
        }.onDisappear { observation?.cancel(); observation = nil; image = nil }
    }
}

struct ContributionRemoteImage: View {
    let form: IosContributionForm
    let url: String
    let version: Int
    @State private var image: UIImage?
    @State private var observation: IosContributionObservation?
    var body: some View {
        ZStack {
            CoffeePeekPhotoPlaceholder()
            if let image { Image(uiImage: image).resizable().scaledToFit() }
        }.accessibilityHidden(true)
            .task(id: "\(version):\(url)") {
                observation?.cancel(); image = nil
                observation = form.loadImage(url: url, version: Int32(version)) { image = $0 }
            }.onDisappear { observation?.cancel(); observation = nil; image = nil }
    }
}

