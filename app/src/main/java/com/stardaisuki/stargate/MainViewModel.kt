package com.stardaisuki.stargate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stardaisuki.stargate.data.NetworkProfile
import com.stardaisuki.stargate.data.NetworkStatus
import com.stardaisuki.stargate.data.ProfileRepository
import com.stardaisuki.stargate.utils.NetworkManager
import com.stardaisuki.stargate.utils.ShellExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "StarGate.VM"
    private val repository = ProfileRepository(application)
    private val networkManager = NetworkManager(application)

    val profiles: StateFlow<List<NetworkProfile>> = repository.profilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _hasRoot = MutableStateFlow<Boolean?>(null)
    val hasRoot: StateFlow<Boolean?> = _hasRoot.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _isSwitching = MutableStateFlow(false)
    val isSwitching: StateFlow<Boolean> = _isSwitching.asStateFlow()

    init {
        viewModelScope.launch {
            // 先检查 root（读取网络状态也需要 root）
            val root = ShellExecutor.checkRoot()
            Log.d(TAG, "Root check: $root")
            _hasRoot.value = root

            // root 检查完后立即读取网络状态
            if (root) {
                refreshNetworkStatus()
            }
        }
    }

    fun refreshNetworkStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing network status...")
                val status = networkManager.getNetworkStatus()
                Log.d(TAG, "Got status: $status")

                // 匹配当前配置
                val currentProfiles = profiles.value
                val matchedProfile = currentProfiles.find { it.isActive }
                    ?: currentProfiles.find { !it.useDhcp && it.gateway == status.gateway }

                _networkStatus.value = status.copy(
                    activeProfileId = matchedProfile?.id
                )
            } catch (e: Exception) {
                Log.e(TAG, "refreshNetworkStatus error: ${e.message}", e)
                _networkStatus.value = NetworkStatus(isConnected = false)
            }
        }
    }

    fun switchProfile(profile: NetworkProfile) {
        viewModelScope.launch {
            if (_isSwitching.value) return@launch
            _isSwitching.value = true

            try {
                Log.d(TAG, "Switching to profile: ${profile.name}")
                val result = networkManager.applyProfile(profile)
                result.fold(
                    onSuccess = { message ->
                        repository.setActiveProfile(profile.id)
                        delay(1500)
                        refreshNetworkStatus()
                        _snackbarMessage.emit(message)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Switch failed: ${error.message}")
                        _snackbarMessage.emit("切换失败: ${error.message}")
                    }
                )
            } finally {
                _isSwitching.value = false
            }
        }
    }

    fun addProfile(profile: NetworkProfile) {
        viewModelScope.launch {
            repository.addProfile(profile)
            _snackbarMessage.emit("配置「${profile.name}」已添加")
        }
    }

    fun updateProfile(profile: NetworkProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            _snackbarMessage.emit("配置「${profile.name}」已更新")
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val profile = profiles.value.find { it.id == profileId }
            repository.deleteProfile(profileId)
            _snackbarMessage.emit("配置「${profile?.name}」已删除")
        }
    }

    suspend fun getProfile(profileId: String): NetworkProfile? {
        return profiles.value.find { it.id == profileId }
    }

    fun pingTest(host: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = networkManager.pingTest(host)
            result.fold(
                onSuccess = { onResult("$host → $it") },
                onFailure = { onResult("$host → 不可达") }
            )
        }
    }
}
