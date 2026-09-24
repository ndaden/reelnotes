package com.danstudios.reelnotes.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danstudios.reelnotes.domain.model.NoteCategory

@Composable
fun CategoryChipRow(
    selectedCategory: NoteCategory?,
    onlyFavorites: Boolean,
    onCategorySelected: (NoteCategory?) -> Unit,
    onToggleFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Tous" (All) Chip
        FilterChip(
            selected = selectedCategory == null && !onlyFavorites,
            onClick = {
                if (onlyFavorites) onToggleFavorites()
                onCategorySelected(null)
            },
            label = { Text("Tous") }
        )

        // "Favoris" Chip
        FilterChip(
            selected = onlyFavorites,
            onClick = { onToggleFavorites() },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text("Favoris") }
        )

        // Specific category chips
        for (category in NoteCategory.entries) {
            FilterChip(
                selected = selectedCategory == category && !onlyFavorites,
                onClick = {
                    if (onlyFavorites) onToggleFavorites()
                    if (selectedCategory == category) {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(category)
                    }
                },
                label = { Text("${category.iconEmoji} ${category.label}") }
            )
        }
    }
}
