plugins {
   androidLibraryModule
   di
   testFixtures
}

dependencies {
   api(libs.dispatch)
   api(libs.pebblekit.api)

   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.okio)

   testImplementation(libs.kotlinova.core.test)
}
