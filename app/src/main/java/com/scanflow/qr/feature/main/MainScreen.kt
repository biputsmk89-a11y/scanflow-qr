package com.scanflow.qr.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.DarkSurface
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.navigation.BottomNavItem
import com.scanflow.qr.feature.generator.CreateQrScreen
import com.scanflow.qr.feature.generator.CreateQrViewModel
import com.scanflow.qr.feature.history.HistoryScreen
import com.scanflow.qr.feature.history.HistoryViewModel
import com.scanflow.qr.feature.home.HomeScreen
import com.scanflow.qr.feature.home.HomeViewModel
import com.scanflow.qr.feature.profile.ProfileScreen

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    createQrViewModel: CreateQrViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToMyQr: () -> Unit,
    onNavigateToResult: (Long) -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(BottomNavItem.Home.route) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                BottomNavItem.items.forEach { item ->
                    if (item == BottomNavItem.Scan) {
                        // Hero Center Scan Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = onNavigateToScan,
                                shape = CircleShape,
                                containerColor = ElectricBlue,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                                modifier = Modifier
                                    .offset(y = (-14).dp)
                                    .size(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(ElectricBlue, CyanAccent))),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isSelected = currentTab == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = item.route },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = { Text(text = item.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BottomNavItem.Home.route -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToScan = onNavigateToScan,
                        onNavigateToCreate = { currentTab = BottomNavItem.Create.route },
                        onNavigateToHistory = { currentTab = BottomNavItem.History.route },
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToMyQr = onNavigateToMyQr,
                        onNavigateToResult = onNavigateToResult
                    )
                }
                BottomNavItem.Create.route -> {
                    CreateQrScreen(
                        viewModel = createQrViewModel,
                        onNavigateBack = { currentTab = BottomNavItem.Home.route },
                        onNavigateToPreview = onNavigateToPreview
                    )
                }
                BottomNavItem.History.route -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onNavigateToResult = onNavigateToResult
                    )
                }
                BottomNavItem.Profile.route -> {
                    ProfileScreen(
                        onNavigateToMyQr = onNavigateToMyQr,
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSecurity = onNavigateToSecurity,
                        onNavigateToAbout = onNavigateToAbout
                    )
                }
            }
        }
    }
}
