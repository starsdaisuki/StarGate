package com.stardaisuki.stargate.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore 实例（整个 app 共用一个）
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stargate_prefs")

/**
 * 配置数据仓库
 * 负责配置的增删改查和持久化存储
 */
class ProfileRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val PROFILES_KEY = stringPreferencesKey("profiles")
        private val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile_id")
    }

    /**
     * 监听所有配置（Flow 是 Kotlin 的响应式流，类似 Vue 的 watch）
     */
    val profilesFlow: Flow<List<NetworkProfile>> = context.dataStore.data.map { prefs ->
        val json = prefs[PROFILES_KEY] ?: "[]"
        val activeId = prefs[ACTIVE_PROFILE_KEY] ?: ""
        val type = object : TypeToken<List<NetworkProfile>>() {}.type
        val profiles: List<NetworkProfile> = gson.fromJson(json, type)
        // 标记当前激活的配置
        profiles.map { it.copy(isActive = it.id == activeId) }
    }

    /**
     * 监听当前激活的配置 ID
     */
    val activeProfileIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROFILE_KEY]
    }

    /**
     * 保存所有配置
     */
    private suspend fun saveProfiles(profiles: List<NetworkProfile>) {
        context.dataStore.edit { prefs ->
            prefs[PROFILES_KEY] = gson.toJson(profiles)
        }
    }

    /**
     * 添加配置
     */
    suspend fun addProfile(profile: NetworkProfile) {
        val current = getCurrentProfiles()
        val newProfile = profile.copy(sortOrder = current.size)
        saveProfiles(current + newProfile)
    }

    /**
     * 更新配置
     */
    suspend fun updateProfile(profile: NetworkProfile) {
        val current = getCurrentProfiles()
        saveProfiles(current.map { if (it.id == profile.id) profile else it })
    }

    /**
     * 删除配置
     */
    suspend fun deleteProfile(profileId: String) {
        val current = getCurrentProfiles()
        saveProfiles(current.filter { it.id != profileId })
        // 如果删的是当前激活的，清除激活状态
        context.dataStore.edit { prefs ->
            if (prefs[ACTIVE_PROFILE_KEY] == profileId) {
                prefs.remove(ACTIVE_PROFILE_KEY)
            }
        }
    }

    /**
     * 设置激活的配置
     */
    suspend fun setActiveProfile(profileId: String?) {
        context.dataStore.edit { prefs ->
            if (profileId != null) {
                prefs[ACTIVE_PROFILE_KEY] = profileId
            } else {
                prefs.remove(ACTIVE_PROFILE_KEY)
            }
        }
    }

    /**
     * 根据 ID 获取单个配置
     */
    suspend fun getProfile(profileId: String): NetworkProfile? {
        return getCurrentProfiles().find { it.id == profileId }
    }

    /**
     * 获取当前所有配置（非 Flow，一次性读取）
     */
    private suspend fun getCurrentProfiles(): List<NetworkProfile> {
        var result: List<NetworkProfile> = emptyList()
        context.dataStore.edit { prefs ->
            val json = prefs[PROFILES_KEY] ?: "[]"
            val type = object : TypeToken<List<NetworkProfile>>() {}.type
            result = gson.fromJson(json, type)
        }
        return result
    }
}
