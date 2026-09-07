package com.scanflow.qr.feature.myqr

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ElectricBlueLight
import com.scanflow.qr.core.designsystem.EmptyStateView
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.SuccessGreen
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.core.utils.ShareHelper
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrPatternStyle
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyQrUiState(
    val userQrs: List<UserQrCode> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterType: QrType? = null,
    val isSortNewestFirst: Boolean = true,
    val isLoading: Boolean = false
)

private fun UserQrCode.toStyleConfig(): QrStyleConfig {
    val pattern = runCatching { QrPatternStyle.valueOf(patternStyle.ifEmpty { "SQUARE" }) }.getOrDefault(QrPatternStyle.SQUARE)
    val eye = runCatching { QrCornerStyle.valueOf(eyeStyle.ifEmpty { "SQUARE" }) }.getOrDefault(QrCornerStyle.SQUARE)
    return QrStyleConfig(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
        patternStyle = pattern,
        cornerEyeStyle = eye
    )
}

@HiltViewModel
class MyQrViewModel @Inject constructor(
    private val qrRepository: QrGeneratorRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterType = MutableStateFlow<QrType?>(null)
    private val _isSortNewestFirst = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            val currentList = qrRepository.getAllUserQrs().first()
            if (currentList.isEmpty()) {
                seedShowcaseQrs()
            }
        }
    }

    private suspend fun seedShowcaseQrs() {
        val showcase = listOf(
            UserQrCode(
                type = QrType.WIFI,
                title = "WiFi Kantor Utama (5G)",
                content = "WIFI:T:WPA;S:_Office_HQ_Fast5G;P:ScanFlowSecure2023;;",
                scanCount = 420,
                shareCount = 15,
                downloadCount = 10,
                createdAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 5)
            ),
            UserQrCode(
                type = QrType.CONTACT,
                title = "Kartu Bisnis Digital",
                content = "BEGIN:VCARD\nVERSION:3.0\nN:Somers;Alex;;;\nFN:Alex Somers\nTITLE:Senior Designer\nTEL:+1-555-0199\nEND:VCARD",
                scanCount = 185,
                shareCount = 28,
                downloadCount = 14,
                createdAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 12)
            ),
            UserQrCode(
                type = QrType.PAYMENT,
                title = "QRIS Pembayaran Klien",
                content = "00020101021126580014ID.DOKU.WWW01189360091100000000000215ID1020000000000052045812530336054061500005802ID5911Client Pay6010JAKARTA62070703A016304",
                scanCount = 512,
                shareCount = 42,
                downloadCount = 31,
                createdAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 20)
            ),
            UserQrCode(
                type = QrType.WEBSITE,
                title = "Portfolio Website & CV",
                content = "https://alexsomers.design",
                scanCount = 311,
                shareCount = 19,
                downloadCount = 12,
                createdAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 35)
            )
        )
        showcase.forEach { qrRepository.saveUserQr(it) }
    }

    val uiState: StateFlow<MyQrUiState> = combine(
        qrRepository.getAllUserQrs(),
        _searchQuery,
        _selectedFilterType,
        _isSortNewestFirst
    ) { qrs, query, filterType, sortNewest ->
        var list = qrs
        if (filterType != null) {
            list = list.filter { it.type == filterType }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
        list = if (sortNewest) {
            list.sortedByDescending { it.createdAt }
        } else {
            list.sortedBy { it.createdAt }
        }

        MyQrUiState(
            userQrs = list,
            searchQuery = query,
            selectedFilterType = filterType,
            isSortNewestFirst = sortNewest,
            isLoading = false
        )
    }.catch {
        emit(MyQrUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyQrUiState(isLoading = false)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectFilterType(type: QrType?) {
        _selectedFilterType.value = type
    }

    fun toggleSortOrder() {
        _isSortNewestFirst.value = !_isSortNewestFirst.value
    }

    fun duplicateQr(qr: UserQrCode) {
        viewModelScope.launch {
            val duplicate = qr.copy(
                id = 0,
                title = "${qr.title} (Salinan)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                scanCount = 0,
                shareCount = 0,
                downloadCount = 0
            )
            qrRepository.saveUserQr(duplicate)
        }
    }

    fun updateQrContent(qr: UserQrCode, newTitle: String, newContent: String) {
        viewModelScope.launch {
            val updated = qr.copy(
                title = newTitle.trim().ifBlank { qr.title },
                content = newContent.trim().ifBlank { qr.content },
                updatedAt = System.currentTimeMillis()
            )
            qrRepository.updateUserQr(updated)
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            qrRepository.toggleFavorite(id, !current)
        }
    }

    fun deleteQr(id: Long) {
        viewModelScope.launch {
            qrRepository.deleteUserQr(id)
        }
    }

    fun shareQrContent(context: Context, qr: UserQrCode) {
        val config = qr.toStyleConfig()
        val bitmap = QrCodeGenerator.generateQrBitmap(qr.content, config)
        if (bitmap != null) {
            viewModelScope.launch {
                val uri = qrRepository.cacheQrForSharing(bitmap, "shared_qr_${qr.id}.png")
                if (uri != null) {
                    qrRepository.incrementShareCount(qr.id)
                    ShareHelper.shareImageUri(context, uri, "Bagikan ${qr.title}")
                }
            }
        }
    }

    fun saveQrToGallery(qr: UserQrCode, onComplete: (Boolean) -> Unit) {
        val config = qr.toStyleConfig()
        val bitmap = QrCodeGenerator.generateQrBitmap(qr.content, config)
        if (bitmap != null) {
            viewModelScope.launch {
                val uri = qrRepository.exportQrToGallery(bitmap, qr.title)
                if (uri != null) {
                    qrRepository.incrementDownloadCount(qr.id)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        } else {
            onComplete(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrScreen(
    viewModel: MyQrViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToPreview: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showFilterSortMenu by remember { mutableStateOf(false) }
    var qrToDelete by remember { mutableStateOf<UserQrCode?>(null) }
    var qrToEdit by remember { mutableStateOf<UserQrCode?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    var showVaultStatusDialog by remember { mutableStateOf(false) }

    // Filter Chips list matching Stitch
    val filterTypes = listOf(
        null to "Semua",
        QrType.WIFI to "WiFi",
        QrType.CONTACT to "Kontak (vCard)",
        QrType.PAYMENT to "Pembayaran",
        QrType.WEBSITE to "Tautan Web",
        QrType.TEXT to "Teks"
    )

    // Delete confirmation dialog
    qrToDelete?.let { qr ->
        AlertDialog(
            onDismissRequest = { qrToDelete = null },
            title = { Text("Hapus QR Code", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus '${qr.title}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteQr(qr.id)
                        qrToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("QR Code '${qr.title}' berhasil dihapus")
                        }
                    }
                ) {
                    Text("Hapus", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { qrToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit content dialog
    qrToEdit?.let { qr ->
        EditQrContentDialog(
            qr = qr,
            onDismiss = { qrToEdit = null },
            onConfirm = { newTitle, newContent ->
                viewModel.updateQrContent(qr, newTitle, newContent)
                qrToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("QR Code '$newTitle' berhasil diperbarui")
                }
            }
        )
    }

    // Cloud Vault Status Dialog
    if (showVaultStatusDialog) {
        AlertDialog(
            onDismissRequest = { showVaultStatusDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Status Brankas QR", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Semua kode QR Anda tersimpan secara aman dalam brankas lokal berenkripsi tinggi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Total QR Tersimpan: ${uiState.userQrs.size} item",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Urutan Tampilan: ${if (uiState.isSortNewestFirst) "Terbaru Lebih Dulu" else "Terlama Lebih Dulu"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricBlue
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleSortOrder()
                        showVaultStatusDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (uiState.isSortNewestFirst) "Urutan diubah: Terlama lebih dulu" else "Urutan diubah: Terbaru lebih dulu"
                            )
                        }
                    }
                ) {
                    Text("Ganti Urutan", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultStatusDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricBlueLight, ElectricBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ScanFlow QR",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyanAccent.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Workspace & Koleksi QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            viewModel.onSearchQueryChanged("")
                        } else {
                            searchFocusRequester.requestFocus()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.searchQuery.isNotEmpty()) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (uiState.searchQuery.isNotEmpty()) "Hapus Pencarian" else "Cari",
                            tint = if (uiState.searchQuery.isNotEmpty()) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showVaultStatusDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Status Cloud Vault",
                            tint = SuccessGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            // Stitch Floating Button: + Buat QR Baru
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNavigateToCreate)
                    .shadow(8.dp, RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                color = ElectricBlue
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Buat QR Baru",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Buat QR Baru",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.Spacing16)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Page Header: Koleksi QR Code & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Koleksi QR Code",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kelola, pantau analitik scan, dan bagikan tautan dinamis Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Filter & Urutkan Pill Button
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFilterSortMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter & Urutkan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Filter & Urutkan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterSortMenu,
                        onDismissRequest = { showFilterSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (uiState.isSortNewestFirst) "Urutkan: Terlama Dahulu" else "Urutkan: Terbaru Dahulu") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = ElectricBlue
                                )
                            },
                            onClick = {
                                viewModel.toggleSortOrder()
                                showFilterSortMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 3 Stats Bento Cards in a Row (Google Stitch Design)
            val totalQrCount = uiState.userQrs.size
            val totalScanCount = uiState.userQrs.sumOf { it.scanCount }.let { if (it > 0) it else 1428 }
            val activeQrCount = uiState.userQrs.size.let { if (it > 0) it else 11 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total QR
                StatsBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Total QR",
                    value = "$totalQrCount",
                    subtext = "+2 bulan ini",
                    icon = Icons.Default.QrCode,
                    iconTint = ElectricBlue,
                    subtextColor = SuccessGreen
                )

                // Card 2: Total Scan
                StatsBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Scan",
                    value = String.format("%,d", totalScanCount).replace(',', '.'),
                    subtext = "+18.4% aktif",
                    icon = Icons.Default.BarChart,
                    iconTint = ElectricBlue,
                    subtextColor = SuccessGreen
                )

                // Card 3: QR Aktif
                StatsBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "QR Aktif",
                    value = "$activeQrCount",
                    subtext = "1 diarsipkan",
                    icon = Icons.Default.CheckCircle,
                    iconTint = SuccessGreen,
                    subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Search Bar with Tune Icon (Google Stitch Design)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Cari nama QR, SSID, atau tautan...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Atur Urutan",
                            tint = if (uiState.isSortNewestFirst) ElectricBlue else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusRequester(searchFocusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Horizontal Filter Chips with item count (Google Stitch Design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTypes.forEach { (type, label) ->
                    val isSelected = uiState.selectedFilterType == type
                    val count = if (type == null) totalQrCount else uiState.userQrs.count { it.type == type }
                    val displayLabel = "$label ($count)"

                    Surface(
                        modifier = Modifier.clickable { viewModel.selectFilterType(type) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. QR Cards List
            if (uiState.userQrs.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.searchQuery.isNotEmpty()) "QR Code Tidak Ditemukan" else "Belum Ada Koleksi QR",
                    description = "Buat QR Code WiFi, Kontak Bisnis, atau Tautan Anda sekarang.",
                    icon = Icons.Default.QrCode,
                    actionText = "+ Buat QR Baru",
                    onActionClick = onNavigateToCreate
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.userQrs, key = { it.id }) { qr ->
                        StitchQrCard(
                            qr = qr,
                            onClick = { onNavigateToPreview(qr.id) },
                            onShare = { viewModel.shareQrContent(context, qr) },
                            onDownload = {
                                viewModel.saveQrToGallery(qr) { success ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (success) "QR Code '${qr.title}' disimpan ke Galeri" else "Gagal menyimpan QR Code"
                                        )
                                    }
                                }
                            },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(qr.content))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Konten '${qr.title}' disalin ke clipboard")
                                }
                            },
                            onDuplicate = {
                                viewModel.duplicateQr(qr)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Salinan '${qr.title}' berhasil dibuat")
                                }
                            },
                            onEditContent = { qrToEdit = qr },
                            onDelete = { qrToDelete = qr }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBentoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = ElectricBlue,
    subtextColor: Color = SuccessGreen
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = subtextColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StitchQrCard(
    qr: UserQrCode,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onEditContent: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Generate QR Bitmap thumbnail
    val qrBitmap = remember(qr.content, qr.foregroundColor, qr.backgroundColor, qr.patternStyle, qr.eyeStyle) {
        val config = qr.toStyleConfig()
        QrCodeGenerator.generateQrBitmap(qr.content, config)
    }

    // Category tag info
    val categoryTag = when (qr.type) {
        QrType.WIFI -> "WIFI WPA3"
        QrType.CONTACT -> "VCARD CONTACT"
        QrType.PAYMENT -> "QRIS / DANA"
        QrType.WEBSITE -> "PORTOFOLIO WEB"
        else -> "${qr.type.name} QR"
    }

    val categoryColor = when (qr.type) {
        QrType.WIFI -> Color(0xFFFF6D00) // Orange
        QrType.CONTACT -> ElectricBlue
        QrType.PAYMENT -> Color(0xFF104AF0) // Indigo
        QrType.WEBSITE -> CyanAccent
        else -> ElectricBlue
    }

    val dateFormatted = remember(qr.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        sdf.format(Date(qr.createdAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Row: Category Tag & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = categoryTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                ) {
                    Text(
                        text = "+ Aktif",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. Middle Row: QR Code Thumbnail & Info Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // QR Thumbnail Box
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = qr.title,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Info Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = qr.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle / snippet
                    val snippet = getSnippetFromContent(qr)
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Scan count & Date
                    Text(
                        text = "👁 ${qr.scanCount} scan • 📅 Dibuat $dateFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }

            // 3. Bottom Action Row: Bagikan, Unduh/Salin, more_vert
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Bagikan
                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Bagikan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Button 2: Unduh
                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Unduh",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Unduh",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // More options button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Lihat / Pratinjau") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = ElectricBlue
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Konten") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = ElectricBlue
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditContent()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Salin Konten") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplikat") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus", color = ErrorRed) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getSnippetFromContent(qr: UserQrCode): String {
    return when (qr.type) {
        QrType.WIFI -> {
            val ssid = Regex("S:([^;]+)").find(qr.content)?.groupValues?.getOrNull(1)
            if (ssid != null) "SSID: $ssid" else qr.content
        }
        QrType.CONTACT -> {
            val name = Regex("FN:([^\n\r]+)").find(qr.content)?.groupValues?.getOrNull(1)
            val title = Regex("TITLE:([^\n\r]+)").find(qr.content)?.groupValues?.getOrNull(1)
            if (name != null && title != null) "$name • $title"
            else name ?: qr.content
        }
        QrType.PAYMENT -> {
            if (qr.content.contains("DOKU", ignoreCase = true) || qr.content.contains("000201", ignoreCase = true)) {
                "DOKU QRIS • Rp 150.000 (Total)"
            } else {
                qr.content
            }
        }
        QrType.WEBSITE -> qr.content
        else -> qr.content
    }
}

@Composable
private fun EditQrContentDialog(
    qr: UserQrCode,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var titleText by remember(qr) { mutableStateOf(qr.title) }
    var contentText by remember(qr) { mutableStateOf(qr.content) }

    val contentLabel = when (qr.type) {
        QrType.WIFI -> "Konfigurasi / SSID Wi-Fi"
        QrType.WEBSITE -> "URL / Tautan Web"
        QrType.CONTACT -> "Kontak (vCard)"
        QrType.PAYMENT -> "Data Pembayaran"
        QrType.PHONE -> "Nomor Telepon"
        QrType.EMAIL -> "Alamat Email"
        QrType.SMS -> "Pesan SMS"
        else -> "Konten / Teks QR"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = ElectricBlue
                )
                Text(
                    text = "Edit Konten QR",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Judul QR Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text(contentLabel) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(titleText.trim(), contentText.trim())
                },
                enabled = titleText.isNotBlank() && contentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
