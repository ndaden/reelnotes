package com.danstudios.reelnotes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.danstudios.reelnotes.domain.model.IngredientItem

@Composable
fun RecipeChecklist(
    ingredients: List<IngredientItem>,
    onToggleIngredient: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧂 Ingrédients (${ingredients.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val checkedCount = ingredients.count { it.isChecked }
                if (checkedCount > 0) {
                    Text(
                        text = "$checkedCount/${ingredients.size} prêts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ingredients.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleIngredient(index, !item.isChecked) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggleIngredient(index, it) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val quantity = listOfNotNull(item.amount, item.unit).filter { it.isNotBlank() }.joinToString(" ")
                    if (quantity.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = quantity,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
