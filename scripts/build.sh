#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME is required" >&2
  exit 1
fi

PLATFORM="${ANDROID_PLATFORM:-35}"
BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS:-35.0.0}"
ANDROID_JAR="$SDK/platforms/android-$PLATFORM/android.jar"
BUILD_TOOLS="$SDK/build-tools/$BUILD_TOOLS_VERSION"
AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

for f in "$ANDROID_JAR" "$AAPT2" "$D8" "$ZIPALIGN" "$APKSIGNER"; do
  if [[ ! -e "$f" ]]; then
    echo "Missing required Android SDK component: $f" >&2
    exit 1
  fi
done

BUILD="$ROOT/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/stub_classes" "$BUILD/classes" "$BUILD/dex" "$BUILD/out"

mapfile -t STUB_FILES < <(find "$ROOT/stubs" -name '*.java' -print | sort)
javac -source 8 -target 8 -encoding UTF-8 -d "$BUILD/stub_classes" "${STUB_FILES[@]}"
jar cf "$BUILD/xposed-stubs.jar" -C "$BUILD/stub_classes" .

mapfile -t SOURCE_FILES < <(find "$ROOT/src" -name '*.java' -print | sort)
javac -source 8 -target 8 -encoding UTF-8 \
  -classpath "$ANDROID_JAR:$BUILD/xposed-stubs.jar" \
  -d "$BUILD/classes" "${SOURCE_FILES[@]}"
jar cf "$BUILD/module.jar" -C "$BUILD/classes" .

"$D8" --release --lib "$ANDROID_JAR" --classpath "$BUILD/xposed-stubs.jar" \
  --output "$BUILD/dex" "$BUILD/module.jar"

UNSIGNED="$BUILD/unsigned.apk"
"$AAPT2" link \
  -o "$UNSIGNED" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -A "$ROOT/assets" \
  --min-sdk-version 31 \
  --target-sdk-version 35

(
  cd "$BUILD/dex"
  zip -q "$UNSIGNED" classes.dex
)

ALIGNED="$BUILD/aligned.apk"
"$ZIPALIGN" -f 4 "$UNSIGNED" "$ALIGNED"

KS="${KEYSTORE_PATH:-}"
STOREPASS="${KEYSTORE_PASSWORD:-android}"
ALIAS="${KEY_ALIAS:-androiddebugkey}"
KEYPASS="${KEY_PASSWORD:-$STOREPASS}"

if [[ -z "$KS" ]]; then
  KS="$BUILD/ci-debug.jks"
  keytool -genkeypair \
    -keystore "$KS" \
    -storepass "$STOREPASS" \
    -keypass "$KEYPASS" \
    -alias "$ALIAS" \
    -dname 'CN=Doubao Monet CI,O=Local,C=CN' \
    -keyalg RSA -keysize 2048 -validity 3650 -noprompt >/dev/null 2>&1
fi

OUT="$BUILD/out/Doubao_Monet.apk"
cp "$ALIGNED" "$OUT"
"$APKSIGNER" sign \
  --ks "$KS" \
  --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$STOREPASS" \
  --key-pass "pass:$KEYPASS" \
  "$OUT"
"$APKSIGNER" verify --verbose "$OUT"
sha256sum "$OUT"
echo "Built: $OUT"
