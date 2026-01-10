package com.matejdro.bucketsync.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatusQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface BucketSyncProviders {
   @Provides
   @SingleIn(AppScope::class)
   @BucketSyncDatabase
   fun provideBucketsyncSqliteDriver(context: Context): SqlDriver {
      return AndroidSqliteDriver(Database.Schema, context, "bucketsync.db")
   }

   @Provides
   @SingleIn(AppScope::class)
   fun provideDatabase(
      @BucketSyncDatabase
      driver: SqlDriver,
   ): Database {
      return Database(driver)
   }

   @Provides
   fun provideBucketQueries(database: Database): DbBucketQueries {
      return database.dbBucketQueries
   }

   @Provides
   fun provideSyncStatusQueries(database: Database): DbSyncStatusQueries {
      return database.dbSyncStatusQueries
   }
}

@Qualifier
annotation class BucketSyncDatabase
