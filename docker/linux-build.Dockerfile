# Linux build environment for SVGPaw's GraalVM native image + .deb/.zip packaging.
#
# A GraalVM native image cannot be cross-compiled: the Linux binary has to be
# produced on Linux. This image gives a Windows/macOS host a Linux toolchain so
# `scripts/build-all.sh` can build the Linux artefacts in a container.
#
# Contents:
#   - a JDK to launch Gradle (Nucleus downloads its own GraalVM CE 25 on top)
#   - build-essential + zlib1g-dev: the C toolchain native-image links against
#   - patchelf: Nucleus' fixGraalvmRpath step rewrites the binary's RPATH with it
#   - Node.js 20: Nucleus provisions electron-builder through npm for packaging
#   - fakeroot: electron-builder needs it to stage the .deb without root
FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      build-essential zlib1g-dev \
      patchelf \
      ca-certificates curl gnupg \
      fakeroot \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /work
