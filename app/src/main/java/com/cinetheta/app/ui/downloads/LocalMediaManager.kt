package com.cinetheta.app.ui.downloads

import android.content.Context
import android.provider.MediaStore
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMediaManager {

    suspend fun getLocalVideos(context: Context): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            val videoList = mutableListOf<SearchResult>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
            )
            
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            
            try {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val durationMs = cursor.getLong(durationColumn)
                        val path = cursor.getString(dataColumn)
                        
                        // Construct a content URI
                        val contentUri = android.content.ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                        
                        val durationMin = durationMs / 1000 / 60
                        val durationSec = (durationMs / 1000) % 60
                        val timeStr = String.format("%d:%02d", durationMin, durationSec)

                        videoList.add(
                            SearchResult(
                                id = id.toString(),
                                url = contentUri.toString(),
                                title = name,
                                providerId = "local_media",
                                providerName = "Local Device",
                                mediaType = com.cinetheta.domain.models.MediaType.UNKNOWN,
                                overview = "Duration: $timeStr"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.let { com.cinetheta.core.logger.Logger.e("Exception", it) }
            }
            videoList
        }
    }
}
