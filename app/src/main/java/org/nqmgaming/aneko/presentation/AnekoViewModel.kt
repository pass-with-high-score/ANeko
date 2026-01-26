package org.nqmgaming.aneko.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Xml
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
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
import org.nqmgaming.aneko.core.data.ApiService
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.core.data.repository.SkinRepository
import org.nqmgaming.aneko.core.networking.ApiResult
import org.nqmgaming.aneko.core.service.AnimationService
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class AnekoViewModel @Inject constructor(
    application: Application,
    private val repo: SkinRepository,
    private val apiService: ApiService
) : AndroidViewModel(application) {
    companion object {
        const val PREF_KEY_THEME = "theme"
        const val PREF_KEY_FINISHED_SETUP = "finished_setup"
    }

    private val _uiState = MutableStateFlow(ANekoState())
    val uiState = _uiState.asStateFlow()

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)
    private val _isEnabledState =
        MutableStateFlow(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false))
    val isEnabledState: StateFlow<Boolean> = _isEnabledState.asStateFlow()

    private val _isFinishedSetup =
        MutableStateFlow(prefs.getBoolean(PREF_KEY_FINISHED_SETUP, false))
    val isFinishedSetup: StateFlow<Boolean> = _isFinishedSetup.asStateFlow()
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
        viewModelScope.launch {
            repo.observeSkins()
                .collect { skins ->
                    _uiState.update { it.copy(skins = skins) }
                }
        }
        getSkinCollection()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val _isDarkTheme =
        MutableStateFlow(prefs.getString(PREF_KEY_THEME, "light") == "dark")
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(prefs.getBoolean("is_first_launch", true))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    fun setFirstLaunchDone() {
        prefs.edit {
            putBoolean("is_first_launch", false)
        }
        _isFirstLaunch.value = false
    }

    fun toggleTheme() {
        val newTheme = if (_isDarkTheme.value) "light" else "dark"
        prefs.edit { putString(PREF_KEY_THEME, newTheme) }
    }

    fun updateAnimationEnabled(enabled: Boolean) {
        // if empty, use default skin
        if (prefs.getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "org.nqmgaming.aneko")
                .isNullOrBlank()
        ) {
            val defaultSkin = _uiState.value.skins.firstOrNull { it.isBuiltin }
            defaultSkin?.let {
                prefs.edit {
                    putString(AnimationService.PREF_KEY_SKIN_COMPONENT, it.packageName)
                }
            }
        }
        prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enabled) }
    }

    fun onSelectSkin(packageName: String) {
        viewModelScope.launch {
            repo.switchActive(packageName)
        }
        prefs.edit {
            putString(AnimationService.PREF_KEY_SKIN_COMPONENT, packageName)
        }
    }

    fun onDeselectSkin(skin: SkinEntity, context: Context) {
        if (skin.isBuiltin) {
            Toast.makeText(
                context,
                "Cannot remove built-in skin: ${skin.name}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModelScope.launch {
            prefs.edit {
                remove(AnimationService.PREF_KEY_SKIN_COMPONENT)
            }
            // remove skin in storage first
            val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
            val destDir = File(skinsRoot, skin.packageName)

            // remove files in storage
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            repo.removeSkin(skin)
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

    suspend fun importSkinFromUri(
        context: Context,
        zipUri: Uri,
        overwrite: Boolean = true
    ): String? {
        val tempDir =
            File(context.cacheDir, "skin_temp_${zipUri.lastPathSegment}").apply { mkdirs() }

        return context.contentResolver.openInputStream(zipUri)?.use { input ->
            importSkinFromStream(context, input, tempDir, overwrite)
        }
    }

    suspend fun importSkinFromAssets(
        context: Context,
        folderName: String,
        overwrite: Boolean = true
    ): String? {
        val tempDir = File(context.cacheDir, "skin_temp_assets_$folderName").apply { mkdirs() }

        return try {
            val assetManager = context.assets
            val assetFiles = assetManager.list(folderName) ?: arrayOf()
            for (fileName in assetFiles) {
                if (fileName.equals(
                        "__MACOSX",
                        ignoreCase = true
                    ) || fileName.endsWith(".DS_Store")
                ) {
                    continue
                }
                val assetPath = "$folderName/$fileName"
                // Check if the asset exists before opening
                if (assetManager.list(folderName)?.contains(fileName) == true) {
                    val outFile = File(tempDir, fileName)
                    assetManager.open(assetPath).use { input ->
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            input.copyTo(bos)
                        }
                    }
                } else {
                    // Log missing asset for debugging
                    Timber.e("Asset not found: $assetPath")
                }
            }
            importSkinFromStream(context, null, tempDir, overwrite)
        } catch (e: Exception) {
            e.printStackTrace()
            tempDir.deleteRecursively()
            null
        }
    }

    suspend fun importSkinFromStream(
        context: Context,
        inputStream: InputStream?,
        tempDir: File,
        overwrite: Boolean,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val files = tempDir.listFiles()
            val skinXmlFile = if (files != null && files.isNotEmpty()) {
                files.firstOrNull { it.name.equals("skin.xml", ignoreCase = true) }
            } else {
                if (inputStream == null) return@withContext null
                val extractedFiles = unzipToTempDir(inputStream, tempDir)
                extractedFiles.firstOrNull { it.name.equals("skin.xml", ignoreCase = true) }
            }

            if (skinXmlFile == null) {
                tempDir.deleteRecursively()
                return@withContext null
            }

            val skin = parseSkinMetadata(skinXmlFile, context)
            if (skin == null || skin.packageName.isBlank()) {
                tempDir.deleteRecursively()
                return@withContext null
            }

            val validationResult = validateSkinImages(skinXmlFile, skinXmlFile.parentFile!!)
            if (validationResult.any { it.startsWith("Missing images") }) {
                tempDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Skin validation failed: ${validationResult.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext null
            }

            val destDir = File(File(context.filesDir, "skins"), skin.packageName).apply { mkdirs() }
            moveSkinFilesToFinalDestination(skinXmlFile.parentFile!!, destDir, overwrite)

            tempDir.deleteRecursively()

            repo.upsertSkin(skin)
            return@withContext skin.packageName

        } catch (e: Exception) {
            e.printStackTrace()
            tempDir.deleteRecursively()
            return@withContext null
        }
    }


    fun parseSkinMetadata(xmlFile: File, context: Context): SkinEntity? {
        var pkg: String
        var author: String
        var name: String
        var preview: String

        FileInputStream(xmlFile).use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, null)
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals(
                        "motion-params",
                        ignoreCase = true
                    )
                ) {
                    pkg = parser.getAttributeValue(null, "package")
                    if (pkg.isBlank()) {
                        return null
                    }
                    author = parser.getAttributeValue(null, "author")
                    if (author.isBlank()) {
                        return null
                    }
                    name = parser.getAttributeValue(null, "name")
                    if (name.isBlank()) {
                        return null
                    }
                    preview = parser.getAttributeValue(null, "preview")
                    if (preview.isBlank()) {
                        return null
                    }
                    return SkinEntity(
                        packageName = pkg,
                        author = author,
                        name = name,
                        previewPath = preview,
                        isActive = context.packageName == pkg,
                        isFavorite = false,
                        isBuiltin = context.packageName == pkg,
                    )
                }
                event = parser.next()
            }
        }
        return null
    }


    fun unzipToTempDir(input: InputStream, tempDir: File): List<File> {
        val zis = ZipInputStream(BufferedInputStream(input))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val extractedFiles = mutableListOf<File>()

        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && !entry.name.startsWith("__MACOSX") && !entry.name.endsWith(".DS_Store")) {
                val outFile = File(tempDir, entry.name)
                outFile.parentFile?.mkdirs()
                BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        bos.write(buffer, 0, count)
                    }
                }
                extractedFiles.add(outFile)
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }

        zis.close()
        return extractedFiles
    }

    fun moveSkinFilesToFinalDestination(sourceDir: File, destDir: File, overwrite: Boolean) {
        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(destDir, relativePath)
                destFile.parentFile?.mkdirs()
                if (overwrite || !destFile.exists()) {
                    file.copyTo(destFile, overwrite = true)
                }
            }
        }
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

    fun getSkinCollection(isRefresh: Boolean = false) {
        viewModelScope.launch {
            apiService.getSkinCollection().collect { result ->
                when (result) {
                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                skinCollections = emptyList(),
                                isRefreshing = false
                            )
                        }
                    }

                    is ApiResult.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = !isRefresh,
                                isRefreshing = isRefresh,
                            )
                        }
                    }

                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(
                                skinCollections = result.data,
                                isLoading = false,
                                isRefreshing = false
                            )
                        }

                    }
                }
            }
        }
    }

    fun finishedSetup() {
        prefs.edit {
            putBoolean(PREF_KEY_FINISHED_SETUP, true)
        }
    }
}