package com.danstudios.reelnotes.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.danstudios.reelnotes.ui.components.InstagramLoginDialog
import com.danstudios.reelnotes.ui.viewmodel.ReelNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ReelNotesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentApiKey by viewModel.geminiApiKey.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val isInstagramLoggedIn by viewModel.isInstagramLoggedIn.collectAsState()

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showPassword by remember { mutableStateOf(false) }

    var isTestingKey by remember { mutableStateOf(false) }
    var keyTestResult by remember { mutableStateOf<Result<String>?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        InstagramLoginDialog(
            onDismiss = {
                showLoginDialog = false
                viewModel.refreshInstagramLoginState()
            },
            onLoginSuccess = {
                showLoginDialog = false
                viewModel.refreshInstagramLoginState()
                Toast.makeText(context, "Connexion Instagram réussie !", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            // Section 1: Gemini AI
            Text(
                text = "Intelligence Artificielle Google Gemini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grâce à Gemini 3.6 Flash Multimodal, l'application écoute la voix du Reel, analyse les images et extrait automatiquement recettes, ingrédients précis et étapes pas à pas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keyTestResult = null
                        },
                        label = { Text("Clé API Google Gemini") },
                        placeholder = { Text("AIzaSy... ou AQ...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Masquer" else "Afficher"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Notice if key format is unusual
                    if (apiKeyInput.isNotBlank() && !apiKeyInput.startsWith("AIzaSy") && !apiKeyInput.startsWith("AQ.") && keyTestResult?.isSuccess != true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attention : les clés Google AI Studio débutent normalement par 'AIzaSy'.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Test result banner
                    keyTestResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val isSuccess = result.isSuccess
                        val msg = if (isSuccess) result.getOrNull() ?: "" else result.exceptionOrNull()?.message ?: ""
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSuccess) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    isTestingKey = true
                                    keyTestResult = null
                                    viewModel.testGeminiKey(apiKeyInput.trim()) { res ->
                                        isTestingKey = false
                                        keyTestResult = res
                                    }
                                } else {
                                    Toast.makeText(context, "Veuillez entrer une clé d'abord", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isTestingKey && apiKeyInput.isNotBlank()
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test...")
                            } else {
                                Text("Tester la clé")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.updateApiKey(apiKeyInput.trim())
                                Toast.makeText(context, "Clé API enregistrée !", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Enregistrer")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Obtenir une clé Gemini gratuite (Google AI Studio)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Instagram Session (Optional)
            Text(
                text = "Session Instagram (Optionnel)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Certains Reels (notamment avec restrictions d'âge ou provenant de comptes privés) ne sont pas accessibles sans compte Instagram.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isInstagramLoggedIn) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isInstagramLoggedIn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isInstagramLoggedIn) "Connecté à Instagram" else "Non connecté",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isInstagramLoggedIn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isInstagramLoggedIn) "Tous les Reels peuvent être analysés" else "Seuls les Reels publics sans restriction sont accessibles",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (isInstagramLoggedIn) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.logoutInstagram()
                                    Toast.makeText(context, "Déconnexion effectuée", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Se déconnecter")
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { showLoginDialog = true }
                            ) {
                                Text("Se connecter à Instagram")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Langue
            Text(
                text = "Langue des résumés",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferredLanguage == "fr",
                            onClick = { viewModel.updateLanguage("fr") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Français (par défaut)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferredLanguage == "en",
                            onClick = { viewModel.updateLanguage("en") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("English")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Données & Démo
            Text(
                text = "Données & Démonstration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ajoutez des exemples de notes pré-remplies (recette de pâtes crémeuses, routine sportive, astuces tech) pour tester l'application.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = {
                            viewModel.reloadSampleData()
                            Toast.makeText(context, "Exemples ajoutés avec succès !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recharger les notes d'exemples")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: À propos
            Text(
                text = "À propos de ReelNotes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ReelNotes v1.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Application Android conçue pour transformer facilement les Reels Instagram en fiches de recettes, tutoriels et notes pratiques avec analyse IA multimodale audio/vidéo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
