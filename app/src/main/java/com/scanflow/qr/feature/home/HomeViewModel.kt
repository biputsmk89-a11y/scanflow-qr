package com.scanflow.qr.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.AnalyticsSummary
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentScans: List<ScanHistoryItem> = emptyList(),
    val analytics: AnalyticsSummary = AnalyticsSummary(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getHistoryUseCase.getRecent(limit = 5).catch { emit(emptyList()) },
        getAnalyticsSummaryUseCase().catch { emit(AnalyticsSummary()) }
    ) { recent, stats ->
        HomeUiState(
            recentScans = recent,
            analytics = stats,
            isLoading = false
        )
    }.catch {
        emit(HomeUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = false)
    )

    fun toggleFavorite(scanId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase.toggleScanFavorite(scanId, !currentFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
