package com.matejdro.bucketsync

import io.rebble.pebblekit2.common.model.PebbleDictionary

interface BucketSyncWatchLoop {
   fun sendFirstPacketAndStartLoop(
      helloPacketBase: PebbleDictionary,
      initialWatchVersion: UShort,
      watchBufferSize: Int,
      currentlyActiveBuckets: List<UByte>,
      maxActiveBuckets: Int = 15,
      onBucketsChanged: suspend () -> Unit = {},
   )
}
