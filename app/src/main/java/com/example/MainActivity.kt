package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BienEtreScreen
import com.example.ui.screens.PlanningScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Gestion de la permission de notifications pour Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notifications activées !", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Les rappels et notifications ne pourront pas s'afficher.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // S'assurer que les alarmes existantes sont synchronisées
        val viewModeLocal = viewmodelScope()

        // Demander les permissions
        checkAndRequestNotificationsPermission()

        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf(0) }
                val viewModel: com.example.viewmodel.AppViewModel = viewModel()

                // Synchroniser les alarmes actives au démarrage
                LaunchedEffect(Unit) {
                    viewModel.synchronizeReminders()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("Planning") },
                                icon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Planning")
                                }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("Bien-être") },
                                icon = {
                                    Icon(Icons.Default.Favorite, contentDescription = "Bien-être")
                                }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                label = { Text("Graphiques") },
                                icon = {
                                    Icon(Icons.Default.BarChart, contentDescription = "Graphiques")
                                }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                label = { Text("Rappels") },
                                icon = {
                                    Icon(Icons.Default.Alarm, contentDescription = "Rappels")
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> PlanningScreen(viewModel = viewModel)
                            1 -> BienEtreScreen(viewModel = viewModel)
                            2 -> StatsScreen(viewModel = viewModel)
                            3 -> RemindersScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun viewmodelScope(): Boolean {
        // Simple helper
        return true
    }
}
