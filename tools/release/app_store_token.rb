require 'openssl'
require 'json'
require 'base64'

key_id = ENV.fetch('APPSTORE_API_KEY_ID')
key = OpenSSL::PKey.read(File.read(File.join(ENV.fetch('RUNNER_TEMP'), "AuthKey_#{key_id}.p8")))
abort 'Expected an App Store Connect EC P-256 key' unless key.is_a?(OpenSSL::PKey::EC) && key.group.curve_name == 'prime256v1'
now = Time.now.to_i
encode = ->(value) { Base64.urlsafe_encode64(JSON.generate(value), padding: false) }
unsigned = [encode.call({ alg: 'ES256', kid: key_id, typ: 'JWT' }),
            encode.call({ iss: ENV.fetch('APPSTORE_ISSUER_ID'), iat: now - 30, exp: now + 240, aud: 'appstoreconnect-v1' })].join('.')
signature = OpenSSL::ASN1.decode(key.sign(OpenSSL::Digest::SHA256.new, unsigned))
raw = signature.value.map do |integer|
  hex = integer.value.to_i.to_s(16).rjust(64, '0')
  raise 'Invalid ES256 signature' unless hex.length == 64
  [hex].pack('H*')
end.join
raise 'Invalid ES256 signature' unless raw.bytesize == 64
print unsigned + '.' + Base64.urlsafe_encode64(raw, padding: false)
