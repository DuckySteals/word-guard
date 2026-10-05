#!/usr/bin/env bash
# Usage: sudo ./install.sh [NEXTDNS_ID]
set -euo pipefail
cd "$(dirname "$0")"
[[ $EUID -eq 0 ]] || { echo "Run with sudo" >&2; exit 1; }

install -Dm644 dist/yt-word-filter.crx /opt/yt-word-filter/yt-word-filter.crx
install -Dm644 dist/update.xml /opt/yt-word-filter/update.xml
install -Dm644 dist/id /etc/yt-word-filter/extension-id
[[ -f /etc/yt-word-filter/words.txt ]] || install -Dm644 words.txt /etc/yt-word-filter/words.txt
[[ -n "${1:-}" ]] && echo "$1" > /etc/yt-word-filter/nextdns-id
install -Dm755 yt-filter-apply /usr/local/bin/yt-filter-apply

yt-filter-apply
