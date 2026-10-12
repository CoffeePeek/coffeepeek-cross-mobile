import ComposeApp
import CoreImage
import ImageIO
import UIKit
import Vision

final class NativeDrinkPhotoProvider: NSObject, IosDrinkPhotoProvider {
    func extract(photo: Data) -> IosDrinkPhotoResult {
        guard #available(iOS 17.0, *) else {
            return IosDrinkPhotoResult(photos: [], error: "Автоматическое выделение чашки доступно с iOS 17. Можно продолжить без вырезки.")
        }
        return autoreleasepool {
            do {
                return IosDrinkPhotoResult(photos: try extractSubjects(photo), error: nil)
            } catch {
                return IosDrinkPhotoResult(photos: [], error: "Не удалось выделить чашку. Попробуйте другое фото или продолжите без вырезки.")
            }
        }
    }

    @available(iOS 17.0, *)
    private func extractSubjects(_ data: Data) throws -> [Data] {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 1024
              ] as CFDictionary) else {
            throw CutoutError.invalidImage
        }
        let handler = VNImageRequestHandler(cgImage: image, options: [:])
        let request = VNGenerateForegroundInstanceMaskRequest()
        try handler.perform([request])
        guard let observation = request.results?.first else { return [] }
        let context = CIContext()
        return try observation.allInstances.map { instance in
            let buffer = try observation.generateMaskedImage(
                ofInstances: IndexSet(integer: instance), from: handler, croppedToInstancesExtent: true
            )
            let masked = CIImage(cvPixelBuffer: buffer)
            guard let cgImage = context.createCGImage(masked, from: masked.extent) else {
                throw CutoutError.invalidImage
            }
            let scale = min(1, 768 / CGFloat(max(cgImage.width, cgImage.height)))
            let size = CGSize(width: CGFloat(cgImage.width) * scale, height: CGFloat(cgImage.height) * scale)
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            format.opaque = false
            let png = UIGraphicsImageRenderer(size: size, format: format).pngData { _ in
                UIImage(cgImage: cgImage).draw(in: CGRect(origin: .zero, size: size))
            }
            guard !png.isEmpty else { throw CutoutError.invalidImage }
            return png
        }
    }
}

private enum CutoutError: Error { case invalidImage }
