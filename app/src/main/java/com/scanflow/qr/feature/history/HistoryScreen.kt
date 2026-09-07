package com.scanflow.qr.feature.history

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Wifi
import com.scanflow.qr.ScanFlowApplication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.EmptyStateView
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToResult: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var onlyFavoritesFilter by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var itemsToExport by remember { mutableStateOf<List<ScanHistoryItem>>(emptyList()) }

    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && itemsToExport.isNotEmpty()) {
            viewModel.exportCsvToUri(context, uri, itemsToExport) { success ->
                Toast.makeText(
                    context,
                    if (success) "Berkas CSV berhasil disimpan" else "Gagal menyimpan berkas CSV",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Filter Chips list matching Stitch
    val filterTypes = listOf(
        null to "All Scans",
        QrType.WEBSITE to "Website",
        QrType.WIFI to "WiFi",
        QrType.CONTACT to "Contact",
        QrType.PAYMENT to "Payment",
        QrType.TEXT to "Text"
    )

    // Filter items based on active type and favorites filter
    val displayedItems = remember(uiState.items, onlyFavoritesFilter) {
        if (onlyFavoritesFilter) {
            uiState.items.filter { it.isFavorite }
        } else {
            uiState.items
        }
    }

    // Group items by Date (TODAY, YESTERDAY, OLDER)
    val groupedItems = remember(displayedItems) {
        groupHistoryByDate(displayedItems)
    }

    // Confirmation dialog: Clear all
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all scan records? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("All scan history records have been cleared")
                        }
                    }
                ) {
                    Text("Clear All", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog: Delete selected
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Delete Selected Items", fontWeight = FontWeight.Bold) },
            text = { Text("Delete ${uiState.selectedIds.size} selected scan record(s)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val count = uiState.selectedIds.size
                        viewModel.deleteSelectedItems()
                        showDeleteSelectedDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("$count scan record(s) deleted")
                        }
                    }
                ) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            text = "${uiState.selectedIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Scan History",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = ElectricBlue
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        // Select all button
                        IconButton(onClick = {
                            val allIds = displayedItems.map { it.id }
                            viewModel.selectAll(allIds)
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = ElectricBlue
                            )
                        }

                        // Export selected items button
                        if (uiState.selectedIds.isNotEmpty()) {
                            IconButton(onClick = {
                                val selectedItems = displayedItems.filter { uiState.selectedIds.contains(it.id) }
                                viewModel.shareCsv(context, selectedItems)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Bagikan CSV Terpilih",
                                    tint = ElectricBlue
                                )
                            }
                        }

                        // Delete button
                        if (uiState.selectedIds.isNotEmpty()) {
                            IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = ErrorRed
                                )
                            }
                        }

                        // Cancel button
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Done", color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Normal mode: Export CSV button with dropdown
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Ekspor CSV",
                                    tint = ElectricBlue
                                )
                            }

                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bagikan Semua Riwayat (CSV)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = ElectricBlue)
                                    },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.shareCsv(context, displayedItems)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simpan File CSV (.csv)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TableChart, contentDescription = null, tint = ElectricBlue)
                                    },
                                    onClick = {
                                        showExportMenu = false
                                        itemsToExport = displayedItems
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        (context.applicationContext as? ScanFlowApplication)?.appLockManager?.setTemporarilyBypassed(true)
                                        createCsvDocumentLauncher.launch("ScanFlow_History_$timeStamp.csv")
                                    }
                                )
                            }
                        }

                        // Select button matching Stitch
                        TextButton(
                            onClick = {
                                if (displayedItems.isNotEmpty()) {
                                    viewModel.setSelectionMode(true)
                                }
                            }
                        ) {
                            Text(
                                text = "Select",
                                color = ElectricBlue,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Search Bar (Google Stitch Design)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = "Search history...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(ElectricBlue),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box {
                            IconButton(
                                onClick = { showFilterMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (onlyFavoritesFilter) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (onlyFavoritesFilter) "Show All Scans" else "Show Favorites Only") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (onlyFavoritesFilter) Icons.Default.History else Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = if (onlyFavoritesFilter) ElectricBlue else ErrorRed
                                        )
                                    },
                                    onClick = {
                                        onlyFavoritesFilter = !onlyFavoritesFilter
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Filter Chips (Google Stitch Design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTypes.forEach { (type, label) ->
                    val isSelected = uiState.selectedFilterType == type
                    Surface(
                        modifier = Modifier.clickable { viewModel.selectFilterType(type) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyanAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)) else null
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. History List Grouped by Date
            if (displayedItems.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.searchQuery.isNotEmpty()) "No Matching Records" else "History is Empty",
                    description = if (onlyFavoritesFilter) "No favorite scans saved yet." else "Your scanned QR codes and barcodes will appear here automatically.",
                    icon = Icons.Default.History
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedItems.forEach { (header, itemsInGroup) ->
                        item {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(itemsInGroup, key = { it.id }) { item ->
                            StitchHistoryItemCard(
                                item = item,
                                isSelected = uiState.selectedIds.contains(item.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        onNavigateToResult(item.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.setSelectionMode(true)
                                    viewModel.toggleSelection(item.id)
                                },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(item.id, item.isFavorite)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (!item.isFavorite) "Added to Favorites" else "Removed from Favorites"
                                        )
                                    }
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(item.content))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Content copied to clipboard")
                                    }
                                },
                                onShare = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, item.content)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share QR Content"))
                                },
                                onDelete = {
                                    viewModel.deleteItem(item.id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Scan record deleted")
                                    }
                                }
                            )
                        }
                    }

                    // Clear All History Button matching Stitch
                    if (!uiState.isSelectionMode && displayedItems.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.clickable { showClearDialog = true },
                                    shape = RoundedCornerShape(50),
                                    color = ElectricBlue.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Clear All History",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Clear All History",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StitchHistoryItemCard(
    item: ScanHistoryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getTypeColor(item.type)
    val categoryIcon = getTypeIcon(item.type)
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkbox in Selection Mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ElectricBlue
                    )
                )
            } else {
                // Category Left Accent Bar & Icon Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Vertical Accent Bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(categoryColor)
                    )

                    // Icon Container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = item.type.name,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title.ifEmpty { item.content },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = getItemStatusSubtitle(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trailing Actions (More menu)
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (item.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (item.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Content") },
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
                            text = { Text("Share") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ErrorRed) },
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

private fun groupHistoryByDate(items: List<ScanHistoryItem>): Map<String, List<ScanHistoryItem>> {
    val groups = LinkedHashMap<String, MutableList<ScanHistoryItem>>()
    val now = Calendar.getInstance()

    for (item in items) {
        val header = getRelativeDateHeader(item.createdAt, now)
        val list = groups.getOrPut(header) { mutableListOf() }
        list.add(item)
    }
    return groups
}

private fun getRelativeDateHeader(timestamp: Long, now: Calendar): String {
    val itemDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameDay = now.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == itemDate.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "TODAY"

    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == itemDate.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "YESTERDAY"

    return "OLDER"
}

private fun formatItemTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getItemStatusSubtitle(item: ScanHistoryItem): String {
    val time = formatItemTime(item.createdAt)
    val action = when (item.type) {
        QrType.WEBSITE -> "Scanned"
        QrType.WIFI -> "Connected"
        QrType.CONTACT -> "Added to Contacts"
        QrType.PAYMENT -> "Payment"
        QrType.PHONE -> "Phone Call"
        QrType.EMAIL -> "Email Draft"
        QrType.SMS -> "SMS Message"
        QrType.LOCATION -> "Location"
        else -> "Scanned"
    }
    return "$time • $action"
}

private fun getTypeColor(type: QrType): Color {
    return when (type) {
        QrType.WEBSITE -> Color(0xFF00E3FD) // CyanAccent
        QrType.WIFI -> Color(0xFFFF6D00) // Orange Accent
        QrType.CONTACT -> Color(0xFF0052FF) // ElectricBlue
        QrType.PAYMENT -> Color(0xFF104AF0) // Indigo
        QrType.PHONE, QrType.EMAIL, QrType.SMS -> Color(0xFF00C853) // Green
        else -> Color(0xFF7C4DFF) // Purple
    }
}

private fun getTypeIcon(type: QrType): ImageVector {
    return when (type) {
        QrType.WEBSITE -> Icons.Default.Language
        QrType.WIFI -> Icons.Default.Wifi
        QrType.CONTACT -> Icons.Default.Person
        QrType.PAYMENT -> Icons.Default.Payments
        QrType.PHONE -> Icons.Default.Phone
        QrType.EMAIL -> Icons.Default.Email
        QrType.SMS -> Icons.AutoMirrored.Filled.Notes
        else -> Icons.Default.QrCode
    }
}
