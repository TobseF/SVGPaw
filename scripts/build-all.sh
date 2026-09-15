#!/usr/bin/env bash
#
# Build every SVGPaw desktop artefact in one go and collect them in ./dist:
#
#   dist/svgpaw-<ver>-win-x64.zip        portable Windows build (unzip and run, no installer)
#   dist/svgpaw-<ver>-win-x64.msi        Windows installer
#   dist/svgpaw-<ver>-linux-x64.zip      portable Linux build (unpack and run, no installer)
#   dist/svgpaw_<ver>_amd64.deb          Linux installer
#   dist/svgpaw-<ver>-linux-x64.AppImage Linux portable AppImage
#
# NOTE: Official releases are built and published automatically via GitHub Actions
# (.github/workflows/release.yml). This script remains the local test and fallback build path.
#
# A GraalVM native image cannot cross-compile, so each OS is built on its own:
#   - the host OS (Windows here) is built natively with ./gradlew
#   - Linux is built inside a Docker container (docker/linux-build.Dockerfile)
#
# Run from anywhere; the script cd's to the repo root. Needs a POSIX shell
# (Git Bash on Windows) and, for the Linux artefacts, a running Docker daemon.
set -euo pipefail

# A GRAALVM_HOME left pointing at some other GraalVM overrides Nucleus' own
# toolchain and breaks the build (it must use the GraalVM CE 25 pinned in
# desktopApp/build.gradle.kts). Clearing it here keeps the build independent of
# whatever is installed on the machine.
unset GRAALVM_HOME

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DIST="$ROOT/dist"
BIN="desktopApp/build/compose/binaries/main"
GRADLE="./gradlew"

mkdir -p "$DIST"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*|Windows_NT) HOST=windows ;;
  Linux)   HOST=linux ;;
  Darwin)  HOST=macos ;;
  *)       HOST=unknown ;;
esac

collect() { # <glob-dir> <ext>
  find "$1" -maxdepth 1 -type f -iname "*.$2" -exec cp -v {} "$DIST/" \; 2>/dev/null || true
}

build_windows_native() {
  echo "==> Windows: portable ZIP + MSI (native, on host)"
  "$GRADLE" :desktopApp:packageGraalvmZip :desktopApp:packageGraalvmMsi
  collect "$BIN/graalvm-zip" zip
  collect "$BIN/graalvm-msi" msi
}

build_linux_native() {
  echo "==> Linux: portable ZIP + DEB + AppImage (native, on host)"
  "$GRADLE" :desktopApp:packageGraalvmZip :desktopApp:packageGraalvmDeb :desktopApp:packageGraalvmAppImage
  collect "$BIN/graalvm-zip" zip
  collect "$BIN/graalvm-deb" deb
  collect "$BIN/graalvm-appimage" AppImage
}

build_linux_docker() {
  echo "==> Linux: portable ZIP + DEB + AppImage (in Docker)"
  if ! docker info >/dev/null 2>&1; then
    echo "!! Docker is not available/running — skipping the Linux artefacts." >&2
    echo "!! Start Docker Desktop (or run this script on Linux) and re-run." >&2
    return 0
  fi
  docker build -t svgpaw-linux-build -f docker/linux-build.Dockerfile docker
  # The native output dir is OS-specific; wipe it so the container rebuilds for
  # Linux instead of reusing the host's binary. A separate --project-cache-dir
  # and no config cache keep the container's Gradle state apart from the host's.
  rm -rf "$BIN" desktopApp/build/compose/tmp/main/graalvm

  # Git Bash/MSYS rewrites Unix-looking args (`/work`, `/root/.gradle`) into
  # Windows paths, which breaks `docker run`. Turn that off for this call and
  # feed the bind-mount source as a Windows path (pwd -W → D:/...).
  local mount_src="$ROOT" noconv=""
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) mount_src="$(pwd -W)"; noconv="1" ;;
  esac
  MSYS_NO_PATHCONV="$noconv" MSYS2_ARG_CONV_EXCL='*' \
  docker run --rm \
    -v "$mount_src":/work -w /work \
    -v svgpaw-gradle-cache:/root/.gradle \
    svgpaw-linux-build \
    bash -lc 'sh ./gradlew --no-configuration-cache --project-cache-dir=.gradle-linux :desktopApp:packageGraalvmZip :desktopApp:packageGraalvmDeb :desktopApp:packageGraalvmAppImage'
  collect "$BIN/graalvm-zip" zip
  collect "$BIN/graalvm-deb" deb
  collect "$BIN/graalvm-appimage" AppImage
}

echo "Host OS: $HOST"
case "$HOST" in
  windows)
    build_windows_native
    build_linux_docker
    ;;
  linux)
    build_linux_native
    echo "!! Windows artefacts need a Windows host — build those on Windows." >&2
    ;;
  macos)
    echo "!! This script builds Windows + Linux. On macOS, build the macOS DMG" >&2
    echo "!! with ./gradlew :desktopApp:packageGraalvmDmg and use it for Linux only." >&2
    build_linux_docker
    ;;
  *)
    echo "!! Unknown host OS; nothing built." >&2
    exit 1
    ;;
esac

echo
echo "== Artefacts in dist/ =="
ls -lh "$DIST"
