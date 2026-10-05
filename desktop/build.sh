#!/usr/bin/env bash
# Packs ext/ into a signed .crx and writes the matching update.xml.
# Run as your normal user; install with: sudo ./install.sh
set -euo pipefail
cd "$(dirname "$0")"

[[ -f key.pem ]] || openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out key.pem 2>/dev/null

rm -rf dist && mkdir dist
chromium --no-message-box --pack-extension="$PWD/ext" --pack-extension-key="$PWD/key.pem" >/dev/null 2>&1 || true
[[ -f ext.crx ]] || { echo "Packing failed" >&2; exit 1; }
mv ext.crx dist/yt-word-filter.crx

# Extension ID = first 32 hex chars of sha256(public key DER), mapped 0-f -> a-p
id=$(openssl rsa -in key.pem -pubout -outform DER 2>/dev/null | sha256sum | head -c32 | tr '0-9a-f' 'a-p')
version=$(python3 -c 'import json;print(json.load(open("ext/manifest.json"))["version"])')

cat > dist/update.xml <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<gupdate xmlns='http://www.google.com/update2/response' protocol='2.0'>
  <app appid='$id'>
    <updatecheck codebase='file:///opt/yt-word-filter/yt-word-filter.crx' version='$version' />
  </app>
</gupdate>
EOF
echo "$id" > dist/id
echo "Built $id v$version"
