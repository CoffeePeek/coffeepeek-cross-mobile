# Native iOS contribution history

MyContributions (shops, roasters and shop changes) and ShopChangeRequestDetail
now use SwiftUI through the iOS root navigator. The same contribution list and
request ViewModels own account changes, status tabs, pagination and request
loading. The iOS presentation session owns its ViewModel stores and sends Swift
only presentation fields. It blocks opening a previously displayed item as
soon as the current account changes.

The profile entry still opens the list. Published shops link to the existing
shop destination and a shop change opens its request detail. Other live entries
remain on their existing screens: BusinessTab and MyBusiness (including
ownership claims, switching, editing and the check-in QR), AddShop,
AddRoaster, SuggestShopChange and ShopChangeEditor (including camera/map,
upload and permission flows).

The iOS build workflow runs the contribution Kotlin/Native suite, including a
native session test for account replacement. Manual acceptance still needs a
signed-in list covering all statuses, pagination and retry, guest and account
switching, request sections, back navigation, both languages, light/dark
appearance and Dynamic Type. Do not create or change live requests for review.
