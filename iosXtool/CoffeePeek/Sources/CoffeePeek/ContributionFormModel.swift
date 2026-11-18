import ComposeApp
import SwiftUI
import UIKit

struct ContributionFormSnapshot: Decodable {
    var kind = ""
    var accountVersion = 0
    var signedIn = -1
    var loading = true
    var loadError: String?
    var loadMessage: String?
    var submit = "idle"
    var submitMessage: String?
    var step = 0
    var canSubmit = false
    var title = ""
    var section: String?
    var requestId: String?
    var resultStatus: String?
    var editing = false
    var hasEdits = false
    var addressValidated: Bool?
    var fields: [ContributionInput] = []
    var groups: [ContributionOptionGroup] = []
    var sections: [String] = []
    var photos: [ContributionFormPhoto] = []
    var menu: [ContributionMenuInput] = []
    var photoLimit = 0
    var menuPhotoLimit = 0
    var perDay = false
    var open = ""
    var close = ""
    var days: [ContributionDayInput] = []
    var closedDays: [Int] = []
    var locationHint: String?
    var resolvingAddress = false
    var map: ContributionMapSnapshot?

    static func decode(_ json: String) -> Self {
        guard let data = json.data(using: .utf8), let state = try? JSONDecoder().decode(Self.self, from: data) else {
            var failed = Self()
            failed.loading = false
            failed.loadError = "failed"
            return failed
        }
        return state
    }

    func field(_ key: String) -> ContributionInput { fields.first { $0.key == key } ?? ContributionInput(key: key, value: "", issues: []) }
    func group(_ key: String) -> [ContributionOption] { groups.first { $0.key == key }?.options ?? [] }
}

struct ContributionInput: Decodable { let key: String; let value: String; let issues: [String] }
struct ContributionOptionGroup: Decodable { let key: String; let options: [ContributionOption] }
struct ContributionOption: Decodable, Identifiable { let id: String; let name: String; let selected: Bool; let photoUrl: String? }
struct ContributionFormPhoto: Decodable, Identifiable {
    let key: String
    let url: String?
    let fileName: String
    let menu: Bool
    let groupedRemoval: Bool
    var id: String { key }
}
struct ContributionMenuInput: Decodable, Identifiable {
    let slug: String
    let name: String
    let availability: String
    let price: String
    var id: String { slug }
}
struct ContributionDayInput: Decodable, Identifiable {
    let day: Int
    let closed: Bool
    let open: String
    let close: String
    var id: Int { day }
}
struct ContributionMapSnapshot: Decodable {
    let latitude: Double?
    let longitude: Double?
    let cameraLatitude: Double?
    let cameraLongitude: Double?
    let address: String?
    let resolving: Bool
    let failed: Bool
    let canConfirm: Bool
}

struct ContributionMediaRequest: Identifiable {
    let revision: Int32
    let menu: Bool
    let camera: Bool
    let limit: Int
    let version: Int
    var id: Int32 { revision }
}

@MainActor
final class ContributionFormModel: ObservableObject {
    let form: IosContributionForm
    let strings: ContributionsStrings
    @Published var state: ContributionFormSnapshot
    @Published var sourceMenu: Bool?
    @Published var mediaRequest: ContributionMediaRequest?
    @Published var preparingPhotos = false
    @Published var photoFailed = false
    let permission = ContributionLocationPermission()
    private var observation: IosContributionObservation?
    private var mediaRevision: Int32?
    private var initialLocationVersion: Int?
    private var lastOutcome: String?

    init(form: IosContributionForm, strings: ContributionsStrings) {
        self.form = form
        self.strings = strings
        state = ContributionFormSnapshot.decode(form.current())
    }

    // The UIKit destination retains this model through map/photo covers.
    // Observation ends with the model, not a temporary disappearance of its form view.
    func appear() {
        guard observation == nil else { return }
        update()
        observation = form.observe { [weak self] in self?.update() }
    }

    private func update() {
        let next = ContributionFormSnapshot.decode(form.current())
        if state.accountVersion != next.accountVersion {
            clearMediaRequest()
        }
        state = next
        if next.kind == "AddShop", next.signedIn == 1, initialLocationVersion != next.accountVersion {
            initialLocationVersion = next.accountVersion
            requestLocation(map: false)
        }
        let outcome = "\(next.accountVersion):\(next.submit)"
        if outcome != lastOutcome && ["done", "failed"].contains(next.submit) {
            form.feedback(success: next.submit == "done")
        }
        lastOutcome = outcome
    }

    private func clearMediaRequest() {
        sourceMenu = nil
        mediaRequest = nil
        mediaRevision = nil
        preparingPhotos = false
        photoFailed = false
    }

    func requestPhoto(menu: Bool) { sourceMenu = menu }

    func selectPhotoSource(camera: Bool) {
        guard let menu = sourceMenu else { return }
        sourceMenu = nil
        let revision = form.beginPhotoPick(menu: menu)
        guard revision >= 0 else { return }
        let limit = (menu ? state.menuPhotoLimit : state.photoLimit) - state.photos.filter { $0.menu == menu }.count
        mediaRevision = revision
        mediaRequest = ContributionMediaRequest(revision: revision, menu: menu, camera: camera,
            limit: camera ? 1 : max(1, limit), version: state.accountVersion)
    }

    func picked(_ data: [Data], request: ContributionMediaRequest) {
        guard mediaRevision == request.revision, state.accountVersion == request.version else { return }
        mediaRequest = nil
        preparingPhotos = !data.isEmpty
        form.photosPicked(revision: request.revision, data: data) { [weak self] in
            guard let self, mediaRevision == request.revision, state.accountVersion == request.version else { return }
            preparingPhotos = false
            mediaRevision = nil
        }
    }

    func requestLocation(map: Bool) {
        let version = state.accountVersion
        permission.request { [weak self] granted in
            guard let self, state.accountVersion == version else { return }
            if granted { form.locationGranted(map: map) } else { form.locationDenied() }
        }
    }

    func pickedCamera(_ image: UIImage?, request: ContributionMediaRequest) {
        guard let image else { picked([], request: request); return }
        guard mediaRevision == request.revision, state.accountVersion == request.version else { return }
        mediaRequest = nil
        preparingPhotos = true
        form.cameraPicked(revision: request.revision, image: image) { [weak self] in
            guard let self, mediaRevision == request.revision, state.accountVersion == request.version else { return }
            preparingPhotos = false
            mediaRevision = nil
        }
    }

    func startedImport(request: ContributionMediaRequest) {
        guard mediaRevision == request.revision, state.accountVersion == request.version else { return }
        mediaRequest = nil
        preparingPhotos = true
    }

    deinit { observation?.cancel() }
}

