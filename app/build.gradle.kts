plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mizunoto.hpconv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mizunoto.hpconv"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.map {
            it as com.android.build.gradle.internal.api.BaseVariantOutputImpl
        }.forEach { output ->
            val outputFileName = "HPConv-${variant.baseName}-${variant.versionName}.apk"
            println("OutputFileName: $outputFileName")
            output.outputFileName = outputFileName
        }
    }
}
println("Debug build is enabled")

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.connect.client)
}