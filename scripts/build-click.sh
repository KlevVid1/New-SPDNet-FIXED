#!/usr/bin/env bash
set -euo pipefail

ARCH="${1:-arm64}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/build/clickable"
STAGE_DIR="${BUILD_DIR}/stage"
JMODS_DIR=""

echo "============================================================"
echo " Building SPDNet Click package for Ubuntu Touch (${ARCH})"
echo "============================================================"

# 1. Build release fat JAR using Gradle
echo "[1/5] Building release JAR with Gradle..."
cd "${ROOT_DIR}"
./gradlew :desktop:release

# Locate release JAR
JAR_FILE="$(ls -1t "${ROOT_DIR}/desktop/build/libs"/desktop-*.jar | head -n 1)"
if [ -z "${JAR_FILE}" ] || [ ! -f "${JAR_FILE}" ]; then
    echo "Error: Release JAR not found in desktop/build/libs!"
    exit 1
fi
echo "Found JAR: ${JAR_FILE}"

# Extract version from build.gradle: appNetVersionName
APP_VERSION="$(grep -o "appNetVersionName = '[^']*'" "${ROOT_DIR}/build.gradle" | cut -d"'" -f2)"
if [ -z "${APP_VERSION}" ]; then
    APP_VERSION="2.4.1"
fi
echo "App Version: ${APP_VERSION}"

# 2. Setup JDK & modular JRE for target architecture
echo "[2/5] Setting up modular JRE for ${ARCH}..."
JDKS_DIR="${ROOT_DIR}/desktop/build/jdks"
mkdir -p "${JDKS_DIR}"

if [ "${ARCH}" = "arm64" ] || [ "${ARCH}" = "aarch64" ]; then
    ARCH="arm64"
    JDK_NAME="jdk-17.0.17+10"
    TARGET_JDK_DIR="${JDKS_DIR}/linux-arm64/${JDK_NAME}"
    if [ ! -d "${TARGET_JDK_DIR}/jmods" ]; then
        echo "Downloading Adoptium Temurin 17 Linux aarch64..."
        mkdir -p "${JDKS_DIR}/linux-arm64"
        aria2c -c -x 8 -s 8 "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.17%2B10/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.17_10.tar.gz" -d "${JDKS_DIR}/linux-arm64" -o "jdk-aarch64.tar.gz"
        tar -xzf "${JDKS_DIR}/linux-arm64/jdk-aarch64.tar.gz" -C "${JDKS_DIR}/linux-arm64"
    fi
    JMODS_DIR="${TARGET_JDK_DIR}/jmods"
elif [ "${ARCH}" = "amd64" ] || [ "${ARCH}" = "x86_64" ]; then
    ARCH="amd64"
    JDK_NAME="jdk-17.0.17+10"
    TARGET_JDK_DIR="${JDKS_DIR}/linux-amd64/${JDK_NAME}"
    if [ ! -d "${TARGET_JDK_DIR}/jmods" ]; then
        echo "Downloading Adoptium Temurin 17 Linux x64..."
        mkdir -p "${JDKS_DIR}/linux-amd64"
        aria2c -c -x 8 -s 8 "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.17%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.17_10.tar.gz" -d "${JDKS_DIR}/linux-amd64" -o "jdk-x64.tar.gz"
        tar -xzf "${JDKS_DIR}/linux-amd64/jdk-x64.tar.gz" -C "${JDKS_DIR}/linux-amd64"
    fi
    JMODS_DIR="${TARGET_JDK_DIR}/jmods"
else
    echo "Error: Unsupported architecture ${ARCH}. Supported: arm64, amd64"
    exit 1
fi

TARGET_JRE_DIR="${BUILD_DIR}/jre-${ARCH}"
rm -rf "${TARGET_JRE_DIR}"
echo "Generating modular JRE using jlink..."
jlink \
    --module-path "${JMODS_DIR}" \
    --add-modules java.base,java.desktop,java.logging,java.naming,java.xml,java.net.http,java.sql,jdk.unsupported,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.naming.dns,jdk.management \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output "${TARGET_JRE_DIR}"

# 3. Assemble Click staging directory
echo "[3/5] Assembling Click stage contents in ${STAGE_DIR}..."
rm -rf "${STAGE_DIR}"
mkdir -p "${STAGE_DIR}/bin" "${STAGE_DIR}/lib"

# Manifest
cat << EOF > "${STAGE_DIR}/manifest.json"
{
    "name": "ru.klevvid.spdnet",
    "title": "SPDNet",
    "description": "Multiplayer mod for Shattered Pixel Dungeon",
    "version": "${APP_VERSION}",
    "maintainer": "klevvid <klevvid@localhost>",
    "framework": "@CLICK_FRAMEWORK@",
    "architecture": "@CLICK_ARCH@",
    "hooks": {
        "spdnet": {
            "apparmor": "spdnet.apparmor",
            "desktop": "spdnet.desktop"
        }
    }
}
EOF

# AppArmor policy
cat << EOF > "${STAGE_DIR}/spdnet.apparmor"
{
    "policy_groups": [
        "audio",
        "networking",
        "video",
        "keep-display-on"
    ],
    "policy_version": 20.04
}
EOF

