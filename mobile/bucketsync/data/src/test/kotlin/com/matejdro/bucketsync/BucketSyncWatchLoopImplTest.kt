package com.matejdro.bucketsync

import com.matejdro.bucketsync.background.FakeBackgroundSyncNotifier
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class BucketSyncWatchLoopImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val watchappOpenController = FakeBucketSyncWatchappOpenController()
   private val sender = FakePebbleSender(scope.virtualTimeProvider())
   private val watch = WatchIdentifier("watch")
   private val packetQueue = PacketQueue(sender, watch, WATCHAPP_UUID)
   private val bucketSyncRepository = FakeBucketSyncRepository()
   private val backgroundSyncNotifier = FakeBackgroundSyncNotifier()

   private val loop = BucketSyncWatchLoopImpl(
      scope.backgroundScope,
      packetQueue,
      bucketSyncRepository,
      watchappOpenController,
      backgroundSyncNotifier,
      watch
   )

   @Test
   fun `Only send status 3 when watch is up to date`() = scope.runTest {
      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         30
      )
      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            2u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in a single packet`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         30
      )
      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            2u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in two packets`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         29
      )
      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            2u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in three packets`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      bucketSyncRepository.updateBucket(3u, ByteArray(10) { 3 })

      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         29
      )
      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            2u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  0, 3, // Latest version
                  3, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  3, 0, // Metadata for bucket 3
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  3, 10 // Sync data for bucket 3
               ) + ByteArray(10) { 3 }
            ),
         )
      )
   }

   @Test
   fun `Send new sync packet if buckets update after initial sync packet `() = scope.runTest {
      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()

      sender.sentPackets.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(2u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send new sync packet if large size buckets update after initial sync packet in 3 packets`() = scope.runTest {
      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()
      sender.sentPackets.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, ByteArray(33) { 2 })
      bucketSyncRepository.updateBucket(3u, ByteArray(33) { 3 })
      delay(1.seconds)

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(2u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  0, 3, // Latest version
                  3, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  3, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  2, 33, // Sync data for bucket 2
               ) + ByteArray(33) { 2 }
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  3, 33 // Sync data for bucket 3
               ) + ByteArray(33) { 3 }
            ),
         ),
      )
   }

   @Test
   fun `Do not duplicate subsequent sync packets if bucketsync loop is called twice`() = scope.runTest {
      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()

      sender.sentPackets.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      sender.sentData.shouldHaveSize(1)
   }

   @Test
   fun `Notify that the watch up to date when the startup sync completes`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()

      backgroundSyncNotifier.watchesFullySynced.shouldContainExactly("watch")
   }

   @Test
   fun `Notify that the watch up to date when the follow up sync completes`() = scope.runTest {
      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()

      sender.sentPackets.clear()
      backgroundSyncNotifier.watchesFullySynced.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      backgroundSyncNotifier.watchesFullySynced.shouldContainExactly("watch")
   }

   @Test
   fun `Reset auto sync flag after open`() = scope.runTest {
      watchappOpenController.setNextWatchappOpenForAutoSync()

      init()
      loop.sendFirstPacketAndStartLoop(
         mapOf(0u to PebbleDictionaryItem.UInt8(1u)),
         0u,
         52
      )
      runCurrent()

      watchappOpenController.isNextWatchappOpenForAutoSync() shouldBe false
   }

   private fun init() {
      scope.backgroundScope.launch { packetQueue.runQueue() }
   }
}

private val WATCHAPP_UUID: UUID = UUID.fromString("54be2d78-a70c-4573-a73e-5b0f1323d4cd")
