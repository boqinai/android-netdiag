plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="com.boqinai.android.netdiag.demo"; compileSdk=36; defaultConfig { applicationId="com.boqinai.android.netdiag.demo"; minSdk=23; targetSdk=36; versionCode=1; versionName="1.0" }; compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }; kotlinOptions { jvmTarget="17" } }
dependencies { implementation(project(":netdiag")); implementation("androidx.appcompat:appcompat:1.7.1"); implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3") }
