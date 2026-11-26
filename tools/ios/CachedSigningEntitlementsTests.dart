import 'dart:convert';
import 'cached_signing_entitlements.dart';

void expect(bool value) {
  if (!value) throw StateError('Signing entitlement contract failed.');
}

void resolvesOnlyProfileWildcard() {
  final original = <String, Object?>{
    'keychain-access-groups': ['SEED.*', 'com.apple.token'],
    'aps-environment': 'development',
  };
  final result = resolveCachedSigningEntitlements(original, 'SEED.com.example.test');
  expect(jsonEncode(result['keychain-access-groups']) == '["SEED.com.example.test","com.apple.token"]');
  expect(result['aps-environment'] == 'development');
  expect(jsonEncode(original['keychain-access-groups']) == '["SEED.*","com.apple.token"]');
}

void preservesConcreteAndImplicitGroups() {
  final concrete = <String, Object?>{'keychain-access-groups': ['SEED.com.example.test']};
  expect(jsonEncode(resolveCachedSigningEntitlements(concrete, 'SEED.com.example.test')) == jsonEncode(concrete));
  expect(resolveCachedSigningEntitlements({}, 'SEED.com.example.test').isEmpty);
}

void rejectsInvalidInputs() {
  final cases = <(Map<String, Object?>, String)>[
    ({'keychain-access-groups': ['OTHER.*']}, 'SEED.com.example.test'),
    ({'keychain-access-groups': ['SEED.partial.*']}, 'SEED.com.example.test'),
    ({'keychain-access-groups': ['']}, 'SEED.com.example.test'),
    ({'keychain-access-groups': [42]}, 'SEED.com.example.test'),
    ({'keychain-access-groups': []}, 'SEED.com.example.test'),
    ({'keychain-access-groups': 'SEED.*'}, 'SEED.com.example.test'),
    ({}, 'SEED.*'),
  ];
  for (final entry in cases) {
    var failed = false;
    try { resolveCachedSigningEntitlements(entry.$1, entry.$2); } on StateError { failed = true; }
    expect(failed);
  }
}

void main() {
  resolvesOnlyProfileWildcard();
  preservesConcreteAndImplicitGroups();
  rejectsInvalidInputs();
  print('Cached signing entitlements: 10 scenarios passed.');
}
