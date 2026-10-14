package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao
) {
    private val vaultDir: File = File(context.filesDir, "vault_media").apply {
        if (!exists()) mkdirs()
    }

    val activePhotos: Flow<List<VaultMediaItem>> = dao.getMediaByType(isVideo = false)
    val activeVideos: Flow<List<VaultMediaItem>> = dao.getMediaByType(isVideo = true)
    val allActiveMedia: Flow<List<VaultMediaItem>> = dao.getAllActiveMedia()
    val trashedMedia: Flow<List<VaultMediaItem>> = dao.getTrashedMedia()
    val albums: Flow<List<String>> = dao.getAlbums()
    val notes: Flow<List<VaultNote>> = dao.getAllNotes()
    val intruderLogs: Flow<List<IntruderAttempt>> = dao.getIntruderAttempts()

    fun getFile(item: VaultMediaItem): File {
        return File(vaultDir, item.fileName)
    }

    suspend fun importMediaFromUri(uri: Uri, album: String = "Default"): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var displayName = "vault_media_${System.currentTimeMillis()}"
            var size: Long = 0

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(displayName)
            val isVideo = mimeType.startsWith("video")

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: if (isVideo) "mp4" else "jpg"
            val secureFileName = "${UUID.randomUUID()}.$extension"
            val targetFile = File(vaultDir, secureFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (size == 0L && targetFile.exists()) {
                size = targetFile.length()
            }

            val item = VaultMediaItem(
                fileName = secureFileName,
                originalName = displayName,
                mimeType = mimeType,
                isVideo = isVideo,
                sizeBytes = size,
                dateAdded = System.currentTimeMillis(),
                album = album,
                isTrashed = false
            )
            dao.insertMedia(item)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun unhideAndExport(item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = getFile(item)
            if (!sourceFile.exists()) return@withContext false

            val resolver = context.contentResolver
            val collection = if (item.isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relPath = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$relPath/RestoredVault")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val destinationUri = resolver.insert(collection, values) ?: return@withContext false

            resolver.openOutputStream(destinationUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(destinationUri, values, null, null)
            }

            // Remove from vault once exported safely
            deleteMediaPermanently(item)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun moveToTrash(item: VaultMediaItem) {
        dao.setTrashedStatus(item.id, true)
    }

    suspend fun restoreFromTrash(item: VaultMediaItem) {
        dao.setTrashedStatus(item.id, false)
    }

    suspend fun deleteMediaPermanently(item: VaultMediaItem) = withContext(Dispatchers.IO) {
        try {
            val file = getFile(item)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.deleteMediaById(item.id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        // Collect trashed files and delete physical storage
        val trashed = dao.getMediaById(0) // helper or query
        dao.emptyTrash()
    }

    // Notes
    suspend fun saveNote(note: VaultNote): Long = withContext(Dispatchers.IO) {
        if (note.id == 0L) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note)
            note.id
        }
    }

    suspend fun deleteNote(note: VaultNote) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }

    // Intruder
    suspend fun logIntruderAttempt(pin: String) = withContext(Dispatchers.IO) {
        dao.insertIntruderAttempt(IntruderAttempt(attemptedPin = pin))
    }

    suspend fun clearIntruders() = withContext(Dispatchers.IO) {
        dao.clearIntruderAttempts()
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "image/jpeg"
    }
}
