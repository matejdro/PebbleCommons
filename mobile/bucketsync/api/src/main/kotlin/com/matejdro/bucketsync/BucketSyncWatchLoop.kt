package com.matejdro.bucketsync

import io.rebble.pebblekit2.common.model.PebbleDictionary

interface BucketSyncWatchLoop {
   fun sendFirstPacketAndStartLoop(
      helloPacketBase: PebbleDictionary,
      initialWatchVersion: UShort,
      watchBufferSize: Int,
      onBucketsChanged: suspend () -> Unit = {},
   )
}
