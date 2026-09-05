package com.scanflow.qr.feature.favorites

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.common.toFormattedDateString
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.EmptyStateView
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.SharingStarted
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteScans: List<ScanHistoryItem> = emptyList(),
    val favoriteUserQrs: List<UserQrCode> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoriteRepository: FavoriteRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = combine(
        favoriteRepository.getAllFavoriteScans().catch { emit(emptyList()) },
        favoriteRepository.getAllFavoriteCreatedQrs().catch { emit(emptyList()) }
    ) { scans, qrs ->
        FavoritesUiState(
            favoriteScans = scans,
            favoriteUserQrs = qrs,
            isLoading = false
        )
    }.catch {
        emit(FavoritesUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState(isLoading = false)
    )

    fun toggleScanFavorite(scanId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase.toggleScanFavorite(scanId, !isFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleUserQrFavorite(qrId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase.toggleUserQrFavorite(qrId, !isFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (Long) -> Unit,
    onNavigateToPreview: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scans (${uiState.favoriteScans.size})", "Created QRs (${uiState.favoriteUserQrs.size})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            if (selectedTabIndex == 0) {
                if (uiState.favoriteScans.isEmpty()) {
                    EmptyStateView(
                        title = "No Favorite Scans",
                        description = "Tap the heart icon on any scanned code to save it here for quick access.",
                        icon = Icons.Default.FavoriteBorder
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Dimens.Spacing32),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
                    ) {
                        items(uiState.favoriteScans, key = { it.id }) { scan ->
                            FavoriteScanCard(
                                scan = scan,
                                onClick = { onNavigateToResult(scan.id) },
                                onToggleFavorite = { viewModel.toggleScanFavorite(scan.id, scan.isFavorite) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.favoriteUserQrs.isEmpty()) {
                    EmptyStateView(
                        title = "No Favorite Created QRs",
                        description = "Favorite QR codes you create in QR Studio to see them pinned here.",
                        icon = Icons.Default.FavoriteBorder
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Dimens.Spacing32),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
                    ) {
                        items(uiState.favoriteUserQrs, key = { it.id }) { qr ->
                            FavoriteUserQrCard(
                                qr = qr,
                                onClick = { onNavigateToPreview(qr.id) },
                                onToggleFavorite = { viewModel.toggleUserQrFavorite(qr.id, qr.isFavorite) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteScanCard(
    scan: ScanHistoryItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScanFlowCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = scan.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite", tint = ErrorRed)
            }
        }
    }
}

@Composable
fun FavoriteUserQrCard(
    qr: UserQrCode,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScanFlowCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.Spacing12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = qr.type.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = qr.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite", tint = ErrorRed)
            }
        }
    }
}
