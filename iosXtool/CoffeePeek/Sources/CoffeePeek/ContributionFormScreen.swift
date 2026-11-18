import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeContributionFormController(form: IosContributionForm, strings: ContributionsStrings) -> UIViewController {
    ContributionFormHostingController(form: form, strings: strings)
}

private func contributionFormSample(_ kind: String, step: Int = 0, section: String = "Description") -> some View {
    let fields = [
        ContributionInput(key: "name", value: "Sandbox Cafe", issues: []),
        ContributionInput(key: "description", value: "Кофейня для спокойного утра", issues: []),
        ContributionInput(key: "address", value: "Sandbox street, 1", issues: []),
        ContributionInput(key: "website", value: "coffeepeek.by", issues: [])
    ]
    let groups = [
        ContributionOptionGroup(key: "city", options: [ContributionOption(id: "minsk", name: "Минск", selected: true, photoUrl: nil)]),
        ContributionOptionGroup(key: "brewMethods", options: [ContributionOption(id: "v60", name: "V60", selected: true, photoUrl: nil)])
    ]
    let snapshot = ContributionFormSnapshot(kind: kind, accountVersion: 1, signedIn: 1, loading: false,
        step: step, canSubmit: true, title: "Sandbox Cafe", section: section, fields: fields, groups: groups,
        sections: ["Photos", "Contacts", "Description", "Tags", "Roasters", "Menu", "BrewMethods"],
        photos: [], menu: [ContributionMenuInput(slug: "flat-white", name: "Флэт уайт", availability: "Present", price: "8")],
        photoLimit: 5, menuPhotoLimit: 4, open: "09:00", close: "21:00")
    return ContributionFormContent(state: snapshot, strings: ContributionsStrings(languageTag: "ru"), preparingPhotos: false,
        onBack: {}, onSignIn: {}, onEdit: { _, _ in }, onToggle: { _, _ in }, onAction: { _ in },
        onSchedule: { _, _, _ in }, onMenu: { _, _, _ in }, onAddPhoto: { _ in }, onRemovePhoto: { _ in },
        onSection: { _ in }, onAddRoaster: {}, onOpenRequest: {}) { _ in CoffeePeekPhotoPlaceholder() }
        optionImage: { _ in CoffeePeekPhotoPlaceholder() }
}
#Preview("Add shop light") { contributionFormSample("AddShop").preferredColorScheme(.light) }
#Preview("Add shop dark") { contributionFormSample("AddShop").preferredColorScheme(.dark) }
#Preview("Add roaster light") { contributionFormSample("AddRoaster").preferredColorScheme(.light) }
#Preview("Add roaster dark") { contributionFormSample("AddRoaster").preferredColorScheme(.dark) }
#Preview("Suggest change light") { contributionFormSample("Suggest").preferredColorScheme(.light) }
#Preview("Suggest change dark") { contributionFormSample("Suggest").preferredColorScheme(.dark) }
#Preview("Change editor light") { contributionFormSample("Editor", section: "Menu").preferredColorScheme(.light) }
#Preview("Change editor dark") { contributionFormSample("Editor", section: "Menu").preferredColorScheme(.dark) }

@MainActor
private final class ContributionFormHostingController: UIHostingController<ContributionFormHost> {
    private let form: IosContributionForm
    private let model: ContributionFormModel
    init(form: IosContributionForm, strings: ContributionsStrings) {
        self.form = form
        model = ContributionFormModel(form: form, strings: strings)
        super.init(rootView: ContributionFormHost(model: model))
    }
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("Use init(form:strings:)") }
    deinit { let form = form; DispatchQueue.main.async { form.close() } }
}

private struct ContributionFormHost: View {
    @ObservedObject var model: ContributionFormModel

