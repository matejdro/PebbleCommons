plugins {
   androidLibraryModule
   testHelpers
}

dependencies {
   api(projects.bucketsync.api)
   implementation(projects.bucketsync.data)
   implementation(libs.androidx.datastore.preferences.core)
   implementation(libs.sqldelight.jvm)
}
