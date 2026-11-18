import ComposeApp
import SwiftUI

struct OwnershipNativeClaimForm: View {
    let state: IosOwnershipState
    let strings: OwnershipNativeStrings
    let send: (OwnershipNativeIntent) -> Void
    let imageLoader: CoffeePeekCardImageLoader
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(strings["ownership_claim_hint"])
            input("Contact", value: state.contact)
            input("Evidence", value: state.evidence)
            OwnershipNativePhotos(keys: state.evidencePhotos, title: strings["ownership_evidence_photos"],
                maximum: 3, evidence: true, enabled: !state.busy, strings: strings, send: send, imageLoader: imageLoader)
            Text(strings["ownership_limits"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            Button(strings["ownership_submit"]) { send(.action("submit")) }
                .buttonStyle(.borderedProminent).foregroundStyle(.black).disabled(state.busy)
        }
    }
    private func input(_ field: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.field(field)).font(.subheadline)
            TextField(strings.field(field), text: Binding(get: { value }, set: { send(.claimText(field, $0)) }), axis: .vertical)
                .lineLimit(field == "Evidence" ? 3...8 : 1...3).textFieldStyle(.roundedBorder).disabled(state.busy)
                .textInputAutocapitalization(field == "Contact" ? .never : .sentences)
        }
    }
}

struct OwnershipNativeEditor: View {
    let state: IosOwnershipState
    let strings: OwnershipNativeStrings
    let send: (OwnershipNativeIntent) -> Void
    let imageLoader: CoffeePeekCardImageLoader
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(strings["ownership_editor_hint"]).font(.footnote).foregroundStyle(CoffeePeekNativeStyle.secondary)
            ForEach(state.fields, id: \.name) { field in
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings.field(field.name)).font(.subheadline)
                    TextField(strings.field(field.name), text: Binding(get: { field.value }, set: { send(.profileText(field.name, $0)) }), axis: .vertical)
                        .lineLimit(field.name == "About" ? 3...8 : 1...3).textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(["Email", "Site", "Instagram"].contains(field.name) ? .never : .sentences)
                        .keyboardType(["Latitude", "Longitude", "Phone"].contains(field.name) ? .numbersAndPunctuation : field.name == "Email" ? .emailAddress : .default)
                        .disabled(state.busy)
                }
            }
            OwnershipNativePhotos(keys: state.editorPhotos, title: strings["ownership_photos"], maximum: 10,
                evidence: false, enabled: !state.busy && !state.reloadRequired, strings: strings, send: send, imageLoader: imageLoader)
            Button(strings["ownership_save"]) { send(.action("askSave")) }
                .buttonStyle(.borderedProminent).foregroundStyle(.black).disabled(!state.dirty || state.busy || state.reloadRequired)
        }
    }
}

struct OwnershipNativeClaimDetails: View {
    let claim: IosOwnershipClaim
    let strings: OwnershipNativeStrings
    let imageLoader: CoffeePeekCardImageLoader
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(claim.title).font(.title2.weight(.semibold))
            Text(claim.slug)
            Text(strings.status(claim.status)).foregroundStyle(CoffeePeekNativeStyle.accent)
            Text(strings[claim.access ? "ownership_access_yes" : "ownership_access_no"])
            detail("ownership_contact", claim.contact)
            detail("ownership_evidence", claim.evidence)
            if let created = claim.created { detail("ownership_claim_created", created) }
            ForEach(claim.photos, id: \.self) { key in
                CoffeePeekCardImage(url: key, symbol: nil, fits: true, loadImage: imageLoader).frame(height: 160)
            }
            if claim.status != "Pending" {
                detail("ownership_decision", claim.decision ?? strings["ownership_no_reason"])
                if let reviewed = claim.reviewed { Text(reviewed) }
            }
        }
    }
    private func detail(_ key: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 8) { Text(strings[key]).font(.headline); Text(value).textSelection(.enabled) }
            .frame(maxWidth: .infinity, alignment: .leading).padding(12)
            .background(CoffeePeekNativeStyle.surface, in: RoundedRectangle(cornerRadius: 12))
    }
}

struct OwnershipNativePhotos: View {
    let keys: [String]
    let title: String
    let maximum: Int
    let evidence: Bool
    let enabled: Bool
    let strings: OwnershipNativeStrings
    let send: (OwnershipNativeIntent) -> Void
    let imageLoader: CoffeePeekCardImageLoader
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.headline)
            ScrollView(.horizontal) {
                HStack(spacing: 12) {
                    ForEach(Array(keys.enumerated()), id: \.element) { index, key in
                        VStack(spacing: 8) {
                            CoffeePeekCardImage(url: key, symbol: nil, fits: true, loadImage: imageLoader)
                                .frame(width: 110, height: 110).clipShape(RoundedRectangle(cornerRadius: 8))
                            HStack(spacing: 4) {
                                Button { send(.movePhoto(index, index - 1, evidence)) } label: { Image(systemName: "arrow.left").frame(width: 32, height: 44) }
                                    .disabled(index == 0).accessibilityLabel(strings["ownership_photo_previous"])
                                Button(role: .destructive) { send(.removePhoto(index, evidence)) } label: { Image(systemName: "trash").frame(width: 32, height: 44) }
                                    .accessibilityLabel(strings["ownership_photo_remove"])
                                Button { send(.movePhoto(index, index + 1, evidence)) } label: { Image(systemName: "arrow.right").frame(width: 32, height: 44) }
                                    .disabled(index == keys.count - 1).accessibilityLabel(strings["ownership_photo_next"])
                            }
                        }
                    }
                    if keys.count < maximum {
                        Button { send(.addPhotos) } label: { Label(strings["ownership_photo_add"], systemImage: "plus").frame(width: 110, height: 110) }
                            .buttonStyle(.bordered)
                    }
                }
            }
        }.disabled(!enabled)
    }
}

private func ownershipFormPreview(_ editor: Bool) -> some View {
    ScrollView {
        if editor { OwnershipNativeEditor(state: ownershipPreviewState("Editor"), strings: OwnershipNativeStrings("ru"), send: { _ in }, imageLoader: { _, _ in nil }).padding() }
        else { OwnershipNativeClaimForm(state: ownershipPreviewState("Submit"), strings: OwnershipNativeStrings("ru"), send: { _ in }, imageLoader: { _, _ in nil }).padding() }
    }.foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background)
}
#Preview("Claim form light") { ownershipFormPreview(false).preferredColorScheme(.light) }
#Preview("Claim form dark") { ownershipFormPreview(false).preferredColorScheme(.dark) }
#Preview("Business editor light") { ownershipFormPreview(true).preferredColorScheme(.light) }
#Preview("Business editor dark") { ownershipFormPreview(true).preferredColorScheme(.dark) }
#Preview("Claim detail light") {
    ScrollView { OwnershipNativeClaimDetails(claim: ownershipPreviewClaim(), strings: OwnershipNativeStrings("ru"), imageLoader: { _, _ in nil }).padding() }.preferredColorScheme(.light)
}
#Preview("Claim detail dark") {
    ScrollView { OwnershipNativeClaimDetails(claim: ownershipPreviewClaim(), strings: OwnershipNativeStrings("ru"), imageLoader: { _, _ in nil }).padding() }.preferredColorScheme(.dark)
}