# Desktop launcher entry
cat << EOF > "${STAGE_DIR}/spdnet.desktop"
[Desktop Entry]
Name=SPDNet
Comment=Multiplayer mod for Shattered Pixel Dungeon
Exec=bin/spdnet
Icon=spdnet.png
Terminal=false
Type=Application
X-Ubuntu-Touch=true
X-Lomiri-Touch=true
X-Lomiri-Splash-Color=#000000
EOF

# Startup launcher script - pure shell builtins, no external utilities
cat << "EOF" > "${STAGE_DIR}/bin/spdnet"
#!/bin/sh
set -e

# Pure shell path resolution (no external binaries like readlink, dirname, mkdir)
case "$0" in
    /*) SCRIPT="$0" ;;
    *) SCRIPT="$PWD/$0" ;;
esac
SCRIPT_DIR="${SCRIPT%/*}"
APP_DIR="${SCRIPT_DIR%/*}"

if [ -z "$APP_DIR" ] || [ ! -d "$APP_DIR/jre" ]; then
    APP_DIR="$PWD"
fi
export APP_DIR

# App sandbox directory (strictly enforced by AppArmor)
export XDG_DATA_HOME="$HOME/.local/share/ru.klevvid.spdnet"
export XDG_CONFIG_HOME="$HOME/.config/ru.klevvid.spdnet"
export XDG_CACHE_HOME="$HOME/.cache/ru.klevvid.spdnet"
export TMPDIR="$XDG_DATA_HOME"

if [ -z "$XDG_RUNTIME_DIR" ]; then
    export XDG_RUNTIME_DIR="/run/user/32011"
fi
if [ -z "$WAYLAND_DISPLAY" ]; then
    export WAYLAND_DISPLAY="wayland-0"
fi

# Ensure Wayland is selected over Xwayland.
# Halium/Ubuntu Touch runs Xwayland with software rasterizer (-shm, 3-5 FPS).
# Wayland connects directly to Lomiri compositor with full Mali GPU acceleration!
unset DISPLAY
export EGL_PLATFORM="wayland"
export HYBRIS_EGLPLATFORM_DIR="${APP_DIR}/lib"
export LD_LIBRARY_PATH="${APP_DIR}/lib:${LD_LIBRARY_PATH:-}"

# Run the game using the bundled modular JRE
# java.io.tmpdir is placed in XDG_DATA_HOME so libGDX/LWJGL can extract native libraries without AppArmor permission errors
exec "${APP_DIR}/jre/bin/java" \
    -XX:+IgnoreUnrecognizedVMOptions \
    -Duser.home="$XDG_DATA_HOME" \
    -Djava.io.tmpdir="$XDG_DATA_HOME" \
    -Dorg.lwjgl.opengl.explicitInit=true \
    -Dorg.lwjgl.egl.libname=libEGL.so.1 \
    -Dorg.lwjgl.opengles.libname=libGLESv2.so.2 \
    -Dspdnet.gles=true \
    -jar "${APP_DIR}/spdnet.jar" "$@"
EOF
chmod +x "${STAGE_DIR}/bin/spdnet"

# Copy Icon
cp "${ROOT_DIR}/desktop/src/main/assets/icons/icon_256.png" "${STAGE_DIR}/spdnet.png"

# Copy patched libhybris eglplatform_wayland and libwayland_touch if arm64
if [ "${ARCH}" = "arm64" ]; then
    if [ -f "${ROOT_DIR}/desktop/src/main/native/linux-arm64/eglplatform_wayland.so" ]; then
        echo "Bundling patched libhybris Wayland EGL platform..."
        cp "${ROOT_DIR}/desktop/src/main/native/linux-arm64/eglplatform_wayland.so" "${STAGE_DIR}/lib/"
    fi
    if [ -f "${ROOT_DIR}/desktop/src/main/native/linux-arm64/libwayland_touch.so" ]; then
        echo "Bundling Wayland touch native library..."
        cp "${ROOT_DIR}/desktop/src/main/native/linux-arm64/libwayland_touch.so" "${STAGE_DIR}/lib/"
    fi
fi

# Copy JRE and JAR
echo "Copying modular JRE..."
cp -a "${TARGET_JRE_DIR}" "${STAGE_DIR}/jre"
echo "Copying game JAR..."
cp "${JAR_FILE}" "${STAGE_DIR}/spdnet.jar"

# 4. Build Click package with Clickable
echo "[4/5] Building Click package with Clickable..."
cd "${ROOT_DIR}"
clickable build --arch "${ARCH}" --skip-review

# 5. Copy built package to root
CLICK_FILE="ru.klevvid.spdnet_${APP_VERSION}_${ARCH}.click"
BUILT_PATH="$(find "${ROOT_DIR}/build" -name "${CLICK_FILE}" | head -n 1)"

if [ -n "${BUILT_PATH}" ] && [ -f "${BUILT_PATH}" ]; then
    TARGET_CLICK="${ROOT_DIR}/SPDNet-v${APP_VERSION}-ubuntu-touch-${ARCH}.click"
    cp "${BUILT_PATH}" "${TARGET_CLICK}"
    echo "[5/5] Click package successfully created:"
    echo "  1. ${BUILT_PATH}"
    echo "  2. ${TARGET_CLICK}"
    ls -lh "${TARGET_CLICK}"
else
    echo "Error: Clickable did not produce ${CLICK_FILE}!"
    exit 1
fi
