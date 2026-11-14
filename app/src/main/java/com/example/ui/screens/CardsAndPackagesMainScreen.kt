package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CardPackageEntity
import com.example.ui.cardstudio.CardStudioScreen
import com.example.ui.cardstudio.CardStudioViewModel
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.viewmodel.AppViewModel

import androidx.compose.material.icons.filled.Bolt

/**
 * Dedicated Top-Level Screen for "الكروت والباقات" (Cards & Packages)
 * Unifies Network Package Specifications with Quick Generation and the Card Design Studio.
 */
@Composable
fun CardsAndPackagesMainScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    initialSubTab: Int = 0
) {
    val activeRole by viewModel.activeRole.collectAsState()
    val agentPermissions by viewModel.activeAgentPermissions.collectAsState()
    val canAccessStudio = com.example.data.auth.RoleAccessPolicy.canAccessCardStudioGeneration(activeRole)
    val canViewStock = com.example.data.auth.RoleAccessPolicy.canViewAssignedStock(activeRole, agentPermissions)

    if (!canAccessStudio) {
        if (canViewStock) {
            PackagesStockScreen(
                viewModel = viewModel,
                onNavigateToCardStudioWithPackage = null,
                modifier = modifier
            )
        }
        return
    }

    var subTab by rememberSaveable { mutableIntStateOf(initialSubTab) }
    var selectedPackageForStudio by remember { mutableStateOf<CardPackageEntity?>(null) }
    val cardStudioViewModel: CardStudioViewModel = viewModel()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = CyberDarkCardElevated,
            contentColor = Color.White
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("باقات الكروت 📦", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(17.dp), tint = if (subTab == 0) BrandCyanPrimary else Color.LightGray) },
                modifier = Modifier.testTag("subtab_packages_stock")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("توليد الكروت ⚡", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(17.dp), tint = if (subTab == 1) BrandCyanPrimary else Color.LightGray) },
                modifier = Modifier.testTag("subtab_quick_generator")
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text("استوديو التصاميم 🎴", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(17.dp), tint = if (subTab == 2) BrandCyanPrimary else Color.LightGray) },
                modifier = Modifier.testTag("subtab_card_studio")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (subTab) {
                0 -> {
                    PackagesStockScreen(
                        viewModel = viewModel,
                        onNavigateToCardStudioWithPackage = { pkg ->
                            selectedPackageForStudio = pkg
                            cardStudioViewModel.selectPackageForGeneration(pkg)
                            subTab = 1 // Switch directly to Quick Generator
                        }
                    )
                }
                1 -> {
                    QuickCardGeneratorScreen(
                        viewModel = cardStudioViewModel,
                        onNavigateBack = { subTab = 0 },
                        onNavigateToCardStudio = { subTab = 2 }
                    )
                }
                else -> {
                    CardStudioScreen(
                        onNavigateBack = { subTab = 0 },
                        viewModel = cardStudioViewModel
                    )
                }
            }
        }
    }
}
