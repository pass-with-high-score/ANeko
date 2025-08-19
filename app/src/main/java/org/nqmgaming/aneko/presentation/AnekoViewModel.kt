package org.nqmgaming.aneko.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Xml
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo
import org.nqmgaming.aneko.util.loadSkinList
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class AnekoViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    companion object {
        const val PREF_KEY_THEME = "theme"
    }

    private val _uiState = MutableStateFlow(ANekoState())
    val uiState = _uiState.asStateFlow()

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)
    private val _isEnabledState =
        MutableStateFlow(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false))
    val isEnabledState: StateFlow<Boolean> = _isEnabledState.asStateFlow()
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                AnimationService.PREF_KEY_ENABLE -> {
                    _isEnabledState.value = sharedPreferences.getBoolean(key, false)
                }

                PREF_KEY_THEME -> {
                    _isDarkTheme.value = sharedPreferences.getString(key, "light") == "dark"
                }
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        loadSkin()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val _isDarkTheme =
        MutableStateFlow(prefs.getString(PREF_KEY_THEME, "light") == "dark")
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = if (_isDarkTheme.value) "light" else "dark"
        prefs.edit { putString(PREF_KEY_THEME, newTheme) }
    }

    fun updateAnimationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enabled) }
    }

    fun updateSkin(skinInfo: SkinInfo, index: Int) {
        _uiState.update {
            it.copy(
                selectedIndex = index
            )
        }
        prefs.edit {
            putString(
                AnimationService.PREF_KEY_SKIN_COMPONENT,
                skinInfo.component.flattenToString()
            )
        }
    }

    fun disableAnimation() {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_VISIBLE,
                false
            )
        }
    }

    fun enableAnimation() {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_VISIBLE,
                true
            )
        }
    }

    fun updateNotificationPermission(granted: Boolean) {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_NOTIFICATION_ENABLE,
                granted
            )
        }
    }

    fun loadSkin() {
        viewModelScope.launch {
            val skinList = loadSkinList(application)

            val initialSkinComponentString = prefs.getString(
                AnimationService.PREF_KEY_SKIN_COMPONENT, ""
            )

            val selectedIndex =
                skinList.indexOfFirst { it.component.flattenToString() == initialSkinComponentString }
                    .takeIf { it != -1 } ?: 0

            _uiState.update {
                it.copy(
                    skinList = skinList,
                    selectedIndex = selectedIndex
                )
            }
        }
    }

    fun importSkinZipToAppStorage(
        context: Context,
        zipUri: Uri,
        overwrite: Boolean = true
    ): String? {
        val resolver = context.contentResolver

        val pkg: String = run {
            resolver.openInputStream(zipUri)?.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(
                                "skin.xml",
                                ignoreCase = true
                            )
                        ) {
                            val parser: XmlPullParser =
                                Xml.newPullParser().apply { setInput(zis, null) }
                            var event = parser.eventType
                            while (event != XmlPullParser.END_DOCUMENT) {
                                if (event == XmlPullParser.START_TAG &&
                                    parser.name.equals("motion-params", ignoreCase = true)
                                ) {
                                    val v = parser.getAttributeValue(null, "package")
                                        ?: parser.getAttributeValue("", "package")
                                    if (!v.isNullOrBlank()) return@run v
                                }
                                event = parser.next()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            return null
        }

        val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
        val destDir = File(skinsRoot, pkg).apply { mkdirs() }
        val destCanonical = destDir.canonicalFile

        resolver.openInputStream(zipUri)?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var entry = zis.nextEntry
                while (entry != null) {
                    // Only use the file name, ignore any path in the zip entry
                    val outFile = File(destDir, File(entry.name).name)
                    val outCanonical = outFile.canonicalFile
                    if (!outCanonical.path.startsWith(destCanonical.path)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    if (!entry.isDirectory) {
                        if (overwrite || !outCanonical.exists()) {
                            BufferedOutputStream(FileOutputStream(outCanonical)).use { bos ->
                                var r: Int
                                while (zis.read(buffer).also { r = it } != -1) bos.write(
                                    buffer,
                                    0,
                                    r
                                )
                                bos.flush()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return pkg
    }
}