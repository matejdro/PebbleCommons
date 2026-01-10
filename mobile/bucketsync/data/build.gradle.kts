plugins {
   androidLibraryModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.bucketsync.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))

      }
   }
}
dependencies {
   api(projects.bluetoothCommon)
   api(projects.bucketsync.api)
   api(libs.androidx.datastore.preferences.core)

   implementation(libs.dispatch)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.androidx.workManager)
   implementation(libs.okio)
   implementation(libs.pebblekit)
   implementation(libs.sqldelight.android)

   testImplementation(projects.bucketsync.test)
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
