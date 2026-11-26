/// Provisioning wildcards grant permission; they are not runtime access groups.
/// https://developer.apple.com/library/archive/technotes/tn2415/
Map<String, Object?> resolveCachedSigningEntitlements(
  Map<String, Object?> granted,
  String applicationIdentifier,
) {
  if (applicationIdentifier.contains('*') || !applicationIdentifier.contains('.')) {
    throw StateError('A concrete application identifier is required.');
  }
  final groups = granted['keychain-access-groups'];
  if (groups == null) return Map.of(granted);
  if (groups is! List<Object?> || groups.isEmpty) {
    throw StateError('Invalid Keychain access groups.');
  }
  final prefix = applicationIdentifier.split('.').first;
  return {
    ...granted,
    'keychain-access-groups': [
      for (final group in groups) resolveCachedKeychainGroup(group, prefix, applicationIdentifier),
    ],
  };
}

String resolveCachedKeychainGroup(Object? group, String prefix, String applicationIdentifier) {
  if (group is! String || group.isEmpty) throw StateError('Invalid Keychain access group.');
  if (group == '$prefix.*') return applicationIdentifier;
  if (group.contains('*')) throw StateError('Unresolved Keychain access-group wildcard.');
  return group;
}
