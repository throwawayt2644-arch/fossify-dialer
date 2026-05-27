import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.File
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Codemagic + local signing support
val hasSigningCredentials =
    System.getenv("CM_KEYSTORE_PATH") != null || keystorePropertiesFile.exists()

val mKeyAlias =
    System.getenv("CM_KEY_ALIAS")
        ?: keystoreProperties["keyAlias"]?.toString()

val mKeyPassword =
    System.getenv("CM_KEY_PASSWORD")
        ?: keystoreProperties["keyPassword"]?.toString()

val mKeystorePassword =
    System.getenv("CM_KEYSTORE_PASSWORD")
        ?: keystoreProperties["storePassword"]?.toString()

val mKeystoreFile =
    System.getenv("CM_KEYSTORE_PATH")
        ?: keystoreProperties["storeFile"]?.toString()

base {
    val versionCode = project.property("VERSION_CODE").toString().toInt()
    archivesName = "phone-$versionCode"
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = project.property("APP_ID").toString()

        minSdk = project.libs.versions.app.build.minimumSDK.get().toInt()

        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()

        versionName = project.property("VERSION_NAME").toString()

        versionCode = project.property("VERSION_CODE").toString().toInt()
    }

    signingConfigs {

        if (hasSigningCredentials && mKeystoreFile != null) {

            create("release") {
                storeFile = file(mKeystoreFile)
                storePassword = mKeystorePassword
                keyAlias = mKeyAlias
                keyPassword = mKeyPassword
            }

            create("debug") {
                storeFile = file(mKeystoreFile)
                storePassword = mKeystorePassword
                keyAlias = mKeyAlias
                keyPassword = mKeyPassword
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"

            if (hasSigningCredentials) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }

        release {

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (hasSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions.add("variants")

    productFlavors {
        register("core")
        register("foss")
        register("gplay")
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    compileOptions {

        val currentJavaVersionFromLibs =
            JavaVersion.valueOf(
                libs.versions.app.build.javaVersion.get()
            )

        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
    }

    dependenciesInfo {
        includeInApk = false
    }

    androidResources {

        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    tasks.withType<KotlinCompile>().configureEach {

        compilerOptions.jvmTarget.set(
            JvmTarget.fromTarget(
                project.libs.versions.app.build.kotlinJVMTarget.get()
            )
        )
    }

    namespace = project.property("APP_ID").toString()

    lint {

        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false

        baseline = file("lint-baseline.xml")

        lintConfig = rootProject.file("lint.xml")
    }

    bundle {

        language {
            enableSplit = false
        }
    }
}

detekt {

    baseline = file("detekt-baseline.xml")

    config.setFrom("$rootDir/detekt.yml")

    buildUponDefaultConfig = true

    allRules = false
}

dependencies {

    implementation(libs.fossify.commons)

    implementation(libs.indicator.fast.scroll)

    implementation(libs.autofit.text.view)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.eventbus)

    implementation(libs.libphonenumber)

    implementation(libs.geocoder)

    detektPlugins(libs.compose.detekt)
}
