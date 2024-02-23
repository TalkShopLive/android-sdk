plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "live.talkshop.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        publishing {
            multipleVariants {
                allVariants()
                withJavadocJar()
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("com.pubnub:pubnub-gson:6.4.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.test.ext:junit-ktx:1.1.5")

    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.mockito:mockito-core:3.11.2")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "live.talkshop.sdk"
            artifactId = "talkshoplive-gson"
            version = "0.1.1-alpha"
            artifact("$buildDir/outputs/aar/sdk-release.aar")
        }
    }

    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/TalkShopLive/android-sdk")
            credentials.username = System.getenv("RELEASE_USERNAME")
            credentials.password = System.getenv("RELEASE_TOKEN")
        }
    }
}