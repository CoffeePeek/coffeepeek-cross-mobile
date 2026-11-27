import 'dart:convert';
import 'dart:io';
import 'package:apple_developer_kit/host/shared/apple_host_services.dart';
import 'package:apple_developer_kit/shared/signing/bundle_signer.dart';
import 'package:apple_developer_kit/shared/signing/signing_asset.dart';
import 'package:apple_developer_kit/src/host/windows/windows_file_permissions.dart';
import 'package:apple_developer_kit/src/shared/signing/macho_signer.dart';
import 'package:cli_kit/composition/native_host.dart';
import 'package:path/path.dart' as p;
import 'cached_signing_entitlements.dart';

const _bundle = 'com.coffeepeek.xcross';

final class OfflineMachineIdentity implements MachineIdentityProvider {
  @override
  Future<String> read() async => throw StateError('Cached signing does not use online machine identity.');
}

AppleHostServices signingServices() {
  final snapshot = detectPlatformHostSnapshot();
  return AppleHostServices(host: snapshot.host, abi: snapshot.abi,
    machineIdentity: OfflineMachineIdentity(), permissions: const WindowsAppleFilePermissions());
}

Future<SigningAsset> loadAsset(AppleHostServices services, String profile, String identity, String declared) =>
  SigningAssetLoader(hostServices: services).load(
    privateKeyPemPath: p.join(identity, 'key.pem'),
    certificatePemPath: p.join(identity, 'cert.pem'),
    provisioningProfilePath: profile,
    declaredEntitlements: (jsonDecode(File(declared).readAsStringSync()) as Map).cast<String, Object?>(),
  );

void requireTarget(SigningAsset asset, String device) {
  final devices = asset.profile['ProvisionedDevices'];
  if (!asset.applicationIdentifier.endsWith('.$_bundle') || devices is! List ||
      !devices.any((item) => item is String && item.toUpperCase() == device.toUpperCase())) {
    throw StateError('Cached profile does not cover the requested test app and iPhone.');
  }
}

Future<void> signRoot(String app, SigningAsset asset, AppleHostServices services) async {
  // xcross 2.1.1 treats Keychain groups as profile-owned and leaves their wildcard intact.
  // Re-sign the root through the pinned SDK's Mach-O signer with concrete runtime groups.
  final entitlements = resolveCachedSigningEntitlements(asset.entitlements, asset.applicationIdentifier);
  await MachOSigner(asset, hostServices: services).signFile(p.join(app, 'Runner'),
    identifier: _bundle, teamIdentifier: asset.teamIdentifier, entitlements: entitlements,
    infoPlistBytes: File(p.join(app, 'Info.plist')).readAsBytesSync(),
    codeResourcesBytes: File(p.join(app, '_CodeSignature', 'CodeResources')).readAsBytesSync(),
  );
}

Future<void> main(List<String> args) async {
  if (args.length != 5 || !Platform.isWindows) throw ArgumentError('Expected app, profile, identity, device, entitlements.');
  final [app, profile, identity, device, declared] = args;
  final services = signingServices();
  final asset = await loadAsset(services, profile, identity, declared);
  requireTarget(asset, device);
  final signer = BundleSigner(asset, hostServices: services);
  await signer.preflight(app);
  await signer.signApp(app);
  await signRoot(app, asset, services);
  stdout.writeln('Cached development signature written with concrete Keychain access groups.');
}