    var body: some View {
        ContributionFormContent(state: model.state, strings: model.strings, preparingPhotos: model.preparingPhotos,
            onBack: model.form.formBack, onSignIn: model.form.signIn,
            onEdit: { model.form.edit(field: $0, value: $1) }, onToggle: { model.form.toggle(group: $0, id: $1) },
            onAction: { model.form.control(action: $0) }, onSchedule: { model.form.schedule(action: $0, day: Int32($1), value: Int32($2)) },
            onMenu: { model.form.menu(slug: $0, availability: $1, price: $2) },
            onAddPhoto: model.requestPhoto, onRemovePhoto: { model.form.removePhoto(key: $0) },
            onSection: { model.form.selectSection(value: $0) }, onAddRoaster: model.form.addRoaster,
            onOpenRequest: model.form.openRequest) { photo in
                ContributionFormPhotoView(form: model.form, photo: photo, version: model.state.accountVersion)
            } optionImage: { url in
                ContributionRemoteImage(form: model.form, url: url, version: model.state.accountVersion)
            }
        .background(CoffeePeekLocalBackProtection(protected: model.state.submit == "sending" ||
            model.state.submit == "idle" && (model.state.hasEdits ||
                model.state.kind == "AddShop" && model.state.step > 0) || model.state.map != nil))
        .onAppear(perform: model.appear)
        .confirmationDialog(model.strings.media("title"), isPresented: Binding(
            get: { model.sourceMenu != nil }, set: { if !$0 { model.sourceMenu = nil } })) {
            Button(model.strings.media("gallery")) { model.selectPhotoSource(camera: false) }
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                Button(model.strings.media("camera")) { model.selectPhotoSource(camera: true) }
            }
            Button(model.strings.media("cancel"), role: .cancel) { model.sourceMenu = nil }
        }
        .fullScreenCover(item: $model.mediaRequest) { request in
            ContributionPhotoPicker(request: request, onFinish: { model.picked($0, request: request) },
                onCamera: { model.pickedCamera($0, request: request) }, onStart: { model.startedImport(request: request) })
                .ignoresSafeArea()
        }
        .fullScreenCover(isPresented: Binding(get: { model.state.map != nil }, set: {
            if !$0 { model.form.control(action: "dismissMap") }
        })) { ContributionMapPicker(model: model) }
    }
}

struct ContributionFormContent<Photo: View, OptionImage: View>: View {
    let state: ContributionFormSnapshot
    let strings: ContributionsStrings
    let preparingPhotos: Bool
    let onBack: () -> Void
    let onSignIn: () -> Void
    let onEdit: (String, String) -> Void
    let onToggle: (String, String) -> Void
    let onAction: (String) -> Void
    let onSchedule: (String, Int, Int) -> Void
    let onMenu: (String, String?, String?) -> Void
    let onAddPhoto: (Bool) -> Void
    let onRemovePhoto: (String) -> Void
    let onSection: (String) -> Void
    let onAddRoaster: () -> Void
    let onOpenRequest: () -> Void
    @ViewBuilder let photo: (ContributionFormPhoto) -> Photo
    @ViewBuilder let optionImage: (String) -> OptionImage
    @State private var menuExpanded = false

    var body: some View {
        ContributionsScaffold(title: title, backLabel: strings.text("back"), onBack: onBack) {
            if state.signedIn == -1 || state.loading && state.kind != "AddRoaster" {
                if state.loadError != nil { loadFailure } else { ProgressView(strings.text("loading")) }
            } else if state.signedIn == 0 {
                ContributionsMessage(text: strings.text(signInKey), actionTitle: strings.text("sign_in"), onAction: onSignIn)
            } else if state.submit != "idle" { submission }
            else if state.loadError != nil && state.kind != "AddRoaster" { loadFailure }
            else { form }
        }
        .font(CoffeePeekNativeStyle.font(16))
        .tint(CoffeePeekNativeStyle.foreground)
    }

    private var title: String {
        switch state.kind {
        case "AddShop": strings.text(["shop_step_basic", "shop_step_contacts", "shop_step_photos", "shop_step_features", "shop_step_schedule"][min(4, max(0, state.step))])
        case "AddRoaster": strings.text("roaster_title")
        case "Suggest": strings.text("suggest_title")
        default: strings.sectionTitle(state.section ?? "Description")
        }
    }

    private var signInKey: String {
        switch state.kind {
        case "AddShop": "shop_sign_in_prompt"
        case "AddRoaster": "roaster_sign_in_prompt"
        case "Suggest": "suggest_sign_in_prompt"
        default: "editor_sign_in_prompt"
        }
    }

