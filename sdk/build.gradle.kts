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

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("com.pubnub:pubnub-kotlin:8.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.test.ext:junit-ktx:1.1.5")

    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.mockito:mockito-core:5.10.0")
}

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        publishing.publications.create<MavenPublication>(variant.name) {
            groupId = "com.github.TalkShopLive"
            artifactId = "android-sdk"
            version = "1.0.1-beta"

            pom.withXml {
                asNode().appendNode("dependencies").apply {
                    appendNode("dependency").apply {
                        appendNode("groupId", "com.pubnub")
                        appendNode("artifactId", "pubnub-kotlin")
                        appendNode("version", "8.0.0")
                    }
                }
            }
        }
    }
}