package com.scanflow.qr.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.domain.model.AnalyticsSummary
import com.scanflow.qr.domain.model.DayActivity
import com.scanflow.qr.domain.model.HourlyStats
import com.scanflow.qr.domain.model.LocationStats
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.TopQrAnalyticsItem
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
) : ViewModel() {

    private val _timeRange = MutableStateFlow("7 Hari Terakhir")
    val timeRange: StateFlow<String> = _timeRange.asStateFlow()

    private val _activityInterval = MutableStateFlow("Harian")
    val activityInterval: StateFlow<String> = _activityInterval.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<AnalyticsSummary> = combine(_timeRange, _activityInterval) { range, interval ->
        range to interval
    }.flatMapLatest { (range, interval) ->
        getAnalyticsSummaryUseCase(timeRange = range, interval = interval)
    }.catch {
        emit(AnalyticsSummary())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsSummary()
    )

    fun setTimeRange(range: String) {
        _timeRange.value = range
    }

    fun setActivityInterval(interval: String) {
        _activityInterval.value = interval
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.analytics.collectAsState()
    val selectedTimeRange by viewModel.timeRange.collectAsState()
    val selectedInterval by viewModel.activityInterval.collectAsState()

    var showTimeRangeMenu by remember { mutableStateOf(false) }
    var showHourlyDialog by remember { mutableStateOf(false) }
    var selectedDayTooltip by remember { mutableStateOf<String?>(null) }

    val timeRangeOptions = listOf(
        "7 Hari Terakhir",
        "30 Hari Terakhir",
        "Bulan Ini",
        "Semua Waktu"
    )

    // Hourly Breakdown Dialog
    if (showHourlyDialog) {
        HourlyBreakdownDialog(
            hourlyStats = stats.hourlyBreakdown,
            onDismiss = { showHourlyDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Statistik & Analisis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ringkasan performa QR code",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
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
                    // Date Range Selector Pill Button (Google Stitch Design)
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showTimeRangeMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedTimeRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimeRangeMenu,
                            onDismissRequest = { showTimeRangeMenu = false }
                        ) {
                            timeRangeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option,
                                                fontWeight = if (option == selectedTimeRange) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == selectedTimeRange) ElectricBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (option == selectedTimeRange) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = ElectricBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setTimeRange(option)
                                        showTimeRangeMenu = false
                                    }
                                )
                            }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Spacing16)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. KPI Overview (4 Cards in a 2x2 Grid - Google Stitch Design)
            KpiOverviewGrid(stats = stats)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Aktivitas Scan & Trend Bar Chart (Google Stitch Design)
            ScanActivityCard(
                stats = stats,
                selectedInterval = selectedInterval,
                onIntervalChanged = { viewModel.setActivityInterval(it) },
                selectedDayTooltip = selectedDayTooltip,
                onDaySelected = { day ->
                    selectedDayTooltip = if (selectedDayTooltip == day) null else day
                },
                onViewHourlyDetails = { showHourlyDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. QR Terpopuler (Top Performing QR Codes)
            TopQrsSection(topQrs = stats.topQrs)

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Perangkat & Wilayah Teratas (Segmented Bar & Location Progress Bars)
            DevicesAndLocationsSection(
                deviceStats = stats.deviceStats,
                totalScans = stats.totalScans,
                locations = stats.topLocations
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 1: 4 KPI Overview Grid (2x2)
// -----------------------------------------------------------------------------------------
@Composable
private fun KpiOverviewGrid(stats: AnalyticsSummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Scan
            val trendTotalText = if (stats.scansToday > 0) "+${stats.scansToday}" else "${stats.totalScans}"
            val trendTotalLabel = if (stats.scansToday > 0) "hari ini" else "total"
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Scan",
                value = String.format(Locale.GERMANY, "%,d", stats.totalScans),
                icon = Icons.Default.QrCodeScanner,
                iconTint = ElectricBlue,
                iconBg = ElectricBlue.copy(alpha = 0.12f),
                trendText = trendTotalText,
                trendLabel = trendTotalLabel,
                trendPositive = true,
                isTrend = stats.scansToday > 0
            )

            // Card 2: Pengunjung Unik
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Pengunjung Unik",
                value = String.format(Locale.GERMANY, "%,d", stats.uniqueVisitors),
                icon = Icons.Default.Group,
                iconTint = Color(0xFF006875),
                iconBg = Color(0xFF006875).copy(alpha = 0.12f),
                trendText = if (stats.uniqueVisitors > 0) "${stats.uniqueVisitors} entitas" else "0 entitas",
                trendLabel = "tercatat",
                trendPositive = true,
                isTrend = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: QR Aktif
            val active = stats.activeQrCount - stats.pausedQrCount.coerceAtMost(stats.activeQrCount)
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "QR Aktif",
                value = "${stats.activeQrCount}",
                icon = Icons.Default.Link,
                iconTint = ElectricBlue,
                iconBg = ElectricBlue.copy(alpha = 0.12f),
                trendText = "$active aktif • ${stats.pausedQrCount} jeda",
                trendLabel = "",
                trendPositive = true,
                isTrend = false,
                hasGreenDot = true
            )

            // Card 4: Rata-rata/Hari
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Rata-rata/Hari",
                value = "${stats.dailyAverage}",
                icon = Icons.Default.Insights,
                iconTint = Color(0xFF006875),
                iconBg = Color(0xFF00E3FD).copy(alpha = 0.2f),
                trendText = if (stats.dailyAverage > 0) "${stats.dailyAverage}/hari" else "0/hari",
                trendLabel = "estimasi",
                trendPositive = true,
                isTrend = false
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    trendText: String,
    trendLabel: String,
    trendPositive: Boolean,
    isTrend: Boolean,
    hasGreenDot: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isTrend) {
                    val trendColor = if (trendPositive) Color(0xFF00875A) else Color(0xFFBA1A1A)
                    Icon(
                        imageVector = if (trendPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = trendText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                    if (trendLabel.isNotBlank()) {
                        Text(
                            text = trendLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    if (hasGreenDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00875A))
                        )
                    }
                    Text(
                        text = trendText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 2: Aktivitas Scan & Trend Bar Chart (Google Stitch Design)
// -----------------------------------------------------------------------------------------
@Composable
private fun ScanActivityCard(
    stats: AnalyticsSummary,
    selectedInterval: String,
    onIntervalChanged: (String) -> Unit,
    selectedDayTooltip: String?,
    onDaySelected: (String) -> Unit,
    onViewHourlyDetails: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Title & Segmented Toggle (Harian / Mingguan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aktivitas Scan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tren interaksi pemindaian harian",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Segmented Toggle Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        listOf("Harian", "Mingguan").forEach { tab ->
                            val isSelected = tab == selectedInterval
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onIntervalChanged(tab) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Canvas Area with Dashed Guidelines & 7 Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Dashed Guidelines
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(3) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 1.dp
                        )
                    }
                }

                // Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.dailyActivities.forEach { activity ->
                        DayBarItem(
                            activity = activity,
                            showTooltip = selectedDayTooltip == activity.dayName,
                            onClick = { onDaySelected(activity.dayName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Hari Tersibuk + Link Lihat Rincian Jam
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    )
                    Text(
                        text = "Hari tersibuk: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stats.busiestDayText.ifBlank { "Belum ada data scan" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Lihat Rincian Jam",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onViewHourlyDetails() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DayBarItem(
    activity: DayActivity,
    showTooltip: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxHeight()
            .width(42.dp)
            .clickable { onClick() }
    ) {
        // Floating Tooltip Badge
        Box(
            modifier = Modifier
                .height(28.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (activity.isPeak) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricBlue,
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = "${activity.scanCount}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (showTooltip) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${activity.scanCount} scan",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bar Fill
        val barHeight = if (activity.scanCount > 0) {
            (activity.heightRatio * 110).dp.coerceIn(16.dp, 120.dp)
        } else {
            8.dp
        }
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    when {
                        activity.isPeak -> ElectricBlue
                        activity.scanCount > 0 -> ElectricBlue.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Day Label (Sen, Sel, Rab, Kam, etc.)
        Text(
            text = activity.dayName,
            fontSize = 11.sp,
            fontWeight = if (activity.isPeak) FontWeight.Bold else FontWeight.Medium,
            color = if (activity.isPeak) ElectricBlue else MaterialTheme.colorScheme.outline
        )
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 3: QR Terpopuler Section (Top 3 Performing QR Codes)
// -----------------------------------------------------------------------------------------
@Composable
private fun TopQrsSection(topQrs: List<TopQrAnalyticsItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "QR Terpopuler",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "3 Teratas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Semua",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (topQrs.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum Ada Data QR Terpopuler",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pindai atau buat QR Code baru untuk melihat performa di sini",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topQrs.take(3).forEach { item ->
                    TopQrCardItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun TopQrCardItem(item: TopQrAnalyticsItem) {
    val (icon, iconTint, iconBg, barColor) = when (item.type) {
        QrType.WIFI -> Quadruple(Icons.Default.Wifi, Color(0xFF006875), Color(0xFF00E3FD).copy(alpha = 0.2f), Color(0xFF006875))
        QrType.CONTACT -> Quadruple(Icons.Default.Badge, Color(0xFF993100), Color(0xFF993100).copy(alpha = 0.12f), Color(0xFFC24100))
        else -> Quadruple(Icons.Default.Language, ElectricBlue, ElectricBlue.copy(alpha = 0.12f), ElectricBlue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Information & Progress Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.scanCount} ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "scan",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (item.barPercent / 100f).coerceIn(0.05f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.sharePercent}% share",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// -----------------------------------------------------------------------------------------
// COMPONENT 4: Perangkat & Wilayah Teratas (Segmented Bar & Location Progress Bars)
// -----------------------------------------------------------------------------------------
@Composable
private fun DevicesAndLocationsSection(
    deviceStats: com.scanflow.qr.domain.model.DeviceStats,
    totalScans: Int,
    locations: List<LocationStats>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Tipe Perangkat (Segmented Bar Chart Modern - Google Stitch Design)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tipe Perangkat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Total ${String.format(Locale.GERMANY, "%,d", totalScans)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Bar (iOS vs Android)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (totalScans > 0) {
                        val iosFraction = (deviceStats.iosPercent / 100f).coerceIn(0f, 1f)
                        if (iosFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = iosFraction)
                                    .fillMaxHeight()
                                    .background(ElectricBlue)
                            )
                        }
                        if (deviceStats.androidPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF00E3FD))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                        )
                        Text(
                            text = "iOS:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${deviceStats.iosPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "(${deviceStats.iosCount})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E3FD))
                        )
                        Text(
                            text = "Android:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${deviceStats.androidPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "(${deviceStats.androidCount})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Card 2: Wilayah Teratas (3 Kota Utama)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF006875),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Wilayah Teratas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "3 Kota Utama",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    locations.forEach { loc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${loc.rank}. ${loc.cityName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = loc.percent / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(ElectricBlue)
                                    )
                                }
                                Text(
                                    text = "${loc.percent}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 5: Hourly Breakdown Dialog (Lihat Rincian Jam)
// -----------------------------------------------------------------------------------------
@Composable
private fun HourlyBreakdownDialog(
    hourlyStats: List<HourlyStats>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Rincian Aktivitas per Jam",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pola pemindaian berdasarkan rentang waktu",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                hourlyStats.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.timeWindow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.scanCount} scan (${item.percent}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (item.percent / 35f).coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ElectricBlue)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Tutup",
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
