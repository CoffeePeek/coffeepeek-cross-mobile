import ComposeApp
import Photos
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct ContributionPhotoPicker: UIViewControllerRepresentable {
    let request: ContributionMediaRequest
    let onSelection: ([NSItemProvider]) -> Void
    let onCamera: (UIImage?) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onSelection: onSelection, onCamera: onCamera) }

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

    @MainActor
    final class Coordinator: NSObject, PHPickerViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onSelection: ([NSItemProvider]) -> Void
        let onCamera: (UIImage?) -> Void
        private var finished = false
        init(onSelection: @escaping ([NSItemProvider]) -> Void, onCamera: @escaping (UIImage?) -> Void) {
            self.onSelection = onSelection
            self.onCamera = onCamera
        }
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard !finished else { return }
            finished = true
            onSelection(results.map(\.itemProvider))
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



@MainActor
struct ContributionPhotoImport: Identifiable {
    let id = UUID()
    let providers: [NSItemProvider]
    let request: ContributionMediaRequest

    func load() async -> [Data] {
        var result: [Data] = []
        for provider in providers {
            guard !Task.isCancelled else { return [] }
            if let data = await load(provider) { result.append(data) }
        }
        return Task.isCancelled ? [] : result
    }

    private func load(_ provider: NSItemProvider) async -> Data? {
        let type = provider.registeredTypeIdentifiers.first {
            UTType($0)?.conforms(to: .image) == true
        } ?? UTType.image.identifier
        let response = AsyncStream<Data?>.makeStream(bufferingPolicy: .bufferingNewest(1))
        let progress = provider.loadDataRepresentation(forTypeIdentifier: type) { data, _ in
            response.continuation.yield(data)
            response.continuation.finish()
        }
        defer { progress.cancel(); response.continuation.finish() }
        for await data in response.stream { return Task.isCancelled ? nil : data }
        return nil
    }
}
