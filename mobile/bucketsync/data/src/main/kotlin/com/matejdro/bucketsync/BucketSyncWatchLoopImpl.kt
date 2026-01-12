package com.matejdro.bucketsync

import com.matejdro.bucketsync.background.BackgroundSyncNotifier
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.WatchIdentifier
import io.rebble.pebblekit2.common.util.PEBBLE_DICTIONARY_TUPLE_HEADER_SIZE
import io.rebble.pebblekit2.common.util.sizeInBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer

@Inject
@SingleIn(WatchappConnectionScope::class)
@ContributesBinding(WatchappConnectionScope::class)
class BucketSyncWatchLoopImpl(
   private val coroutineScope: CoroutineScope,
   private val packetQueue: PacketQueue,
   private val bucketSyncRepository: BucketSyncRepository,
   private val watchappOpenController: BucketSyncWatchappOpenController,
   private val backgroundSyncNotifier: BackgroundSyncNotifier,
   private val watch: WatchIdentifier,
) : BucketSyncWatchLoop {
   private var bucketSyncJob: Job? = null

   override fun sendFirstPacketAndStartLoop(
      helloPacketBase: PebbleDictionary,
      initialWatchVersion: UShort,
      watchBufferSize: Int,
   ) {
      bucketSyncJob?.cancel()
      bucketSyncJob = coroutineScope.launch {
         val bucketsyncBuffer = Buffer()
         val initialUpdate = bucketSyncRepository.checkForNextUpdate(initialWatchVersion)
         val watchVersion: UShort
         if (initialUpdate == null) {
            bucketsyncBuffer.writeUByte(SYNC_STATUS_UP_TO_DATE)

            val packet = helloPacketBase + (2u to PebbleDictionaryItem.Bytes(bucketsyncBuffer.readByteArray()))

            packetQueue.sendPacket(
               packet,
               PRIORITY_SYNC
            )
            watchVersion = initialWatchVersion
            backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
            watchappOpenController.resetNextWatchappOpen()
         } else {
            logcat { "Sending bucketsync update: ${initialUpdate.toVersion} | ${initialUpdate.bucketsToUpdate.map { it.id }}" }
            val totalHelloSizeUntilBuckets = helloPacketBase.sizeInBytes() +
               PEBBLE_DICTIONARY_TUPLE_HEADER_SIZE +
               SIZE_OF_THE_STATIC_PART_OF_SYNC_START_BUCKET_DATA +
               2 * initialUpdate.activeBuckets.size

            val extraPackets: List<PebbleDictionary> = createBucketsyncPackets(
               initialUpdate,
               bucketsyncBuffer,
               watchBufferSize - totalHelloSizeUntilBuckets,
               watchBufferSize
            )

            logcat { "Extra packets: ${extraPackets.size}" }

            val firstPacket = helloPacketBase + (2u to PebbleDictionaryItem.Bytes(bucketsyncBuffer.readByteArray()))
            packetQueue.sendPacket(
               firstPacket,
               PRIORITY_SYNC
            )
            watchappOpenController.resetNextWatchappOpen()

            for (packet in extraPackets) {
               packetQueue.sendPacket(
                  packet,
                  PRIORITY_SYNC
               )
            }

            watchVersion = initialUpdate.toVersion
            backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
         }

         observeForFutureSyncs(watchVersion, bucketsyncBuffer, watchBufferSize)
      }
   }

   private suspend fun observeForFutureSyncs(
      initialWatchVersion: UShort,
      bucketsyncBuffer: Buffer,
      watchBufferSize: Int,
   ) {
      var watchVersion = initialWatchVersion

      while (currentCoroutineContext().isActive) {
         val nextUpdate = bucketSyncRepository.awaitNextUpdate(watchVersion)
         logcat {
            "Phone updated while the watchapp is open: " +
               "${nextUpdate.toVersion} | ${nextUpdate.bucketsToUpdate.map { it.id }}"
         }

         val totalNewSyncSizeUntilBuckets = SIZE_OF_STATIC_PART_OF_NEW_UPDATE_PACKET + 2 * nextUpdate.activeBuckets.size

         val extraPackets: List<PebbleDictionary> = createBucketsyncPackets(
            nextUpdate,
            bucketsyncBuffer,
            watchBufferSize - totalNewSyncSizeUntilBuckets,
            watchBufferSize
         )

         logcat { "Extra packets: ${extraPackets.size}" }

         packetQueue.sendPacket(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(2u),
               1u to PebbleDictionaryItem.Bytes(bucketsyncBuffer.readByteArray()),
            ),
            PRIORITY_SYNC
         )

         for (packet in extraPackets) {
            packetQueue.sendPacket(
               packet,
               PRIORITY_SYNC
            )
         }

         watchVersion = nextUpdate.toVersion
         backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
      }
   }
}

private const val PRIORITY_SYNC = 0

internal const val SYNC_STATUS_UP_TO_DATE: UByte = 2u
internal const val SYNC_STATUS_LAST_PACKET: UByte = 1u
internal const val SYNC_STATUS_MORE_PACKETS: UByte = 0u

private const val SIZE_OF_THE_STATIC_PART_OF_SYNC_START_BUCKET_DATA = 1 + // Sync complete flag
   2 + // Latest bucketsync version
   1 // Number of active buckets
private const val SIZE_OF_STATIC_PART_OF_NEW_UPDATE_PACKET =
   1 + // Dictionary header
      7 + 1 + // Packet ID key + value
      7 + // Bucketsync data header
      SIZE_OF_THE_STATIC_PART_OF_SYNC_START_BUCKET_DATA
