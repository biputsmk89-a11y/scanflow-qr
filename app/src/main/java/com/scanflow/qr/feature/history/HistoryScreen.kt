package com.scanflow.qr.feature.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.common.toFormattedDateString
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.EmptyStateView
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowChip
import com.scanflow.qr.core.designsystem.ScanFlowTextField
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToResult: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    val filterTypes = listOf(
        null to "All",
        QrType.WEBSITE to "Websites",
        QrType.WIFI to "Wi-Fi",
        QrType.CONTACT to "Contacts",
        QrType.PHONE to "Phone",
        QrType.TEXT to "Text",
        QrType.BARCODE to "Barcodes"
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to permanently delete all scan records?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
                        Text("${uiState.selectedIds.size} Selected", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Scan History", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = ErrorRed)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    } else if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.Spacing20)
        ) {
            // Search Bar
            ScanFlowTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = "Search Scans",
                placeholder = "Search by title or content...",
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
            ) {
                filterTypes.forEach { (type, label) ->
                    ScanFlowChip(
                        text = label,
                        selected = uiState.selectedFilterType == type,
                        onClick = { viewModel.selectFilterType(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            if (uiState.items.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.searchQuery.isNotEmpty()) "No Matching Records" else "History is Empty",
                    description = "Your scanned QR codes and barcodes will appear here automatically.",
                    icon = Icons.Default.History
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimens.Spacing32),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        HistoryItemCard(
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
                            onLongClick = { viewModel.toggleSelection(item.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(item.id, item.isFavorite) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ScanHistoryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScanFlowCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onLongClick = onLongClick,
        backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(Dimens.Spacing8))
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.Spacing12))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = Dimens.Spacing8)
                    ) {
                        Text(
                            text = item.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item.createdAt.toFormattedDateString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.Spacing4))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isSelectionMode) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
