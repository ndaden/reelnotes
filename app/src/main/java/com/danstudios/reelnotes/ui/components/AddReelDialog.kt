package com.danstudios.reelnotes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun AddReelDialog(
    onDismiss: () -> Unit,
    onSubmit: (url: String, manualCaption: String?) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var captionInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un Reel")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Collez le lien d'un Reel Instagram. L'IA écoutera et lira la vidéo pour créer vos notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Lien du Reel Instagram") },
                    placeholder = { Text("https://www.instagram.com/reel/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                urlInput = clipText
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Coller lien"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Légende ou recette (facultatif)") },
                    placeholder = { Text("Utile si le compte est privé ou restreint...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank() && clipText != urlInput) {
                                captionInput = clipText
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Coller texte"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        onSubmit(
                            urlInput.trim(),
                            captionInput.trim().ifBlank { null }
                        )
                    }
                },
                enabled = urlInput.isNotBlank()
            ) {
                Text("Transformer en notes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
