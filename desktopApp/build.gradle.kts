import dev.nucleusframework.desktop.application.dsl.GraalvmChannel
import dev.nucleusframework.desktop.application.dsl.GraalvmDistribution
import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.nucleus)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    // The title-bar label is drawn with Material 3, which shared only exposes as `implementation`.
    implementation(libs.compose.material3)

    // Nucleus production layer: native window chrome via the AWT-free Tao backend, plus the
    // GraalVM native-image build. Replaces Compose Desktop's application{}/Window{}.
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decoratedWindowTao)

    // Native OS file dialogs without AWT/Swing; Nucleus ships GraalVM reachability metadata for it.
    implementation(libs.filekit.dialogs.compose)

    implementation(libs.compose.uiToolingPreview)
}

nucleus.application {
    mainClass = "io.github.tobsef.svgokt.MainKt"

    graalvm {
        isEnabled.set(true)
        imageName.set("SVGPaw")
        // Nucleus 2.5's graalvm-runtime enables SharedArenaSupport, which only exists on GraalVM
        // for JDK 24+. Pin the toolchain to the LTS channel (GraalVM CE 25). NOTE: a GRAALVM_HOME
        // env var, if set, overrides this — the build must run with it cleared.
        toolchain {
            distribution.set(GraalvmDistribution.COMMUNITY)
            channel.set(GraalvmChannel.LTS)
            version.set("25.0.2")
        }
        // Optimize the image for size rather than speed (GraalVM 25 supports -Os).
        optimization.set(NativeImageOptimization.SIZE)
        // Drop per-method metadata the app never reflects over — a few MB off the exe.
        buildArgs.add("-H:+UnlockExperimentalVMOptions")
        buildArgs.add("-H:-IncludeMethodData")
    }
    nativeDistributions {
        // Zip = portable archive (runs without an installer) on every OS; Msi = Windows installer;
        // Deb = Linux installer; Dmg = macOS installer. Nucleus only builds the formats the host
        // OS actually supports, so the same list yields Zip+Msi on Windows, Zip+Deb on Linux,
        // and Dmg on macOS.
        targetFormats(TargetFormat.Zip, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.AppImage)
        packageName = "SVGPaw"
        packageVersion = (project.findProperty("appVersion") as String?) ?: "1.0.0"
        description = "Minimize your SVG footprint"
        vendor = "Tobse"
        homepage = "https://github.com/tobsef/SVGPaw"

        macOS {
            iconFile.set(project.file("icons/icon.icns"))
        }
        windows {
            // jpackage wants a real .ico; the runtime icon is rendered from icon.svg instead.
            iconFile.set(project.file("icons/icon.ico"))
            menuGroup = "SVGPaw"
            shortcut = true
            // Stable across releases, or every MSI installs beside the last one.
            upgradeUuid = "9a4f2c61-7f3b-4c58-9e26-5b8d1f0a7c43"
        }
        linux {
            iconFile.set(project.file("icons/icon.png"))
            debMaintainer = "Tobse <Public.Tobse@gmail.com>"
        }
    }
}
