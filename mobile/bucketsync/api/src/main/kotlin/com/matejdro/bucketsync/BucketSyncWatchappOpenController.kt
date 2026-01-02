package com.matejdro.bucketsync

interface BucketSyncWatchappOpenController {
   fun isNextWatchappOpenForAutoSync(): Boolean
   fun setNextWatchappOpenForAutoSync()
   fun resetNextWatchappOpen()
}
