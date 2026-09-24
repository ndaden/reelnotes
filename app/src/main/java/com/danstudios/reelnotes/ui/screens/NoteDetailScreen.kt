package com.danstudios.reelnotes.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.ui.components.RecipeChecklist
import com.danstudios.reelnotes.ui.components.StepList
import com.danstudios.reelnotes.ui.viewmodel.ReelNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: ReelNotesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val note by viewModel.getNoteById(noteId).collectAsState(initial = null)

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRawCaption by remember { mutableStateOf(false) }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentNote = note!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${currentNote.category.iconEmoji} ${currentNote.category.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Favorite toggle
                    IconButton(onClick = { viewModel.toggleFavorite(currentNote) }) {
                        Icon(
                            imageVector = if (currentNote.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (currentNote.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Copy to clipboard
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(currentNote.markdownContent))
                        Toast.makeText(context, "Note copiée dans le presse-papier !", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                    }

                    // Share note
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentNote.title)
                            putExtra(Intent.EXTRA_TEXT, currentNote.markdownContent)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager la note"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager")
                    }

                    // Delete note
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = currentNote.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Author handle
            if (!currentNote.author.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentNote.author,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Direct link to Instagram Reel
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentNote.reelUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Regarder le Reel sur Instagram")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            if (currentNote.summary.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Résumé",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentNote.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recipe Times & Servings Chips
            val data = currentNote.structuredData
            val hasTimes = data.prepTime != null || data.cookTime != null || data.servings != null
            if (hasTimes) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.prepTime?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("⏱️ Prép : $it") }
                        )
                    }
                    data.cookTime?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("🔥 Cuisson : $it") }
                        )
                    }
                    data.servings?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("👥 $it") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recipe Ingredients Checklist
            if (data.ingredients.isNotEmpty()) {
                RecipeChecklist(
                    ingredients = data.ingredients,
                    onToggleIngredient = { index, isChecked ->
                        viewModel.updateIngredientChecked(currentNote, index, isChecked)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Steps / Instructions / Exercises
            if (data.steps.isNotEmpty()) {
                val stepTitle = when (currentNote.category) {
                    NoteCategory.RECIPE -> "👨‍🍳 Préparation"
                    NoteCategory.WORKOUT -> "💪 Exercices"
                    NoteCategory.TUTORIAL -> "🛠️ Étapes"
                    else -> "📋 Étapes"
                }
                StepList(
                    title = stepTitle,
                    steps = data.steps,
                    onToggleStep = { stepNumber, isDone ->
                        viewModel.updateStepDone(currentNote, stepNumber, isDone)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Chef / Pro Tips
            if (data.tips.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✨ Astuces & Conseils",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (tip in data.tips) {
                            Text(
                                text = "• $tip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Key Takeaways
            if (data.keyTakeaways.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📌 Points Clés",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (point in data.keyTakeaways) {
                            Text(
                                text = "• $point",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tags
            if (currentNote.tags.isNotEmpty()) {
                Text(
                    text = "Tags : " + currentNote.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Expandable Original Caption
            if (currentNote.rawCaption.isNotBlank()) {
                OutlinedCard(
                    onClick = { showRawCaption = !showRawCaption },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Légende originale du Reel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (showRawCaption) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showRawCaption) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentNote.rawCaption,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer cette note ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteNote(currentNote.id)
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
