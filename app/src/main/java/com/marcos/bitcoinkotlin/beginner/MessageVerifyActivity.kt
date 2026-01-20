package com.marcos.bitcoinkotlin.beginner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcos.bitcoin.BitcoinV1
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Message Verify Activity
 * Corresponds to iOS MessageVerifyViewController.swift
 */
class MessageVerifyActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("MessageVerify", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MessageVerifyScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun MessageVerifyScreen(bitcoin: BitcoinV1) {
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }
    var signatureText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<Boolean?>(null) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auto-dismiss snackbar
    LaunchedEffect(showToastMessage) {
        showToastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            delay(2000)
            showToastMessage = null
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Verify Message",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Network Selection
            SectionHeader("Network Type")
            NetworkSelector(
                isTestnet = isTestnet,
                onNetworkChanged = { isTestnet = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message Section
            SectionHeader("Message Content")
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Enter message content",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Signature Section
            SectionHeader("Signature (Base64)")
            OutlinedTextField(
                value = signatureText,
                onValueChange = { signatureText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(
                        text = "Enter signature (Base64)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Address Section
            SectionHeader("Address")
            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter address claimed by signer",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Verify Button
            Button(
                onClick = {
                    if (!isVerifying) {
                        val trimmedMessage = messageText.trim()
                        if (trimmedMessage.isEmpty()) {
                            showToastMessage = "Please enter message content"
                            return@Button
                        }
                        
                        val trimmedSignature = signatureText.trim()
                        if (trimmedSignature.isEmpty()) {
                            showToastMessage = "Please enter signature"
                            return@Button
                        }
                        
                        // Clean signature: remove all whitespace and newlines for Base64 validation
                        val cleanedSignature = trimmedSignature.replace(Regex("\\s+"), "")
                        
                        val trimmedAddress = addressText.trim()
                        if (trimmedAddress.isEmpty()) {
                            showToastMessage = "Please enter address"
                            return@Button
                        }
                        
                        scope.launch {
                            verifyMessage(
                                bitcoin = bitcoin,
                                message = trimmedMessage,
                                signature = cleanedSignature,
                                address = trimmedAddress,
                                isTestnet = isTestnet,
                                onVerifyingChanged = { isVerifying = it },
                                onResultReceived = { isValid ->
                                    verificationResult = isValid
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isVerifying) "Verifying..." else "Verify Signature",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Verification Result Section
            verificationResult?.let { isValid ->
                VerificationResultSection(
                    isValid = isValid,
                    address = addressText.trim()
                )
            }
        }
    }
}

@Composable
fun VerificationResultSection(
    isValid: Boolean,
    address: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Header
            Text(
                text = "Verification Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Status Message
            Text(
                text = if (isValid) {
                    "✅ Signature verification successful!\n\nThe signature is indeed from the holder of address $address."
                } else {
                    "❌ Signature verification failed!\n\nThe signature is invalid, possibly because:\n• Message was modified\n• Signature was tampered with\n• Signer does not own the private key for this address"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isValid) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

suspend fun verifyMessage(
    bitcoin: BitcoinV1,
    message: String,
    signature: String,
    address: String,
    isTestnet: Boolean,
    onVerifyingChanged: (Boolean) -> Unit,
    onResultReceived: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    onVerifyingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, Boolean, String?> = bitcoin.verifyMessage(
            message = message,
            signature = signature,
            address = address,
            isTestnet = isTestnet
        )
        val success = result.first
        val isValid = result.second
        val error = result.third
        
        onVerifyingChanged(false)
        
        if (success) {
            onResultReceived(isValid)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onVerifyingChanged(false)
        onError("Error: ${e.message}")
    }
}
