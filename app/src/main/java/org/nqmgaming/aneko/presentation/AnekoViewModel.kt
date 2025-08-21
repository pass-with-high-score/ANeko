package org.nqmgaming.aneko.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Xml
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.core.data.repository.SkinRepository
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo
import org.nqmgaming.aneko.util.loadSkinList
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class AnekoViewModel @Inject constructor(
    application: Application,
    private val repo: SkinRepository,
) : AndroidViewModel(application) {
    companion object {
        const val PREF_KEY_THEME = "theme"
    }

    private val _uiState = MutableStateFlow(ANekoState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeSkins()
                .collect { skins ->
                    _uiState.update { it.copy(skins = skins) }
                }
        }
    }


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
        val tempDir = File(context.cacheDir, "skin_temp").apply { mkdirs() }
        var pkg: String? = null
        var skinXmlFile: File? = null
        var previewFileName: String? = null
        var authorName: String? = null
        var skinName: String? = null

        // 1. Extract all files to tempDir
        resolver.openInputStream(zipUri)?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tempDir, File(entry.name).name)
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            var r: Int
                            while (zis.read(buffer).also { r = it } != -1) bos.write(buffer, 0, r)
                            bos.flush()
                        }
                        if (entry.name.endsWith("skin.xml", ignoreCase = true)) {
                            skinXmlFile = outFile
                            // Get package name from skin.xml
                            FileInputStream(outFile).use { input ->
                                val parser = Xml.newPullParser()
                                parser.setInput(input, null)
                                var event = parser.eventType
                                while (event != XmlPullParser.END_DOCUMENT) {
                                    if (event == XmlPullParser.START_TAG &&
                                        parser.name.equals("motion-params", ignoreCase = true)
                                    ) {
                                        pkg = parser.getAttributeValue(null, "package")
                                            ?: parser.getAttributeValue("", "package")
                                        authorName = parser.getAttributeValue(null, "author")
                                        previewFileName =
                                            parser.getAttributeValue(null, "preview") + ".png"
                                        skinName = parser.getAttributeValue(null, "name")
                                        break
                                    }
                                    event = parser.next()
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (pkg.isNullOrBlank() || skinXmlFile == null) {
            tempDir.deleteRecursively()
            return null
        }

        // 2. Validate images
        val validationResult = validateSkinImages(skinXmlFile, tempDir)
        if (validationResult.any { it.startsWith("Missing images") }) {
            tempDir.deleteRecursively()
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Skin validation failed: ${validationResult.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return null
        }

        // 3. Move files to final destination
        val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
        val destDir = File(skinsRoot, pkg).apply { mkdirs() }
        tempDir.listFiles()?.forEach { file ->
            val destFile = File(destDir, file.name)
            if (overwrite || !destFile.exists()) {
                file.copyTo(destFile, overwrite = true)
            }
        }
        tempDir.deleteRecursively()


        val skin = SkinEntity(
            packageName = pkg,
            name = skinName ?: "Unknown Skin",
            author = authorName ?: "Unknown",
            previewPath = previewFileName ?: "default_preview.png",
            isActive = false,
            isFavorite = false,
        )
        // 4. Save skin info to database
        viewModelScope.launch {
            repo.upsertSkin(skin)
        }
        return pkg
    }

    fun validateSkinImages(skinXmlFile: File, skinDir: File): List<String> {
        // Helper to collect drawables recursively
        fun collectDrawables(parser: XmlPullParser): Set<String> {
            val drawables = mutableSetOf<String>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            parser.getAttributeValue(null, "drawable")?.trim()
                                ?.let { drawables.add(it) }
                        }

                        "repeat-item" -> {
                            val depth = parser.depth
                            while (parser.next() != XmlPullParser.END_DOCUMENT &&
                                !(parser.eventType == XmlPullParser.END_TAG && parser.depth == depth && parser.name == "repeat-item")
                            ) {
                                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                                    parser.getAttributeValue(null, "drawable")?.trim()
                                        ?.let { drawables.add(it) }
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }
            return drawables
        }

        // 1. Parse skin.xml for all drawable names
        val drawables = FileInputStream(skinXmlFile).use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, null)
            collectDrawables(parser)
        }

        // 2. List image files in skinDir
        val imageFiles = skinDir.listFiles { file ->
            file.isFile && (file.extension in listOf("png", "jpg", "jpeg", "webp"))
        }?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()

        // 3. Find missing and extra images
        val missingImages = drawables.filter { it !in imageFiles }
        val extraImages = imageFiles.filter { it !in drawables }

        // 4. Return result
        return buildList {
            if (missingImages.isNotEmpty()) add("Missing images: $missingImages")
            if (extraImages.isNotEmpty()) add("Extra images: $extraImages")
            if (isEmpty()) add("All images match drawables.")
        }
    }

    //TODO: Save skin information to database or shared preferences
    // info: package(id), skin name, preview image, author

    fun setActive(pkg: String) = viewModelScope.launch {
        repo.switchActive(pkg)
    }
}