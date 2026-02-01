package com.matejdro.bucketsync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.bucketsync.background.BackgroundSyncNotifier
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dispatch.core.withIO
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class BucketsyncRepositoryImpl(
   private val queries: DbBucketQueries,
   private val preferences: DataStore<Preferences>,
   private val backgroundSyncNotifier: BackgroundSyncNotifier,
) : BucketSyncRepository {
   private lateinit var dynamicBucketPool: IntRange

   override suspend fun init(protocolVersion: Int, dynamicPool: IntRange): Boolean {
      dynamicBucketPool = dynamicPool

      val version = preferences.data.first()[lastVersionKey]
      logcat { "Bucket sync init from ${version ?: "START"} to $protocolVersion" }
      return if (version != protocolVersion) {
         preferences.edit { it[lastVersionKey] = protocolVersion }
         withIO {
            queries.deleteAll()
         }
         false
      } else {
         true
      }
   }

   override suspend fun updateBucket(id: UByte, data: ByteArray, sortKey: Long?, flags: UByte) {
      updateBucket(id, data, sortKey, null, flags)
   }

   private suspend fun updateBucket(id: UByte, data: ByteArray, sortKey: Long?, upstreamId: String?, flags: UByte) =
      withIO<Unit> {
         queries.transaction {
            require(data.size <= BucketSyncRepository.MAX_BUCKET_SIZE_BYTES) {
               "bucket size (${data.size}) must be at most 255 bytes"
            }
            logcat { "Update bucket $id (${data.size} bytes)" }
            val inserted = queries.insert(id.toLong(), data, sortKey, upstreamId, flags.toLong()).value
            if (inserted == 0L) {
               logcat { "Content was identical, skipping update" }
            }
            val updatedBucket = queries.getBucket(id.toLong()).executeAsOne()
            if (updatedBucket.version > UShort.MAX_VALUE.toLong()) {
               logcat { "Got over UShort_MAX, wrapping around" }
               queries.resetAllVersions()
            }
         }

         backgroundSyncNotifier.notifyDataChanged()
      }

   override suspend fun awaitNextUpdate(currentVersion: UShort, maxActiveBuckets: Int): BucketUpdate = withIO {
      logcat { "Await next update from $currentVersion" }
      val versionFlow = queries.getLatestVersion().asFlow().map { it.executeAsOne().MAX?.toUShort() ?: 0u }
      val newVersion = versionFlow.debounce(BUCKET_UPDATE_DEBOUNCE).first { it != currentVersion }

      logcat { "Update $newVersion detected" }

      val requestVersion = if (newVersion > currentVersion) {
         currentVersion
      } else {
         // Do a full sync if watch's version is somehow higher than ours
         0u
      }

      createBucketUpdate(requestVersion, newVersion, maxActiveBuckets)
   }

   override suspend fun checkForNextUpdate(currentVersion: UShort, maxActiveBuckets: Int): BucketUpdate? = withIO {
      logcat { "Check next update from $currentVersion" }
      val latestVersion = queries.getLatestVersion().executeAsOne().MAX?.toUShort() ?: 0u

      logcat { "Latest version is $latestVersion" }

      val requestVersion = if (currentVersion == latestVersion) {
         return@withIO null
      } else if (latestVersion > currentVersion) {
         currentVersion
      } else {
         // Do a full sync if watch's version is somehow higher than ours
         0u
      }

      createBucketUpdate(requestVersion, latestVersion, maxActiveBuckets)
   }

   private fun createBucketUpdate(requestVersion: UShort, newVersion: UShort, maxActiveBuckets: Int): BucketUpdate {
      val activeBuckets =
         queries.getActiveBuckets(maxActiveBuckets.toLong()).executeAsList().map { it.id.toUShort() to it.flags.toUByte() }
      val potentialActiveBuckets = queries.getPotentialActiveBuckets(
         maxActiveBuckets.toLong()
      ).executeAsList().map { it.toUShort() }

      val bucketsToUpdate = queries.getUpdatedBuckets(
         requestVersion.toLong(),
         activeBuckets.map { (id, _) -> id.toLong() },
      ).executeAsList()

      // If buckets became inactive and then active again, they were deleted from the watch
      // Even though the contents has not changed. we have to send them along
      val extraBucketsToTransmit = (activeBuckets.map { it.first } - potentialActiveBuckets).mapNotNull { id ->
         val data = queries.getBucket(id.toLong()).executeAsOne().data_ ?: return@mapNotNull null

         Bucket(id.toUByte(), data)
      }

      logcat {
         "Active buckets: $activeBuckets, " +
            "bucketsToUpdate: ${bucketsToUpdate.map { it.id }}, " +
            "extraBucketsToTransmit: ${extraBucketsToTransmit.map { it.id }}"
      }

      return BucketUpdate(
         newVersion,
         activeBuckets.map { it.first },
         bucketsToUpdate.map { Bucket(it.id.toUByte(), it.data_) } + extraBucketsToTransmit,
         activeBuckets.map { it.second },
      )
   }

   override suspend fun deleteBucket(id: UByte) = withIO<Unit> {
      logcat { "Delete bucket $id" }
      queries.clearBucket(id.toLong())
      backgroundSyncNotifier.notifyDataChanged()
   }

   override suspend fun updateBucketDynamic(upstreamId: String, data: ByteArray, sortKey: Long?, flags: UByte): Int =
      withIO<Int> {
         val existingBucket = queries.getBucketWithUpstreamId(upstreamId).executeAsOneOrNull()

         val targetBucketId = if (existingBucket != null) {
            existingBucket
         } else {
            val nextBucketId = queries.getMaxSequenceId().executeAsOneOrNull()?.MAX.let { it ?: 0 } + 1

            if (nextBucketId < dynamicBucketPool.first) {
               dynamicBucketPool.first.toLong()
            } else if (nextBucketId > dynamicBucketPool.last) {
               queries.getOldestBucketInRange(dynamicBucketPool.first.toLong(), dynamicBucketPool.last.toLong()).executeAsOne()
            } else {
               nextBucketId
            }
         }

         updateBucket(targetBucketId.toUByte(), data, sortKey, upstreamId, flags)
         targetBucketId.toInt()
      }

   override suspend fun deleteBucketDynamic(upstreamId: String) = withIO<Unit> {
      val existingBucket = queries.getBucketWithUpstreamId(upstreamId).executeAsOneOrNull()
      if (existingBucket != null) {
         deleteBucket(existingBucket.toUByte())
      }
   }

   override suspend fun clearAllDynamic() = withIO<Unit> {
      queries.transaction {
         val nextVersion = queries.getLatestVersion().executeAsOneOrNull().let { it?.MAX ?: 0 } + 1
         queries.clearBuckets(
            nextVersion,
            dynamicBucketPool.first.toLong(),
            dynamicBucketPool.last.toLong()
         ).value
      }

      backgroundSyncNotifier.notifyDataChanged()
   }

   override suspend fun updateBucketFlagsSilently(id: UByte, flags: UByte) = withIO<Unit> {
      queries.updateFlagsSilently(flags.toLong(), id.toLong()).await()
   }
}

// Matching PERSIST_DATA_MAX_LENGTH of the watch SDK
private val BUCKET_UPDATE_DEBOUNCE = 100.milliseconds

private val lastVersionKey = intPreferencesKey("bucketsync_last_version")
