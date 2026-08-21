#!/usr/bin/env sh
set -eu

PROJECT="$1"
MINECRAFT_VERSION="$2"
USER_AGENT="BetterKeepInventory/1.0.0 (hello@beeps.email)"

# Resolve output relative to this script rather than the caller's directory --
# run_dev_server.sh invokes this without changing directory first.
script_dir="$(cd "$(dirname "$0")" && pwd)"
out_dir="$script_dir/build_server_jar/jars"

# fill.papermc.io serves gzip, so responses have to be decompressed before jq sees them.
api() {
  curl -sf --compressed -H "User-Agent: $USER_AGENT" "$1"
}

stable_build_url() {
  api "https://fill.papermc.io/v3/projects/${PROJECT}/versions/$1/builds" \
    | jq -r 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url) // "null"'
}

echo "Fetching latest stable $PROJECT build for Minecraft version $MINECRAFT_VERSION..."

PAPERMC_URL="$(stable_build_url "$MINECRAFT_VERSION" || echo "null")"
FOUND_VERSION="$MINECRAFT_VERSION"

# If no stable build for the requested version, find the newest version that has one.
if [ "$PAPERMC_URL" = "null" ] || [ -z "$PAPERMC_URL" ]; then
  echo "No stable build for version $MINECRAFT_VERSION, searching for latest version with stable build..."

  VERSIONS=$(api "https://fill.papermc.io/v3/projects/${PROJECT}" \
    | jq -r '.versions | to_entries[] | .value[]' \
    | sort -V -r)

  for VERSION in $VERSIONS; do
    STABLE_URL="$(stable_build_url "$VERSION" || echo "null")"

    if [ "$STABLE_URL" != "null" ] && [ -n "$STABLE_URL" ]; then
      PAPERMC_URL="$STABLE_URL"
      FOUND_VERSION="$VERSION"
      echo "Found stable build for version $VERSION"
      break
    fi
  done
fi

if [ "$PAPERMC_URL" = "null" ] || [ -z "$PAPERMC_URL" ]; then
  echo "No stable builds available for any version :("
  exit 1
fi

mkdir -p "$out_dir"
# -f so a failed download is an error rather than an HTML error page saved as a jar.
curl -fL -o "$out_dir/${PROJECT}-${MINECRAFT_VERSION}.jar" "$PAPERMC_URL"
echo "Download completed (version: $FOUND_VERSION)"
