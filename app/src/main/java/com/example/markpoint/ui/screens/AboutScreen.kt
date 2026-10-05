package com.example.markpoint.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.markpoint.ui.components.NoticeRow
import com.example.markpoint.ui.components.ScreenHeader
import com.example.markpoint.ui.components.SectionTitle

private val FEATURES = listOf(
    "Equal Spacing",
    "Measurement Marks",
    "Fixed Spacing",
    "Start/End Measurements",
    "Multiple Units",
    "Measurement Visualization",
    "Calculation Breakdown",
    "History",
    "Copy Results",
    "Offline"
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("MarkPoint", "Version 1.0")
            Text(
                "An offline measurement utility for calculating evenly spaced marks between two measurements.",
                style = MaterialTheme.typography.bodyLarge
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Features")
                    FEATURES.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(feature, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            NoticeRow(
                Icons.Outlined.WarningAmber,
                "Use calculated positions as a measuring guide. Verify physical measurements before cutting, drilling, or permanent installation.",
                emphasized = true
            )
            Text(
                "MarkPoint performs mathematical spacing calculations only. It works entirely offline and collects no data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
