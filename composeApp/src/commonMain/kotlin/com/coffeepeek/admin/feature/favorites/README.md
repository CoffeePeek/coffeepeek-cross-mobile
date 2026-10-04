# Roaster favorites

This feature owns local roaster favorites. Its `api` is the small contract used
by shop, roaster, and favorites presentation. `data` keeps serialized snapshots
private and uses the existing settings persistence and session contracts.
No new Gradle module or network endpoint is required.

Favorites are stored per account and observed through the settings flow, so
all open screens agree after adding or removing a roaster. Signing out hides
that account's saved roasters. A public slug identifies the same roaster in
the catalog and its detail page; catalog GUIDs are never used for navigation.
