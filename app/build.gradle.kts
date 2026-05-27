import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.mrplayer.kdjpza"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Explicit Compose Stable
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 for Player Screen
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil for images & video thumbnails
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

// Automatically generate legacy launcher icons from the 3D logo asset and convert to genuine PNG if needed
val sourceLogo = file("src/main/res/drawable/mr_player_logo_1779843510438.png")
if (sourceLogo.exists()) {
    try {
        val img = ImageIO.read(sourceLogo)
        if (img != null) {
            // Write genuine PNG format over the source file
            ImageIO.write(img, "png", sourceLogo)

            // Write genuine PNG to the secondary mr_logo.png
            val destLogo = file("src/main/res/drawable/mr_logo.png")
            ImageIO.write(img, "png", destLogo)

            // Write genuine PNGs to the mipmap directories
            listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { d ->
                val dir = file("src/main/res/mipmap-$d")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                ImageIO.write(img, "png", File(dir, "ic_launcher.png"))
                ImageIO.write(img, "png", File(dir, "ic_launcher_round.png"))
                ImageIO.write(img, "png", File(dir, "ic_launcher_foreground.png"))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// copyApkToSafeLocation has been disabled/removed for now as requested by user to focus on application logic.
