"""Inspect the signed Mach-O, not the provisioning profile's wildcard grants."""
from pathlib import Path
import plistlib
import struct


def read_signed_entitlements(executable):
    data = Path(executable).read_bytes()
    if len(data) < 32 or struct.unpack_from('<I', data)[0] != 0xFEEDFACF:
        raise ValueError('Expected a thin 64-bit Mach-O executable')
    position = 32
    for _ in range(struct.unpack_from('<I', data, 16)[0]):
        if position + 8 > len(data):
            raise ValueError('Truncated Mach-O load command')
        command, size = struct.unpack_from('<II', data, position)
        if size < 8 or position + size > len(data):
            raise ValueError('Invalid Mach-O load command size')
        if command == 0x1D:
            return _command_entitlements(data, position, size)
        position += size
    raise ValueError('Executable has no code signature')


def _command_entitlements(data, position, size):
    if size < 16:
        raise ValueError('Truncated signature load command')
    offset, length = struct.unpack_from('<II', data, position + 8)
    if offset < position + size or offset + length > len(data):
        raise ValueError('Invalid code-signature range')
    return _read_entitlement_blob(data[offset:offset + length])


def _read_entitlement_blob(blob):
    if len(blob) < 12:
        raise ValueError('Missing code-signature superblob')
    magic, length, count = struct.unpack_from('>III', blob)
    if magic != 0xFADE0CC0 or length > len(blob) or 12 + count * 8 > length:
        raise ValueError('Invalid code-signature superblob')
    for index in range(count):
        kind, start = struct.unpack_from('>II', blob, 12 + index * 8)
        if kind != 5:
            continue
        if start + 8 > length:
            raise ValueError('Truncated entitlement blob')
        magic, size = struct.unpack_from('>II', blob, start)
        if magic != 0xFADE7171 or size < 8 or start + size > length:
            raise ValueError('Invalid entitlement blob')
        return plistlib.loads(blob[start + 8:start + size])
    raise ValueError('Code signature has no XML entitlements')


def verify_keychain_access(entitlements, bundle_id):
    application = entitlements.get('application-identifier', '')
    if '*' in application or not application.endswith('.' + bundle_id):
        raise ValueError('Signature application identifier does not match the app')
    groups = entitlements.get('keychain-access-groups', [])
    if not groups or groups[0] != application:
        raise ValueError('Default Keychain group does not match the application identifier')
    if any(not isinstance(group, str) or not group or '*' in group for group in groups):
        raise ValueError('Signature contains an invalid or wildcard Keychain group')


def verify_signed_app(app):
    app = Path(app)
    info = plistlib.loads((app / 'Info.plist').read_bytes())
    entitlements = read_signed_entitlements(app / info['CFBundleExecutable'])
    verify_keychain_access(entitlements, info['CFBundleIdentifier'])
    apple_enabled = info.get('CoffeePeekAppleSignInEnabled')
    if apple_enabled is not None and apple_enabled != bool(entitlements.get('com.apple.developer.applesignin')):
        raise ValueError('Apple sign-in availability does not match the signature')
    return entitlements


if __name__ == '__main__':
    import sys
    verify_signed_app(sys.argv[1])
    print('Signed application identifier, Keychain groups and provider availability verified.')
