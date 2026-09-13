import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    // Windows dark title bar and task-bar identity: DWM and shell attributes, with no Java or
    // Compose API to reach them.
    implementation(libs.jna.platform)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "io.github.tobsef.svgokt.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SVGPaw"
            packageVersion = "1.0.0"
            description = "Minimize your SVG footprint"
            vendor = "Tobse"

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
            }
        }
    }
}
