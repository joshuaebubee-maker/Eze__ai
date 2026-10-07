package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GroqResult
import com.example.ui.components.EzeOrb
import com.example.ui.components.OrbState
import com.example.ui.theme.EzeBackground
import com.example.ui.theme.EzeBorder
import com.example.ui.theme.EzePrimary
import com.example.ui.theme.EzeSecondary
import com.example.ui.theme.EzeSurface
import com.example.ui.theme.EzeSurfaceVariant
import com.example.ui.theme.EzeTertiary
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: AssistantViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // Permissions state
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }

    // Groq configuration in onboarding
    var apiKey by remember { mutableStateOf(viewModel.settingsRepository.getGroqApiKey() ?: "") }
    var model by remember { mutableStateOf(viewModel.settingsRepository.getGroqModel()) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    // Wake word
    var enableWakeWord by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EzeBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step indicator dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentStep) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentStep) EzePrimary
                            else if (index < currentStep) EzePrimary.copy(alpha = 0.5f)
                            else EzeSurfaceVariant
                        )
                )
            }
        }

        // Animated Content for Steps
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_steps",
            modifier = Modifier.weight(1f)
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    0 -> {
                        // Step 1: Welcome
                        EzeOrb(state = OrbState.IDLE, size = 160.dp)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Meet EZE",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EzePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your genuine native Android personal assistant & AI companion. Built for real phone actions, natural speech, and lightweight high-speed intelligence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    1 -> {
                        // Step 2: Essential Permissions
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = EzePrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Microphone & Voice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "EZE needs microphone access to understand your speech and listen for the wake phrase \"Hey EZE\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (micGranted) EzeTertiary else EzePrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("grant_mic_button")
                        ) {
                            Icon(
                                imageVector = if (micGranted) Icons.Default.CheckCircle else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (micGranted) "Microphone Granted" else "Grant Microphone")
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (notifGranted) EzeTertiary else EzeSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("grant_notif_button")
                            ) {
                                Icon(
                                    imageVector = if (notifGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (notifGranted) "Notifications Granted" else "Enable Notifications")
                            }
                        }
                    }

                    2 -> {
                        // Step 3: Assistant Integration
                        Icon(
                            imageVector = Icons.Default.Assistant,
                            contentDescription = null,
                            tint = EzeSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Set as Default Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can make EZE your system default assistant to trigger it from the home button or swipe gesture across Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallback)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Assistant Settings")
                        }
                    }

                    3 -> {
                        // Step 4: Groq API Key
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = EzePrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Groq Intelligence Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your Groq API key (from console.groq.com) for high-speed AI responses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Groq API Key") },
                            placeholder = { Text("gsk_...") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_api_key_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model Name") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_model_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    val res = viewModel.groqClient.testConnection(apiKey, model)
                                    isTesting = false
                                    testResultText = when (res) {
                                        is GroqResult.Success -> "Connection successful!"
                                        is GroqResult.Error -> res.message
                                    }
                                }
                            },
                            enabled = !isTesting && apiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Test Connection")
                            }
                        }

                        if (testResultText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = testResultText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testResultText?.contains("successful") == true) EzeTertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    4 -> {
                        // Step 5: Wake Word & Ready
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = EzePrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "EZE is Ready!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You are all set to experience real AI voice assistance on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = EzeSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EzeBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable \"Hey EZE\" Wake Word", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Runs lightweight acoustic monitor in background",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = enableWakeWord,
                                    onCheckedChange = { enableWakeWord = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EzePrimary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Buttons (Back & Next / Finish)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            if (currentStep < totalSteps - 1) {
                Button(
                    onClick = {
                        if (currentStep == 3 && apiKey.isNotBlank()) {
                            viewModel.settingsRepository.saveGroqApiKey(apiKey)
                            viewModel.settingsRepository.saveGroqModel(model)
                        }
                        currentStep++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EzePrimary),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text("Next", color = EzeBackground, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (apiKey.isNotBlank()) {
                            viewModel.settingsRepository.saveGroqApiKey(apiKey)
                            viewModel.settingsRepository.saveGroqModel(model)
                        }
                        if (enableWakeWord) {
                            viewModel.toggleWakeWord(true)
                        }
                        viewModel.settingsRepository.setOnboardingDone(true)
                        onFinishOnboarding()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EzePrimary),
                    modifier = Modifier.testTag("onboarding_finish_button")
                ) {
                    Text("Start Using EZE", color = EzeBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
