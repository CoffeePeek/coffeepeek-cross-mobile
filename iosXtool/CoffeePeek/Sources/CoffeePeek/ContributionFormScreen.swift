import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeContributionFormController(form: IosContributionForm, strings: ContributionsStrings) -> UIViewController {
    ContributionFormHostingController(form: form, strings: strings)
}

@MainActor
private func contributionFormSample(_ kind: String, step: Int = 0, section: String = "Description", submit: String = "idle") -> some View {
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
        submit: submit, step: step, canSubmit: true, title: "Sandbox Cafe", section: section, fields: fields, groups: groups,
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
#Preview("Shop success light") { contributionFormSample("AddShop", submit: "done").preferredColorScheme(.light) }
#Preview("Shop success dark") { contributionFormSample("AddShop", submit: "done").preferredColorScheme(.dark) }
#Preview("Roaster failure light") { contributionFormSample("AddRoaster", submit: "failed").preferredColorScheme(.light) }
#Preview("Roaster failure dark") { contributionFormSample("AddRoaster", submit: "failed").preferredColorScheme(.dark) }
#Preview("Roaster success light") { contributionFormSample("AddRoaster", submit: "done").preferredColorScheme(.light) }
#Preview("Roaster success dark") { contributionFormSample("AddRoaster", submit: "done").preferredColorScheme(.dark) }
#Preview("Shop features light") { contributionFormSample("AddShop", step: 3).preferredColorScheme(.light) }
#Preview("Shop features dark") { contributionFormSample("AddShop", step: 3).preferredColorScheme(.dark) }

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
    isolated deinit { form.close() }
}

