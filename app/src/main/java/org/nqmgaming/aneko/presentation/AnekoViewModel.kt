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
        if (prefs.getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "").isNullOrBlank()) {
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

    suspend fun createSkinAdvanced(
        context: Context,
        name: String,
        packageName: String,
        author: String,
        previewUri: Uri,
        frameUrisPool: List<Uri>,
        stateMapping: Map<String, List<Pair<Uri, Int>>>,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
            val destDir = File(skinsRoot, packageName)
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()

            // Copy preview icon
            val previewFileName = copyUriToNamedFile(context, previewUri, destDir, "icon")

            // Unique pool from mapping (or fallback to pool list)
            val uniqueUris = LinkedHashSet<String>()
            stateMapping.values.flatten().forEach { (uri, _) -> uniqueUris.add(uri.toString()) }
            if (uniqueUris.isEmpty()) frameUrisPool.forEach { uniqueUris.add(it.toString()) }
            if (uniqueUris.isEmpty()) return@withContext null

            // Copy frames and map uri -> baseName
            val uriToBase = LinkedHashMap<String, String>()
            var idx = 1
            uniqueUris.forEach { uriString ->
                val u = Uri.parse(uriString)
                val baseName = "f$idx"
                val fileName = copyUriToNamedFile(context, u, destDir, baseName)
                uriToBase[uriString] = fileName.substringBeforeLast('.')
                idx++
            }

            // Helpers
            fun writeItems(sb: StringBuilder, items: List<Pair<Uri, Int>>) {
                if (items.isEmpty()) return
                sb.appendLine("        <repeat-item>")
                items.forEach { (u, dur) ->
                    val base = uriToBase[u.toString()] ?: uriToBase.values.first()
                    sb.appendLine("            <item drawable=\"$base\" duration=\"${dur.coerceAtLeast(50)}\" />")
                }
                sb.appendLine("        </repeat-item>")
            }

            val fallbackF1 = uriToBase.values.first()
            val fallbackF2 = uriToBase.values.drop(1).firstOrNull() ?: fallbackF1

            // Build XML
            val xml = buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<motion-params package=\"$packageName\" name=\"${name.trim()}\" preview=\"$previewFileName\" author=\"${author.trim()}\" ")
                appendLine("    acceleration=\"160\" awakeState=\"awake\" deaccelerationDistance=\"60\" initialState=\"stop\"")
                appendLine("    maxVelocity=\"100\" moveStatePrefix=\"move\" proximityDistance=\"10\" wallStatePrefix=\"wall\">")

                fun block(state: String, nextState: String? = null, checkMove: Boolean = false, checkWall: Boolean = false, items: List<Pair<Uri, Int>>? = null) {
                    append("    <motion state=\"$state\"")
                    if (nextState != null) append(" nextState=\"$nextState\"")
                    if (checkMove) append(" checkMove=\"true\"")
                    if (checkWall) append(" checkWall=\"true\"")
                    appendLine(">")
                    val mapped = items ?: emptyList()
                    if (mapped.isNotEmpty()) {
                        writeItems(this, mapped)
                    } else {
                        if (state.startsWith("move") || state.startsWith("wall")) {
                            appendLine("        <repeat-item>")
                            appendLine("            <item drawable=\"$fallbackF1\" duration=\"250\" />")
                            appendLine("            <item drawable=\"$fallbackF2\" duration=\"250\" />")
                            appendLine("        </repeat-item>")
                        } else if (state == "awake") {
                            appendLine("        <item drawable=\"$fallbackF1\" duration=\"750\" />")
                        } else {
                            appendLine("        <item drawable=\"$fallbackF1\" duration=\"2000\" />")
                        }
                    }
                    appendLine("    </motion>")
                }

                val get = { key: String -> stateMapping[key] }
                block("stop", nextState = "wait", checkWall = true, items = get("stop"))
                block("wait", items = get("wait"))
                block("awake", nextState = "stop", checkMove = true, items = get("awake"))

                listOf(
                    "moveUp",
                    "moveDown",
                    "moveLeft",
                    "moveRight",
                    "moveUpLeft",
                    "moveUpRight",
                    "moveDownRight",
                    "moveDownLeft",
                ).forEach { s -> block(s, items = get(s)) }

                listOf("wallUp", "wallDown", "wallLeft", "wallRight").forEach { s ->
                    block(s, nextState = "wait", items = get(s))
                }

                appendLine("</motion-params>")
            }

            File(destDir, "skin.xml").writeText(xml)

            repo.upsertSkin(
                SkinEntity(
                    packageName = packageName,
                    name = name,
                    author = author,
                    previewPath = previewFileName,
                    isActive = false,
                    isFavorite = false,
                    isBuiltin = false,
                )
            )
            return@withContext packageName
        } catch (e: Exception) {
            Timber.e(e)
            return@withContext null
        }
    }

    suspend fun createPreviewSkin(
        context: Context,
        name: String,
        author: String,
        previewUri: Uri?,
        frameUrisPool: List<Uri>,
        stateMapping: Map<String, List<Pair<Uri, Int>>>
    ): String? {
        val pkg = "org.nqmgaming.aneko.preview"
        // Fallback preview icon
        val preview = previewUri ?: frameUrisPool.firstOrNull()
        return if (preview != null) {
            createSkinAdvanced(
                context = context,
                name = name.ifBlank { "Preview" },
                packageName = pkg,
                author = author.ifBlank { "" },
                previewUri = preview,
                frameUrisPool = frameUrisPool,
                stateMapping = stateMapping,
            )
        } else null
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
                        isActive = false,
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

    fun updateSkin(
        context: Context,
        packageName: String,
        name: String,
        author: String,
        existingFrameFiles: List<File>,
        newFrameUris: List<Uri>,
        newPreviewUri: Uri?,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
                val destDir = File(skinsRoot, packageName).apply { mkdirs() }

                // Preview: if provided, copy and use; else keep old previewPath if exists
                var previewFileName: String? = null
                if (newPreviewUri != null) {
                    previewFileName =
                        copyUriToNamedFile(context, newPreviewUri, destDir, "icon")
                } else {
                    // Try to reuse from existing skin.xml metadata
                    val skinXml = File(destDir, "skin.xml")
                    if (skinXml.exists()) {
                        previewFileName = parseSkinMetadata(skinXml, context)?.previewPath
                    }
                }

                // Normalize frames list: existing files + new URIs copied in order
                val frameBaseNames = mutableListOf<String>()
                existingFrameFiles.forEach { f ->
                    if (f.exists()) frameBaseNames.add(f.nameWithoutExtension)
                }
                newFrameUris.forEachIndexed { idx, uri ->
                    val base = "frame_added_${idx + 1}"
                    val newName = copyUriToNamedFile(context, uri, destDir, base)
                    frameBaseNames.add(newName.substringBeforeLast('.'))
                }
                if (frameBaseNames.isEmpty()) {
                    withContext(Dispatchers.Main) { onError("No frames selected") }
                    return@launch
                }

                val f1 = frameBaseNames[0]
                val f2 = frameBaseNames.getOrElse(1) { f1 }

                val xml = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                    appendLine("<motion-params package=\"$packageName\" name=\"${name.trim()}\" preview=\"${previewFileName ?: "icon.png"}\" author=\"${author.trim()}\" ")
                    appendLine("    acceleration=\"160\" awakeState=\"awake\" deaccelerationDistance=\"60\" initialState=\"stop\"")
                    appendLine("    maxVelocity=\"100\" moveStatePrefix=\"move\" proximityDistance=\"10\" wallStatePrefix=\"wall\">")
                    appendLine("    <motion checkWall=\"true\" nextState=\"wait\" state=\"stop\">")
                    // Use all frames as idle loop if >2
                    if (frameBaseNames.size > 2) {
                        appendLine("        <repeat-item>")
                        frameBaseNames.forEach { base ->
                            appendLine("            <item drawable=\"$base\" duration=\"300\" />")
                        }
                        appendLine("        </repeat-item>")
                    } else {
                        appendLine("        <item drawable=\"$f1\" duration=\"2000\" />")
                    }
                    appendLine("    </motion>")
                    appendLine("    <motion state=\"wait\">")
                    frameBaseNames.forEach { base ->
                        appendLine("        <item drawable=\"$base\" duration=\"400\" />")
                    }
                    appendLine("    </motion>")
                    appendLine("    <motion checkMove=\"true\" nextState=\"stop\" state=\"awake\">")
                    appendLine("        <item drawable=\"$f1\" duration=\"750\" />")
                    appendLine("    </motion>")
                    fun moveState(state: String) {
                        appendLine("    <motion state=\"$state\">")
                        appendLine("        <repeat-item>")
                        appendLine("            <item drawable=\"$f1\" duration=\"250\" />")
                        appendLine("            <item drawable=\"$f2\" duration=\"250\" />")
                        appendLine("        </repeat-item>")
                        appendLine("    </motion>")
                    }
                    listOf(
                        "moveUp", "moveDown", "moveLeft", "moveRight",
                        "moveUpLeft", "moveUpRight", "moveDownRight", "moveDownLeft"
                    ).forEach { moveState(it) }
                    fun wallState(state: String) {
                        appendLine("    <motion nextState=\"wait\" state=\"$state\">")
                        appendLine("        <repeat-item repeatCount=\"3\">")
                        appendLine("            <item drawable=\"$f1\" duration=\"250\" />")
                        appendLine("            <item drawable=\"$f2\" duration=\"250\" />")
                        appendLine("        </repeat-item>")
                        appendLine("    </motion>")
                    }
                    listOf(
                        "wallUp",
                        "wallDown",
                        "wallLeft",
                        "wallRight"
                    ).forEach { wallState(it) }
                    appendLine("</motion-params>")
                }

                File(destDir, "skin.xml").writeText(xml)

                val skin = SkinEntity(
                    packageName = packageName,
                    name = name,
                    author = author,
                    previewPath = previewFileName ?: "icon.png",
                    isActive = _uiState.value.skins.find { it.packageName == packageName }?.isActive
                        ?: false,
                    isFavorite = _uiState.value.skins.find { it.packageName == packageName }?.isFavorite
                        ?: false,
                    isBuiltin = false,
                )
                repo.upsertSkin(skin)
                withContext(Dispatchers.Main) { onSuccess(packageName) }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) { onError(e.message) }
            }
        }
    }

    fun createSkin(
        context: Context,
        name: String,
        packageName: String,
        author: String,
        previewUri: Uri,
        frameUris: List<Uri>,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Basic validation: unique package and non-empty
                if (_uiState.value.skins.any { it.packageName == packageName }) {
                    withContext(Dispatchers.Main) { onError("Package already exists") }
                    return@launch
                }

                val skinsRoot = File(context.filesDir, "skins").apply { mkdirs() }
                val destDir = File(skinsRoot, packageName)
                if (destDir.exists()) {
                    withContext(Dispatchers.Main) { onError("Target skin folder already exists") }
                    return@launch
                }
                destDir.mkdirs()

                // Copy preview
                val previewFileName = copyUriToNamedFile(
                    context, previewUri, destDir, "icon"
                )

                // Copy frames (use first 2 frames for basic motions)
                val selected = frameUris.take(2).ifEmpty { listOf(previewUri) }
                val frameNames = mutableListOf<String>()
                selected.forEachIndexed { idx, uri ->
                    val base = "frame${idx + 1}"
                    val fileName = copyUriToNamedFile(context, uri, destDir, base)
                    frameNames.add(fileName.substringBeforeLast('.')) // base name without ext
                }

                val f1 = frameNames.getOrElse(0) { "frame1" }
                val f2 = frameNames.getOrElse(1) { f1 }

                // Generate minimal but complete skin.xml
                val skinXml = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                    appendLine("<motion-params package=\"$packageName\" name=\"${name.trim()}\" preview=\"$previewFileName\" author=\"${author.trim()}\" ")
                    appendLine("    acceleration=\"160\" awakeState=\"awake\" deaccelerationDistance=\"60\" initialState=\"stop\"")
                    appendLine("    maxVelocity=\"100\" moveStatePrefix=\"move\" proximityDistance=\"10\" wallStatePrefix=\"wall\">")
                    // stop (idle)
                    appendLine("    <motion checkWall=\"true\" nextState=\"wait\" state=\"stop\">")
                    appendLine("        <item drawable=\"$f1\" duration=\"2000\" />")
                    appendLine("    </motion>")
                    // wait (alternate idle)
                    appendLine("    <motion state=\"wait\">")
                    appendLine("        <item drawable=\"$f1\" duration=\"1500\" />")
                    appendLine("        <item drawable=\"$f2\" duration=\"1500\" />")
                    appendLine("    </motion>")
                    // awake state enables movement
                    appendLine("    <motion checkMove=\"true\" nextState=\"stop\" state=\"awake\">")
                    appendLine("        <item drawable=\"$f1\" duration=\"750\" />")
                    appendLine("    </motion>")
                    // movement states (all 8 directions reuse frames)
                    fun moveState(state: String) {
                        appendLine("    <motion state=\"$state\">")
                        appendLine("        <repeat-item>")
                        appendLine("            <item drawable=\"$f1\" duration=\"250\" />")
                        appendLine("            <item drawable=\"$f2\" duration=\"250\" />")
                        appendLine("        </repeat-item>")
                        appendLine("    </motion>")
                    }
                    listOf(
                        "moveUp", "moveDown", "moveLeft", "moveRight",
                        "moveUpLeft", "moveUpRight", "moveDownRight", "moveDownLeft"
                    ).forEach { moveState(it) }

                    // wall touch states (optional, simple loop)
                    fun wallState(state: String) {
                        appendLine("    <motion nextState=\"wait\" state=\"$state\">")
                        appendLine("        <repeat-item repeatCount=\"3\">")
                        appendLine("            <item drawable=\"$f1\" duration=\"250\" />")
                        appendLine("            <item drawable=\"$f2\" duration=\"250\" />")
                        appendLine("        </repeat-item>")
                        appendLine("    </motion>")
                    }
                    listOf("wallUp", "wallDown", "wallLeft", "wallRight").forEach { wallState(it) }

                    appendLine("</motion-params>")
                }

                File(destDir, "skin.xml").writeText(skinXml)

                // Save DB entry
                val skin = SkinEntity(
                    packageName = packageName,
                    name = name,
                    author = author,
                    previewPath = previewFileName,
                    isActive = false,
                    isFavorite = false,
                    isBuiltin = false,
                )
                repo.upsertSkin(skin)

                withContext(Dispatchers.Main) { onSuccess(packageName) }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) { onError(e.message) }
            }
        }
    }

    private fun copyUriToNamedFile(
        context: Context,
        uri: Uri,
        destDir: File,
        baseName: String
    ): String {
        val resolver = context.contentResolver
        val nameWithExt = runCatching {
            val type = resolver.getType(uri)
            val ext = when {
                type?.endsWith("png") == true -> ".png"
                type?.endsWith("jpeg") == true || type?.endsWith("jpg") == true -> ".jpg"
                type?.endsWith("webp") == true -> ".webp"
                else -> {
                    // Try to guess from uri
                    val p = uri.lastPathSegment ?: ""
                    when {
                        p.endsWith(".png", true) -> ".png"
                        p.endsWith(".jpg", true) || p.endsWith(".jpeg", true) -> ".jpg"
                        p.endsWith(".webp", true) -> ".webp"
                        else -> ".png"
                    }
                }
            }
            baseName + ext
        }.getOrDefault("$baseName.png")

        resolver.openInputStream(uri)?.use { input ->
            val outFile = File(destDir, nameWithExt)
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        return nameWithExt
    }
}