    private var form: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if state.kind == "AddShop" { stepProgress }
                if state.kind == "AddRoaster" && state.loadError != nil {
                    Text(strings.text("load_failed")).foregroundStyle(.red)
                    Button(strings.text("retry")) { onAction("retry") }
                }
                if state.kind == "Suggest" { sections }
                else if state.kind == "AddShop" { shopStep }
                else if state.kind == "AddRoaster" { roasterFields }
                else { editorFields }
            }.padding(16)
        }
        .safeAreaInset(edge: .bottom) {
            if state.kind != "Suggest" {
                CoffeePeekNativeActionButton(title: submitLabel, enabled: state.canSubmit && !preparingPhotos,
                    action: { onAction(state.kind == "AddShop" ? "next" : "submit") }).padding(16).background(.bar)
            }
        }
    }

    private var stepProgress: some View {
        HStack(spacing: 8) {
            ForEach(0..<5) { index in
                Capsule().fill(index <= state.step ? CoffeePeekNativeStyle.accent : CoffeePeekNativeStyle.border).frame(height: 4)
            }
        }.accessibilityLabel(title)
    }

    private var submitLabel: String {
        if state.kind == "AddShop" && state.step < 4 { return strings.text("shop_next") }
        return strings.text(state.kind == "AddRoaster" ? "roaster_submit" : state.kind == "Editor" && state.editing ? "editor_save" : "shop_submit")
    }

    @ViewBuilder private var shopStep: some View {
        switch state.step {
        case 0:
            Text(strings.text("shop_required_hint")).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            field("name")
            field("description", multiline: true)
            city
            HStack(alignment: .top) {
                field("address")
                Button { onAction("openMap") } label: {
                    Image(systemName: "map").frame(width: 48, height: 48)
                        .background(CoffeePeekNativeStyle.accent, in: Circle()).foregroundStyle(.black)
                }.accessibilityLabel(strings.text("shop_pick_on_map")).disabled(state.resolvingAddress)
            }
            if let hint = state.locationHint { Text(strings.locationHint(hint)).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            price
        case 1:
            Text(strings.text("shop_contacts_intro")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            contacts
        case 2:
            photos(menu: false)
            photos(menu: true)
        case 3:
            Text(strings.text("shop_features_intro")).foregroundStyle(CoffeePeekNativeStyle.secondary)
            choices("brewMethods")
            choices("beans")
            choices("roasters")
            Button(strings.text("shop_add_roaster"), action: onAddRoaster)
            choices("equipment")
        default:
            ContributionScheduleContent(state: state, strings: strings, onAction: onSchedule)
        }
    }

    private var roasterFields: some View {
        Group {
            Text(strings.text("shop_required_hint")).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            field("name")
            field("description", multiline: true)
            if state.loading { ProgressView(strings.text("loading")) }
            city
            field("address")
            Text(strings.text("roaster_location_hint")).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            field("instagram")
            field("website")
            photos(menu: false)
        }
    }

    @ViewBuilder private var editorFields: some View {
        if !state.title.isEmpty { Text(state.title).foregroundStyle(CoffeePeekNativeStyle.secondary) }
        switch state.section {
        case "Description": field("description", multiline: true)
        case "Contacts": contacts
        case "Photos": photos(menu: false)
        case "Menu":
            photos(menu: true)
            DisclosureGroup(strings.text("editor_expand_menu"), isExpanded: $menuExpanded) {
                if state.menu.isEmpty { Text(strings.text("editor_drinks_empty")) }
                ForEach(state.menu) { row in menuRow(row) }
            }
        default:
            ForEach(state.groups, id: \.key) { group in choices(group.key) }
        }
    }

    private var contacts: some View {
        Group { field("phone"); field("instagram"); field("website"); field("email") }
    }

    private func field(_ key: String, multiline: Bool = false) -> some View {
        let value = state.field(key)
        return VStack(alignment: .leading, spacing: 6) {
            Text(strings.fieldTitle(key, roaster: state.kind == "AddRoaster") +
                ((key == "name" || state.kind == "AddShop" && key == "address") ? " *" : ""))
                .font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            TextField(strings.fieldTitle(key, roaster: state.kind == "AddRoaster"),
                text: Binding(get: { value.value }, set: { onEdit(key, $0) }), axis: multiline ? .vertical : .horizontal)
                .lineLimit(multiline ? 3...6 : 1...1).textFieldStyle(.roundedBorder)
                .keyboardType(key == "phone" ? .phonePad : key == "email" ? .emailAddress : ["website", "instagram"].contains(key) ? .URL : .default)
                .textInputAutocapitalization(["phone", "email", "website", "instagram"].contains(key) ? .never : .sentences)
                .autocorrectionDisabled(["phone", "email", "website", "instagram"].contains(key))
            if key == "name" { Text("\(value.value.utf16.count)/\(state.kind == "AddRoaster" ? 100 : 55)").font(.caption2).foregroundStyle(CoffeePeekNativeStyle.secondary) }
            if !value.value.isEmpty || state.kind == "AddRoaster" && value.issues.contains("AddressMissing"), let issue = value.issues.first { Text(strings.issue(issue, roaster: state.kind == "AddRoaster")).font(.caption).foregroundStyle(.red) }
        }
    }

    private var city: some View {
        Picker(strings.text("shop_city_label") + (state.kind == "AddShop" ? " *" : ""), selection: Binding(
            get: { state.group("city").first(where: \.selected)?.id ?? "" },
            set: { if $0.isEmpty { onEdit("city", "") } else { onToggle("city", $0) } })) {
                Text(strings.text("shop_city_label")).tag("")
                ForEach(state.group("city")) { Text($0.name).tag($0.id) }
            }.disabled(state.group("city").count == 1 && state.kind == "AddShop")
    }

    private var price: some View {
        Picker(strings.text("price_title"), selection: Binding(
            get: { state.group("price").first(where: \.selected)?.id ?? "" },
            set: { onEdit("price", $0) })) {
                Text(strings.text("price_title")).tag("")
                ForEach(state.group("price")) { Text($0.name).tag($0.id) }
            }
    }

    private func choices(_ key: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if !state.group(key).isEmpty {
                Text(strings.groupTitle(key)).font(.headline)
                ForEach(state.group(key)) { option in
                    Button { onToggle(key, option.id) } label: {
                        HStack(spacing: 12) {
                            if let url = option.photoUrl { optionImage(url).frame(width: 36, height: 36).clipped().clipShape(Circle()) }
                            Text(option.name).frame(maxWidth: .infinity, alignment: .leading)
                            if option.selected { Image(systemName: "checkmark").foregroundStyle(CoffeePeekNativeStyle.accent) }
                        }.frame(minHeight: 44)
                    }.buttonStyle(.plain).accessibilityAddTraits(option.selected ? .isSelected : [])
                }
            }
        }
    }

    private var sections: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(state.title.isEmpty ? strings.text("suggest_heading") : strings.format("suggest_heading_shop", state.title))
                .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
            ForEach(state.sections, id: \.self) { section in
                Button { onSection(section) } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(strings.sectionTitle(section))
                            Text(strings.sectionHint(section)).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
                        }
                        Spacer(); Image(systemName: "chevron.forward")
                    }.frame(minHeight: 56)
                }.buttonStyle(.plain)
                Divider()
            }
        }
    }

    private func photos(menu: Bool) -> some View {
        let all = state.photos.filter { $0.menu == menu }
        let uploaded = all.filter(\.groupedRemoval)
        let regular = all.filter { !$0.groupedRemoval }
        let limit = menu ? state.menuPhotoLimit : state.photoLimit
        return VStack(alignment: .leading, spacing: 12) {
            Text(strings.text(menu ? "shop_menu_photos_title" : state.kind == "AddRoaster" ? "roaster_photos_title" : "shop_photos_title")).font(.headline)
            Text(strings.format("photo_count", all.count, limit)).font(.caption).foregroundStyle(CoffeePeekNativeStyle.secondary)
            if !uploaded.isEmpty {
                HStack {
                    Text(strings.format("editor_already_uploaded", uploaded.count)).font(.caption)
                    Spacer()
                    Button(strings.text("editor_remove_uploaded")) { onRemovePhoto(uploaded[0].key) }
                }
            }
            ScrollView(.horizontal) {
                LazyHStack(spacing: 12) {
                    ForEach(regular) { value in
                        photo(value).frame(width: 112, height: 112).clipped().clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(alignment: .topTrailing) {
                                Button { onRemovePhoto(value.key) } label: {
                                    Image(systemName: "xmark.circle.fill").font(.title2).symbolRenderingMode(.palette)
                                        .foregroundStyle(.white, .black.opacity(0.75)).frame(width: 44, height: 44)
                                }.accessibilityLabel(strings.text("editor_remove"))
                            }
                    }
                    if all.count < limit {
                        Button { onAddPhoto(menu) } label: {
                            if preparingPhotos { ProgressView() }
                            else { Image(systemName: "plus").font(.title) }
                        }.frame(width: 112, height: 112).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 14))
                            .accessibilityLabel(strings.media("title")).disabled(preparingPhotos)
                    }
                }
            }
        }
    }

    private func menuRow(_ row: ContributionMenuInput) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(row.name).font(.headline)
            Picker(strings.text("editor_menu_details"), selection: Binding(get: { row.availability }, set: { onMenu(row.slug, $0, nil) })) {
                ForEach(["Unknown", "Present", "Absent"], id: \.self) { Text(strings.text("availability_\($0.lowercased())")).tag($0) }
            }.pickerStyle(.segmented)
            TextField(strings.text("editor_price_placeholder"), text: Binding(get: { row.price }, set: { onMenu(row.slug, nil, $0) }))
                .keyboardType(.decimalPad).textFieldStyle(.roundedBorder)
        }.padding(.vertical, 8)
    }

    private var loadFailure: some View {
        VStack {
            if state.loadError == "NotFound" {
                Text(state.loadMessage ?? strings.text("editor_not_found")).padding(24)
            } else {
                ContributionsMessage(text: state.loadMessage ?? strings.text(state.kind == "AddShop" ? "shop_catalogs_failed" : "load_failed"),
                    actionTitle: strings.text(state.loadError == "SignInRequired" ? "sign_in" : "retry"),
                    onAction: { if state.loadError == "SignInRequired" { onSignIn() } else { onAction("retry") } })
            }
        }
    }

    private var submission: some View {
        VStack(spacing: 20) {
            if state.submit == "sending" {
                ProgressView(strings.text(state.kind == "AddShop" ? "shop_sending_title" : state.kind == "AddRoaster" ? "roaster_sending_title" : state.editing ? "editor_saving_title" : "editor_sending_title"))
            } else if state.submit == "failed" {
                Image(systemName: "exclamationmark.circle").font(.largeTitle).foregroundStyle(.red).accessibilityHidden(true)
                Text(state.submitMessage ?? strings.text(state.kind == "AddRoaster" ? "roaster_failed_default" : state.kind == "AddShop" ? "shop_failed_default" : "editor_failed_default"))
                Text(strings.text("shop_failed_kept")).foregroundStyle(CoffeePeekNativeStyle.secondary)
                Button(strings.text("shop_retry_send")) { onAction("submit") }.buttonStyle(.borderedProminent)
                Button(strings.text("shop_back_to_form")) { onAction("dismissFailure") }
            } else {
                Image(systemName: "checkmark.circle").font(.largeTitle).foregroundStyle(CoffeePeekNativeStyle.accent).accessibilityHidden(true)
                Text(strings.text(state.kind == "AddShop" ? "shop_success_title" : state.kind == "AddRoaster" ? "roaster_success_title" : state.editing ? "editor_saved_title" : "editor_sent_title")).font(.title2)
                if let message = state.submitMessage, !message.isEmpty { Text(message) }
                if state.kind == "Editor" {
                    Text(strings.text("editor_status_\((state.resultStatus ?? "Pending").lowercased())"))
                    Button(strings.text("open_request"), action: onOpenRequest)
                } else {
                    Text(strings.text(state.kind == "AddRoaster" ? "roaster_success_description" : "shop_success_description"))
                    if state.addressValidated == false { Text(strings.text("roaster_address_unchecked")) }
                    Text(state.field("name").value).font(.headline)
                    if !state.field("address").value.isEmpty { Text(state.field("address").value) }
                }
                Button(strings.text("roaster_done"), action: onBack).buttonStyle(.borderedProminent)
            }
        }.multilineTextAlignment(.center).padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

