package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.ProfileEntity
import com.example.data.TransferLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TransferState {
    object Idle : TransferState()
    data class Uploading(val progress: Float) : TransferState()
    object Success : TransferState()
    data class Error(val message: String) : TransferState()
}

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    private val _ipAddress = MutableStateFlow("192.168.1.100")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow("2121")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _titleId = MutableStateFlow("CUSA08252")
    val titleId: StateFlow<String> = _titleId.asStateFlow()

    private val _remotePath = MutableStateFlow("/user/appmeta/CUSA08252/icon0.png")
    val remotePath: StateFlow<String> = _remotePath.asStateFlow()

    private val _isPathManuallyEdited = MutableStateFlow(false)
    val isPathManuallyEdited: StateFlow<Boolean> = _isPathManuallyEdited.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _fileMetadata = MutableStateFlow<Pair<Long, String>?>(null)
    val fileMetadata: StateFlow<Pair<Long, String>?> = _fileMetadata.asStateFlow()

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    // Observe Saved Profiles from Room
    val savedProfiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe Transfer Logs from Room
    val transferLogs: StateFlow<List<TransferLogEntity>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onIpAddressChange(newIp: String) {
        _ipAddress.value = newIp
    }

    fun onPortChange(newPort: String) {
        _port.value = newPort
    }

    fun onTitleIdChange(newTitleId: String) {
        _titleId.value = newTitleId
        if (!_isPathManuallyEdited.value) {
            _remotePath.value = "/user/appmeta/$newTitleId/icon0.png"
        }
    }

    fun onRemotePathChange(newPath: String) {
        _remotePath.value = newPath
        _isPathManuallyEdited.value = true
    }

    fun resetRemotePath() {
        _isPathManuallyEdited.value = false
        _remotePath.value = "/user/appmeta/${_titleId.value}/icon0.png"
    }

    fun onFileSelected(uri: Uri?) {
        _selectedUri.value = uri
        if (uri != null) {
            _fileMetadata.value = repository.getUriMetadata(uri)
        } else {
            _fileMetadata.value = null
        }
        // If we succeeded previously and select a new file, return to idle
        if (_transferState.value is TransferState.Success || _transferState.value is TransferState.Error) {
            _transferState.value = TransferState.Idle
        }
    }

    fun resetTransferState() {
        _transferState.value = TransferState.Idle
    }

    fun selectProfile(profile: ProfileEntity) {
        _ipAddress.value = profile.ipAddress
        _port.value = profile.port.toString()
        _titleId.value = profile.defaultTitleId
        _remotePath.value = profile.manualRemotePath
        _isPathManuallyEdited.value = profile.manualRemotePath != "/user/appmeta/${profile.defaultTitleId}/icon0.png"
        
        // Update database lastUsed timestamp
        viewModelScope.launch {
            repository.updateProfile(profile.copy(lastUsedTimestamp = System.currentTimeMillis()))
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun saveCurrentProfile(name: String) {
        if (name.isBlank()) return
        val portInt = _port.value.toIntOrNull() ?: 2121
        val profile = ProfileEntity(
            name = name,
            ipAddress = _ipAddress.value,
            port = portInt,
            defaultTitleId = _titleId.value,
            manualRemotePath = _remotePath.value
        )
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun triggerFtpUpload() {
        val uri = _selectedUri.value ?: return
        val ip = _ipAddress.value
        val portVal = _port.value.toIntOrNull() ?: 2121
        val destPath = _remotePath.value
        val tid = _titleId.value

        if (ip.isBlank()) {
            _transferState.value = TransferState.Error("IP Address cannot be blank.")
            return
        }

        viewModelScope.launch {
            _transferState.value = TransferState.Uploading(0.0f)
            val result = repository.performFtpUpload(
                uri = uri,
                ip = ip,
                port = portVal,
                remotePath = destPath,
                titleId = tid,
                onProgress = { progress ->
                    _transferState.value = TransferState.Uploading(progress)
                }
            )

            result.onSuccess {
                _transferState.value = TransferState.Success
            }.onFailure { exception ->
                _transferState.value = TransferState.Error(exception.message ?: "FTP Upload Failed")
            }
        }
    }
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(repository) as T
    }
}
