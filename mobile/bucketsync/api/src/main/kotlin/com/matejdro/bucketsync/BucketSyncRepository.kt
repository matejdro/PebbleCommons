package com.matejdro.bucketsync

import com.matejdro.bucketsync.api.BucketUpdate

interface BucketSyncRepository {
   /**
    * Init the bucket sync with the current protocol version.
    *
    * When this method returns `false`, it means, that due to the protocol upgrade, all existing buckets were invalidated.
    * You need to call [updateBucket] again for all buckets to replenish the data.
    *
    * [dynamicPool] specifies a pool of buckets that [updateBucketDynamic] is allowed to pull from.
    */
   suspend fun init(protocolVersion: Int, dynamicPool: IntRange = 1..MAX_BUCKET_ID): Boolean

   /**
    * Update bucket data for the bucket [id]. This will increment the internal version - this bucket will be included
    * in the next update.
    */
   suspend fun updateBucket(id: UByte, data: ByteArray, sortKey: Long? = null)

   /**
    * Method will check whether a different version than the [currentVersion] is available. It will return a BucketUpdate
    * with the new version and all buckets that need to update or *null* if there is no such update
    */
   suspend fun awaitNextUpdate(currentVersion: UShort, maxActiveBuckets: Int = 15): BucketUpdate

   /**
    * Method will suspend until a different version than the [currentVersion] is available. Then it will return a BucketUpdate
    * with the new version and all buckets that need to update.
    *
    * This may be debounced (e.g. quick successive updates will only trigger a single update).
    */
   suspend fun checkForNextUpdate(currentVersion: UShort, maxActiveBuckets: Int = 15): BucketUpdate?

   /**
    * Delete data of the passed bucket from the watch
    */
   suspend fun deleteBucket(id: UByte)

   /**
    * Update bucket data for the bucket with the upstream ID [upstreamId].
    *
    * If the bucket with the same upstream ID already exists, it will be reused. Otherwise, either a new bucket from the
    * pool of buckets will be created, or the one with the highest [sortKey] replaced.
    *
    * This will increment the internal version - this bucket will be included in the next update.
    */
   suspend fun updateBucketDynamic(upstreamId: String, data: ByteArray, sortKey: Long? = null)

   /**
    * Delete data of the bucket with the upstream id [upstreamId] from the watch
    */
   suspend fun deleteBucketDynamic(upstreamId: String)

   /**
    * Delete all buckets from the dynamic pool
    */
   suspend fun clearAllDynamic()
}

private const val MAX_BUCKET_ID = 255
