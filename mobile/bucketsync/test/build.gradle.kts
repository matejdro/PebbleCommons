plugins {
   androidLibraryModule
   testHelpers
}

dependencies {
   api(projects.bucketsync.api)
   api(libs.kotlin.coroutines)
   implementation(projects.bucketsync.data)
   implementation(libs.androidx.datastore.preferences.core)
   implementation(libs.sqldelight.jvm)
}
