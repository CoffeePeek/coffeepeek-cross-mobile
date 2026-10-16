import ComposeApp
import CoreGraphics
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
        return observation.allInstances.compactMap { instance -> Data? in
            guard let buffer = try? observation.generateMaskedImage(
                ofInstances: IndexSet(integer: instance), from: handler, croppedToInstancesExtent: true
            ) else { return nil }
            let masked = CIImage(cvPixelBuffer: buffer)
            guard let cgImage = context.createCGImage(masked, from: masked.extent) else { return nil }
            let scale = min(1, 768 / CGFloat(max(cgImage.width, cgImage.height)))
            let size = CGSize(width: CGFloat(cgImage.width) * scale, height: CGFloat(cgImage.height) * scale)
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            format.opaque = false
            let rendered = UIGraphicsImageRenderer(size: size, format: format).image { _ in
                UIImage(cgImage: cgImage).draw(in: CGRect(origin: .zero, size: size))
            }
            guard let renderedImage = rendered.cgImage,
                  isValidCutout(renderedImage),
                  let png = rendered.pngData(),
                  !png.isEmpty, png.count <= 4_194_304 else { return nil }
            return png
        }
    }

    private func isValidCutout(_ image: CGImage) -> Bool {
        guard (1...768).contains(image.width), (1...768).contains(image.height) else { return false }
        let bytesPerRow = image.width * 4
        var pixels = [UInt8](repeating: 0, count: bytesPerRow * image.height)
        return pixels.withUnsafeMutableBytes { bytes in
            // RGBA byte order makes the final component alpha on every device architecture.
            guard let context = CGContext(
                data: bytes.baseAddress, width: image.width, height: image.height,
                bitsPerComponent: 8, bytesPerRow: bytesPerRow, space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
            ) else { return false }
            context.draw(image, in: CGRect(x: 0, y: 0, width: CGFloat(image.width), height: CGFloat(image.height)))
            var hasTransparency = false
            var hasVisiblePixels = false
            for offset in stride(from: 3, to: bytes.count, by: 4) {
                hasTransparency = hasTransparency || bytes[offset] < 255
                hasVisiblePixels = hasVisiblePixels || bytes[offset] > 0
                if hasTransparency && hasVisiblePixels { return true }
            }
            return false
        }
    }
}

private enum CutoutError: Error { case invalidImage }
