package org.nqmgaming.aneko.presentation.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PermissionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.PermissionPage
import org.nqmgaming.aneko.presentation.permission.component.CatRunning
import org.nqmgaming.aneko.presentation.permission.component.PagerDots
import org.nqmgaming.aneko.presentation.permission.component.PermissionPageContent
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import kotlin.time.Duration.Companion.seconds

@Destination<RootGraph>(start = true)
@Composable
fun PermissionScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var checking by remember { mutableStateOf(true) }


    suspend fun checkAndNavigate() {
        checking = true
        delay(2.seconds)
//        val granted = Settings.canDrawOverlays(context)
//        if (granted) {
//            navigator.navigate(HomeScreenDestination()) {
//                popUpTo(PermissionScreenDestination) { inclusive = true }
//            }
//        } else {
//            checking = false
//        }
        checking = false
    }


    LaunchedEffect(Unit) {
        checkAndNavigate()
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                MainScope().launch {
                    checkAndNavigate()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (checking) {
        CatRunning()
    } else {
        PermissionPagerUI(
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            },
            onSkip = {
                navigator.navigate(HomeScreenDestination()) {
                    popUpTo(PermissionScreenDestination) {
                        inclusive = true
                    }
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionPagerUI(
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = remember {
        listOf(
            PermissionPage(
                title = "Cho ANeko “xuất hiện” trên màn hình",
                body = "ANeko cần quyền hiển thị trên ứng dụng khác để thả mèo lên mọi nơi bạn đang dùng.",
                imageRes = null,
                imageDesc = null,
                primaryLabel = "Tiếp tục"
            ),
            PermissionPage(
                title = "Bước 1: Chọn ANeko",
                body = "Trong màn hình cài đặt, cuộn xuống và chọn ANeko trong danh sách ứng dụng.",
                imageRes = R.drawable.select_aneko,
                imageDesc = "Chọn ứng dụng ANeko",
                primaryLabel = "Tiếp tục"
            ),
            PermissionPage(
                title = "Bước 2: Bật quyền hiển thị",
                body = "Bật “Cho phép hiển thị trên ứng dụng khác”. Xong bạn quay lại ANeko là được ✨",
                imageRes = R.drawable.grant_permission,
                imageDesc = "Bật quyền overlay",
                primaryLabel = "Mở Cài đặt"
            )
        )
    }

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cấp quyền cho ANeko") },
                actions = {
                    TextButton(onClick = onSkip) { Text("Để sau") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 16.dp
            ) { page ->
                PermissionPageContent(
                    page = pages[page],
                    pageIndex = page,
                    pageCount = pages.size
                )
            }

            // Bottom controls
            PagerDots(
                total = pages.size,
                selected = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(4.dp))

            val isLast = pagerState.currentPage == pages.lastIndex

            Button(
                onClick = {
                    if (isLast) {
                        onOpenSettings()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLast) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(pages[pagerState.currentPage].primaryLabel)
            }

            Text(
                text = "Bật xong quay lại ANeko — ứng dụng sẽ tự tiếp tục.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
private fun PermissionScreenPreview() {
    ANekoTheme {
        PermissionPagerUI(onOpenSettings = {}, onSkip = {})
    }
}