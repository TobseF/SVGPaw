import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "io.github.tobsef.svgokt.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationJson)
        }
        // svgo-kt is published from a Windows host as jvm/js/mingwX64/linuxX64. wasmJs ships as a
        // separate `svgo-kt-wasm-js` artifact instead of a variant of the same module, so it is
        // declared explicitly in wasmJsMain below. Everything talks to `SvgoEngine`, whose
        // remaining non-wired actuals (plain Kotlin/JS) are still stubs.
        jvmMain.dependencies {
            api(libs.svgo.kt)
            // Native OS file dialogs without AWT/Swing (open/save/directory), so the desktop build
            // stays compatible with Nucleus' Tao backend and the GraalVM native image.
            implementation(libs.filekit.dialogs.compose)
            // Tao routes native file drops into Compose; TaoDragAndDropPayload carries the paths
            // without touching AWT's DataFlavor.
            implementation(libs.nucleus.decoratedWindowTao)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // The desktop screenshot generator (ScreenshotTest) renders the real UI off-screen with
        // Compose's ImageComposeScene, so it needs the JUnit runner and the current-OS Skia runtime.
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.junit)
            implementation(compose.desktop.currentOs)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.svgo.kt.wasm)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}