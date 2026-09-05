package com.scanflow.qr.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<ScanHistoryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterType: QrType? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteHistoryUseCase: DeleteHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterType = MutableStateFlow<QrType?>(null)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _selectedFilterType,
        _selectedIds
    ) { query, filterType, selectedIds ->
        Triple(query, filterType, selectedIds)
    }.flatMapLatest { (query, filterType, selectedIds) ->
        val flow = when {
            query.isNotEmpty() -> getHistoryUseCase.search(query).catch { emit(emptyList()) }
            filterType != null -> getHistoryUseCase.getByType(filterType).catch { emit(emptyList()) }
            else -> getHistoryUseCase().catch { emit(emptyList()) }
        }
        flow.combine(_selectedIds) { items, selected ->
            HistoryUiState(
                items = items,
                searchQuery = query,
                selectedFilterType = filterType,
                selectedIds = selected,
                isSelectionMode = selected.isNotEmpty(),
                isLoading = false
            )
        }
    }.catch {
        emit(HistoryUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = false)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectFilterType(type: QrType?) {
        _selectedFilterType.value = type
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase.toggleScanFavorite(id, !current)
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelectedItems() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            deleteHistoryUseCase.deleteItems(ids)
            clearSelection()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            deleteHistoryUseCase.clearAll()
            clearSelection()
        }
    }
}