private struct ContributionFormHost: View {
    @ObservedObject var model: ContributionFormModel
    @State private var photoImport: ContributionPhotoImport?

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
            ContributionPhotoPicker(request: request, onSelection: { providers in
                photoImport = ContributionPhotoImport(providers: providers, request: request)
                model.startedImport(request: request)
            }, onCamera: { model.pickedCamera($0, request: request) })
                .ignoresSafeArea()
        }
        .task(id: photoImport?.id) {
            guard let selection = photoImport else { return }
            let data = await selection.load()
            guard !Task.isCancelled, photoImport?.id == selection.id else { return }
            model.picked(data, request: selection.request)
            photoImport = nil
        }
        .onChange(of: model.state.accountVersion) { _, _ in photoImport = nil }
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
        ContributionsScaffold(title: title, backLabel: strings.text("back"),
            onBack: backAction, showsHeader: state.submit != "done") { formState }
        .font(CoffeePeekNativeStyle.font(16))
        .tint(CoffeePeekNativeStyle.foreground)
    }

    private var backAction: (() -> Void)? {
        state.submit == "sending" ? nil : { submissionBack() }
    }

    @ViewBuilder private var formState: some View {
        if state.signedIn == -1 || state.loading && state.kind != "AddRoaster" {
            if state.loadError != nil { loadFailure } else { CoffeePeekNativeLoader(label: strings.text("loading"), size: 32, showsLabel: true) }
        } else if state.signedIn == 0 {
            ContributionsMessage(text: strings.text(signInKey), actionTitle: strings.text("sign_in"), onAction: onSignIn)
        } else if state.submit != "idle" { submission }
        else if state.loadError != nil && state.kind != "AddRoaster" { loadFailure }
        else { form }
    }

    private func submissionBack() {
        if state.submit == "failed" { onAction("dismissFailure") } else { onBack() }
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
            }.padding(.horizontal, 16).padding(.vertical, 20)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.kind != "Suggest" {
                CoffeePeekNativeActionButton(title: submitLabel, enabled: state.canSubmit && !preparingPhotos,
                    action: { onAction(state.kind == "AddShop" ? "next" : "submit") })
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(CoffeePeekNativeStyle.background)
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
            field("name")
            field("description", multiline: true)
            city
            field("address", showsMap: true)
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
            field("name")
            field("description", multiline: true)
            if state.loading { CoffeePeekNativeLoader(label: strings.text("loading"), size: 32, showsLabel: true) }
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

    private func field(_ key: String, multiline: Bool = false, showsMap: Bool = false) -> some View {
        let value = state.field(key)
        let title = strings.fieldTitle(key, roaster: state.kind == "AddRoaster")
        let required = key == "name" || state.kind == "AddShop" && key == "address"
        let issue = (!value.value.isEmpty || state.kind == "AddRoaster" && value.issues.contains("AddressMissing"))
            ? value.issues.first : nil
        return VStack(alignment: .leading, spacing: 6) {
            Text(title + (required ? " *" : "")).font(.subheadline)
            HStack(spacing: 8) {
                input(key, title: title, value: value.value, multiline: multiline)
                    .contributionInputSurface(multiline: multiline, invalid: issue != nil)
                if showsMap { mapButton }
            }
            if key == "name" {
                Text("\(value.value.utf16.count)/\(state.kind == "AddRoaster" ? 100 : 55)")
                    .font(.caption2).foregroundStyle(CoffeePeekNativeStyle.secondary)
            }
            if let issue {
                Text(strings.issue(issue, roaster: state.kind == "AddRoaster")).font(.caption).foregroundStyle(.red)
            }
        }
    }

    private func input(_ key: String, title: String, value: String, multiline: Bool) -> some View {
        let contact = ["phone", "email", "website", "instagram"].contains(key)
        return TextField(title, text: Binding(get: { value }, set: { onEdit(key, $0) }),
                         prompt: Text(title).foregroundStyle(CoffeePeekNativeStyle.secondary),
                         axis: multiline ? .vertical : .horizontal)
            .lineLimit(multiline ? 3...6 : 1...1)
            .keyboardType(key == "phone" ? .phonePad : key == "email" ? .emailAddress : contact ? .URL : .default)
            .textInputAutocapitalization(contact ? .never : .sentences)
            .autocorrectionDisabled(contact)
    }

    private var mapButton: some View {
        Button { onAction("openMap") } label: {
            Image(systemName: "map").frame(width: 48, height: 48)
                .background(CoffeePeekNativeStyle.surface, in: Circle())
                .overlay { Circle().strokeBorder(CoffeePeekNativeStyle.border) }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(strings.text("shop_pick_on_map"))
        .disabled(state.resolvingAddress)
    }

    private var city: some View {
        let options = state.group("city")
        let placeholder = strings.text("shop_city_label") + (state.kind == "AddShop" ? " *" : "")
        let selected = options.first(where: \.selected)
        return Menu {
            Picker(placeholder, selection: Binding(get: { selected?.id ?? "" }, set: {
                if $0.isEmpty { onEdit("city", "") } else { onToggle("city", $0) }
            })) {
                Text(placeholder).tag("")
                ForEach(options) { Text($0.name).tag($0.id) }
            }
        } label: {
            HStack(spacing: 12) {
                Text(selected?.name ?? placeholder).frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.up.chevron.down").font(.system(size: 14, weight: .semibold))
                    .accessibilityHidden(true)
            }.frame(minHeight: 48).contributionInputSurface().contentShape(Rectangle())
        }.buttonStyle(.plain)
        .accessibilityLabel(strings.text("city_description"))
        .disabled(options.isEmpty)
    }

    private var price: some View {
        let options = state.group("price")
        let index = options.firstIndex(where: \.selected).map { $0 + 1 } ?? 0
        let title = index == 0 ? strings.text("price_title") : strings.format("price_cappuccino", options[index - 1].name)
        return CoffeePeekNativePriceSlider(title: title, prices: [strings.text("price_title")] + options.map(\.name),
            selectedIndex: index, showsValue: false) { selected in
            onEdit("price", selected == 0 ? "" : options[selected - 1].id)
        }
    }

    private func choices(_ key: String) -> some View {
        ContributionChoices(title: strings.groupTitle(key), options: state.group(key),
            select: { onToggle(key, $0) }, optionImage: optionImage)
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
            if !uploaded.isEmpty { uploadedPhotoCount(uploaded) }
            photoCarousel(regular, total: all.count, limit: limit, menu: menu)
        }
    }

    private func uploadedPhotoCount(_ uploaded: [ContributionFormPhoto]) -> some View {
        HStack {
            Text(strings.format("editor_already_uploaded", uploaded.count)).font(.caption)
            Spacer()
            Button(strings.text("editor_remove_uploaded")) { onRemovePhoto(uploaded[0].key) }
        }
    }

    private func photoCarousel(_ photos: [ContributionFormPhoto], total: Int, limit: Int, menu: Bool) -> some View {
        ScrollView(.horizontal) {
            LazyHStack(spacing: 12) {
                ForEach(photos) { value in removablePhoto(value) }
                if total < limit { addPhotoButton(menu: menu) }
            }
        }
    }

    private func removablePhoto(_ value: ContributionFormPhoto) -> some View {
        photo(value).frame(width: 112, height: 112).clipped().clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(alignment: .topTrailing) {
                Button { onRemovePhoto(value.key) } label: {
                    Image(systemName: "xmark.circle.fill").font(.title2).symbolRenderingMode(.palette)
                        .foregroundStyle(.white, .black.opacity(0.75)).frame(width: 44, height: 44)
                }.accessibilityLabel(strings.text("editor_remove"))
            }
    }

    private func addPhotoButton(menu: Bool) -> some View {
        Button { onAddPhoto(menu) } label: {
            if preparingPhotos { CoffeePeekNativeLoader(size: 24) }
            else { Image(systemName: "plus").font(.title) }
        }.frame(width: 112, height: 112).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 14))
            .accessibilityLabel(strings.media("title")).disabled(preparingPhotos)
    }

    private func menuRow(_ row: ContributionMenuInput) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(row.name).font(.headline)
            Picker(strings.text("editor_menu_details"), selection: Binding(get: { row.availability }, set: { onMenu(row.slug, $0, nil) })) {
                ForEach(["Unknown", "Present", "Absent"], id: \.self) { Text(strings.text("availability_\($0.lowercased())")).tag($0) }
            }.pickerStyle(.segmented)
            TextField(strings.text("editor_price_placeholder"), text: Binding(get: { row.price }, set: { onMenu(row.slug, nil, $0) }))
                .keyboardType(.decimalPad).contributionInputSurface()
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

    @ViewBuilder private var submission: some View {
        if state.submit == "done" && (state.kind == "AddShop" || state.kind == "AddRoaster") { creationSuccess }
        else { existingSubmission }
    }

    private var creationSuccess: some View {
        CoffeePeekNativeSuccessScreen(title: strings.text("\(submissionPrefix)_success_title"),
            description: state.submitMessage.flatMap { $0.isEmpty ? nil : $0 } ?? strings.text("\(submissionPrefix)_success_description"),
            buttonTitle: strings.text(state.kind == "AddShop" ? "shop_go_home" : "roaster_done"),
            closeLabel: strings.text("roaster_done"),
            notice: state.kind == "AddRoaster" && !state.field("address").value.isEmpty && state.addressValidated == false
                ? strings.text("roaster_address_unchecked") : nil,
            onDismiss: onBack, action: onBack)
    }

    private var existingSubmission: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 16) {
                    if state.submit == "done" { successContent } else { pendingContent }
                }.padding(24).frame(maxWidth: .infinity)
            }.defaultScrollAnchor(.center)
            submissionActions.padding(16)
        }.multilineTextAlignment(.center)
    }

    private var submissionPrefix: String {
        state.kind == "AddShop" ? "shop" : state.kind == "AddRoaster" ? "roaster" : "editor"
    }

    private var pendingContent: some View {
        VStack(spacing: 16) {
            if state.submit == "sending" {
                CoffeePeekNativeLoader(label: strings.text("\(submissionPrefix)_sending_title"))
                    .frame(width: 88, height: 88)
            } else {
                Image(systemName: "exclamationmark.circle").font(.system(size: 72))
                    .foregroundStyle(.red).frame(width: 88, height: 88).accessibilityHidden(true)
            }
            Text(strings.text(state.submit == "failed" ? "\(submissionPrefix)_failed_title" :
                state.kind == "Editor" && state.editing ? "editor_saving_title" : "\(submissionPrefix)_sending_title"))
                .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
            Text(state.submit == "failed" ? failureDescription : strings.text("\(submissionPrefix)_sending_description"))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    private var failureDescription: String {
        let message = state.submitMessage.flatMap { $0.isEmpty ? nil : $0 } ?? strings.text("\(submissionPrefix)_failed_default")
        return message + "\n\n" + strings.text("shop_failed_kept")
    }

    private var successContent: some View {
        VStack(spacing: 16) {
            ContributionSuccessArtwork()
            Image(systemName: "checkmark").font(.system(size: 24, weight: .semibold))
                .foregroundStyle(.black).frame(width: 48, height: 48)
                .background(CoffeePeekNativeStyle.accent, in: Circle()).accessibilityHidden(true)
            Text(strings.text(state.kind == "Editor" ? (state.editing ? "editor_saved_title" : "editor_sent_title") :
                "\(submissionPrefix)_success_title"))
                .font(CoffeePeekNativeStyle.font(24, weight: "Bold", relativeTo: .title2))
            successDescription
        }
    }

    @ViewBuilder private var successDescription: some View {
        if state.kind == "Editor" {
            Text(strings.text("editor_status_\((state.resultStatus ?? "Pending").lowercased())"))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(strings.text("open_request"), action: onOpenRequest).frame(minHeight: 44)
        } else {
            Text(state.submitMessage.flatMap { $0.isEmpty ? nil : $0 } ?? strings.text("\(submissionPrefix)_success_description"))
                .foregroundStyle(CoffeePeekNativeStyle.secondary)
        }
    }

    @ViewBuilder private var submissionActions: some View {
        if state.submit == "failed" {
            VStack(spacing: 12) {
                CoffeePeekNativeActionButton(title: strings.text("shop_retry_send"), action: { onAction("submit") })
                Button(strings.text("shop_back_to_form")) { onAction("dismissFailure") }
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .overlay { Capsule().strokeBorder(CoffeePeekNativeStyle.border).allowsHitTesting(false) }
            }
        } else if state.submit == "done" {
            CoffeePeekNativeActionButton(title: strings.text(state.kind == "AddShop" ? "shop_go_home" : "roaster_done"), action: onBack)
        }
    }
}

private struct ContributionChoices<OptionImage: View>: View {
    let title: String
    let options: [ContributionOption]
    let select: (String) -> Void
    @ViewBuilder let optionImage: (String) -> OptionImage
    @Environment(\.accessibilityReduceMotion) private var reducedMotion
    @State private var expanded = false

    var body: some View {
        if !options.isEmpty {
            DisclosureGroup(isExpanded: $expanded) {
                ForEach(Array(options.enumerated()), id: \.element.id) { index, option in
                    if index > 0 { Divider().overlay(CoffeePeekNativeStyle.border) }
                    CoffeePeekNativeChoiceRow(title: option.name, selected: option.selected, action: { select(option.id) }) {
                        if let url = option.photoUrl {
                            optionImage(url).frame(width: 28, height: 28).clipped().clipShape(Circle())
                                .accessibilityHidden(true)
                        }
                    }
                }
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(.headline)
                    let selected = options.filter(\.selected).map(\.name).joined(separator: ", ")
                    if !selected.isEmpty { Text(selected).font(.subheadline).foregroundStyle(CoffeePeekNativeStyle.secondary) }
                }.frame(minHeight: 44)
            }.padding(12).background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 16))
                .animation(reducedMotion ? nil : .smooth(duration: 0.25), value: expanded)
        }
    }
}

