package com.example.vault

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IntruderAttempt
import com.example.data.SecurityPreferences
import com.example.data.VaultDatabase
import com.example.data.VaultMediaItem
import com.example.data.VaultNote
import com.example.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class VaultCategory {
    PHOTOS,
    VIDEOS,
    NOTES,
    TRASH,
    SETTINGS
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VaultDatabase.getInstance(application)
    private val repository = VaultRepository(application, database.vaultDao())
    val securityPrefs = SecurityPreferences(application)

    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()

    private val _currentCategory = MutableStateFlow(VaultCategory.PHOTOS)
    val currentCategory: StateFlow<VaultCategory> = _currentCategory.asStateFlow()

    private val _selectedMedia = MutableStateFlow<VaultMediaItem?>(null)
    val selectedMedia: StateFlow<VaultMediaItem?> = _selectedMedia.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Real vs Decoy media streams
    val photos: StateFlow<List<VaultMediaItem>> = repository.activePhotos
        .combine(_isDecoyMode) { list, isDecoy ->
            if (isDecoy) emptyList() else list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<VaultMediaItem>> = repository.activeVideos
        .combine(_isDecoyMode) { list, isDecoy ->
            if (isDecoy) emptyList() else list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<VaultNote>> = repository.notes
        .combine(_isDecoyMode) { list, isDecoy ->
            if (isDecoy) emptyList() else list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedMedia: StateFlow<List<VaultMediaItem>> = repository.trashedMedia
        .combine(_isDecoyMode) { list, isDecoy ->
            if (isDecoy) emptyList() else list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intruderLogs: StateFlow<List<IntruderAttempt>> = repository.intruderLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDecoyMode(decoy: Boolean) {
        _isDecoyMode.value = decoy
    }

    fun setCategory(category: VaultCategory) {
        _currentCategory.value = category
    }

    fun selectMedia(item: VaultMediaItem?) {
        _selectedMedia.value = item
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getFile(item: VaultMediaItem): File {
        return repository.getFile(item)
    }

    fun importUris(uris: List<Uri>, album: String = "Default") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value = true
            var successCount = 0
            for (uri in uris) {
                val ok = repository.importMediaFromUri(uri, album)
                if (ok) successCount++
            }
            _importing.value = false
            _statusMessage.value = "$successCount items securely hidden into Vault!"
        }
    }

    fun unhideMedia(item: VaultMediaItem) {
        viewModelScope.launch {
            val success = repository.unhideAndExport(item)
            if (success) {
                if (_selectedMedia.value?.id == item.id) {
                    _selectedMedia.value = null
                }
                _statusMessage.value = "Unhidden! Restored to your device gallery."
            } else {
                _statusMessage.value = "Failed to export file."
            }
        }
    }

    fun moveToTrash(item: VaultMediaItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            if (_selectedMedia.value?.id == item.id) {
                _selectedMedia.value = null
            }
            _statusMessage.value = "Moved to Vault Recycle Bin"
        }
    }

    fun restoreFromTrash(item: VaultMediaItem) {
        viewModelScope.launch {
            repository.restoreFromTrash(item)
            _statusMessage.value = "Restored to active vault"
        }
    }

    fun deletePermanently(item: VaultMediaItem) {
        viewModelScope.launch {
            repository.deleteMediaPermanently(item)
            _statusMessage.value = "Permanently deleted"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _statusMessage.value = "Trash emptied"
        }
    }

    fun saveNote(id: Long, title: String, content: String) {
        viewModelScope.launch {
            val note = VaultNote(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                dateModified = System.currentTimeMillis()
            )
            repository.saveNote(note)
            _statusMessage.value = "Note saved"
        }
    }

    fun deleteNote(note: VaultNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _statusMessage.value = "Note deleted"
        }
    }

    fun logIntruderAttempt(pin: String) {
        viewModelScope.launch {
            repository.logIntruderAttempt(pin)
        }
    }

    fun clearIntruderLogs() {
        viewModelScope.launch {
            repository.clearIntruders()
            _statusMessage.value = "Intruder log cleared"
        }
    }

    fun createSampleDemoMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val vaultDir = File(app.filesDir, "vault_media").apply { if (!exists()) mkdirs() }

                // Create a sample private photo
                val sampleFileName = "sample_vault_demo_${UUID.randomUUID()}.jpg"
                val sampleFile = File(vaultDir, sampleFileName)
                val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.parseColor("#1E3A8A")) // Dark blue
                val paint = Paint().apply {
                    color = AndroidColor.WHITE
                    textSize = 48f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("Protected Vault Photo", 400f, 380f, paint)
                val subPaint = Paint().apply {
                    color = AndroidColor.parseColor("#93C5FD")
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("Stored securely in private sandbox", 400f, 440f, subPaint)

                FileOutputStream(sampleFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                val item = VaultMediaItem(
                    fileName = sampleFileName,
                    originalName = "Secret_Document_Scan.jpg",
                    mimeType = "image/jpeg",
                    isVideo = false,
                    sizeBytes = sampleFile.length(),
                    dateAdded = System.currentTimeMillis(),
                    album = "Default",
                    isTrashed = false
                )
                database.vaultDao().insertMedia(item)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Sample private photo added for testing!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
