package com.matejdro.bucketsync

import io.rebble.pebblekit2.common.model.PebbleDictionary

interface BucketSyncWatchLoop {
   fun sendFirstPacketAndStartLoop(
      helloPacketBase: PebbleDictionary,
      initialWatchVersion: UShort,
      watchBufferSize: Int,
      currentlyActiveBuckets: List<UByte>,
      maxActiveBuckets: Int = BucketSyncRepository.MAX_BUCKETS_LEGACY_WATCHES,
      onBucketsChanged: suspend () -> Unit = {},
   )
}