private struct ContributionSuccessArtwork: View {
    private var image: UIImage? {
        let resource = "com.coffeepeek.feature.contributions.impl.resources/drawable"
        for prefix in ["compose-resources/composeResources", "compose-resources"] {
            if let path = Bundle.main.path(forResource: "contributions_mascot_happy", ofType: "png", inDirectory: "\(prefix)/\(resource)"),
               let image = UIImage(contentsOfFile: path) { return image }
        }
        return nil
    }

    var body: some View {
        Group {
            if let image { Image(uiImage: image).resizable().scaledToFit() }
            else { Image(systemName: "cup.and.saucer.fill").resizable().scaledToFit().padding(32) }
        }.frame(width: 160, height: 160).accessibilityHidden(true)
    }
}

private extension View {
    func contributionInputSurface(multiline: Bool = false, invalid: Bool = false) -> some View {
        let shape = RoundedRectangle(cornerRadius: multiline ? 16 : 24)
        return textFieldStyle(.plain)
            .padding(.horizontal, 14).padding(.vertical, multiline ? 12 : 0)
            .frame(minHeight: 48)
            .background(CoffeePeekNativeStyle.surface, in: shape)
            .overlay { shape.strokeBorder(invalid ? .red : CoffeePeekNativeStyle.border).allowsHitTesting(false) }
    }
}

