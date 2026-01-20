package com.marcos.bitcoinkotlin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.bitcoinkotlin.advanced.MultisigGenerateActivity
import com.marcos.bitcoinkotlin.advanced.MultisigTransferActivity
import com.marcos.bitcoinkotlin.beginner.*
import com.marcos.bitcoinkotlin.intermediate.*
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme

/**
 * Main Activity - Feature List
 * Corresponds to iOS ViewController.swift
 */
class MainActivity : ComponentActivity() {
    
    // Feature data model
    data class Feature(
        val id: Int,
        val title: String,
        val subtitle: String
    )
    
    // Sections
    private val sections = listOf(
        "🔗 Bitcoin Testnet Faucet",
        "📚 Primary Features",
        "🔧 Intermediate Features",
        "🚀 Advanced Features"
    )
    
    // Features grouped by sections
    private val features: List<List<Feature>> = listOf(
        // Bitcoin Testnet Faucet
        listOf(
            Feature(id = 100, title = "Bitcoin Testnet Faucet", subtitle = "Get Testnet BTC")
        ),
        // Primary Features
        listOf(
            Feature(id = 1, title = "Generate Wallet", subtitle = "Generate New Wallet"),
            Feature(id = 2, title = "Import Wallet", subtitle = "Import Wallet"),
            Feature(id = 3, title = "UTXO Query", subtitle = "UTXO Check"),
            Feature(id = 4, title = "Address Validator", subtitle = "Address Validator"),
            Feature(id = 5, title = "One-click Transfer", subtitle = "One-click Transfer"),
            Feature(id = 12, title = "Batch Transfer", subtitle = "Batch Transfer"),
            Feature(id = 21, title = "Sign Message", subtitle = "BIP322 Sign Message"),
            Feature(id = 22, title = "Verify Message", subtitle = "BIP322 Verify Message")
        ),
        // Intermediate Features
        listOf(
            Feature(id = 7, title = "HTLC: Address Generation", subtitle = "HTLC Address Generation"),
            Feature(id = 8, title = "HTLC: Unlock & Transfer", subtitle = "HTLC Unlock & Transfer"),
            Feature(id = 9, title = "No-Sig Script: Generation", subtitle = "No-Signature Script Generation"),
            Feature(id = 10, title = "No-Sig Script: Unlock", subtitle = "No-Signature Script Unlock")
        ),
        // Advanced Features
        listOf(
            Feature(id = 13, title = "Multisig: Address Generation", subtitle = "N-of-M Multisig Address"),
            Feature(id = 14, title = "Multisig: Transfer", subtitle = "Multisig Transfer")
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeatureListScreen(
                        sections = sections,
                        features = features,
                        onFeatureClick = { feature ->
                            navigateToFeature(feature)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Navigate to feature based on feature ID
     * Corresponds to iOS switch case logic
     */
    private fun navigateToFeature(feature: Feature) {
        when (feature.id) {
            100 -> {
                // Bitcoin Testnet Faucet - Open testnet faucet website
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coinfaucet.eu/en/btc-testnet/"))
                startActivity(intent)
            }
            
            1 -> {
                // Generate Wallet
                val intent = Intent(this, GenerateWalletActivity::class.java)
                startActivity(intent)
            }
            
            2 -> {
                // Import Wallet
                val intent = Intent(this, ImportWalletActivity::class.java)
                startActivity(intent)
            }
            
            3 -> {
                // UTXO Query
                val intent = Intent(this, QueryUTXOActivity::class.java)
                startActivity(intent)
            }
            
            4 -> {
                // Address Validator
                val intent = Intent(this, AddressValidatorActivity::class.java)
                startActivity(intent)
            }
            
            5 -> {
                // One-click Transfer
                val intent = Intent(this, OneClickTransferActivity::class.java)
                startActivity(intent)
            }
            
            12 -> {
                // Batch Transfer
                val intent = Intent(this, BatchTransferActivity::class.java)
                startActivity(intent)
            }
            
            7 -> {
                // HTLC: Address Generation
                val intent = Intent(this, HTLCGenerateActivity::class.java)
                startActivity(intent)
            }
            
            8 -> {
                // HTLC: Unlock & Transfer
                val intent = Intent(this, HTLCUnlockActivity::class.java)
                startActivity(intent)
            }
            
            9 -> {
                // No-Sig Script: Generation
                val intent = Intent(this, NoSigScriptGenerateActivity::class.java)
                startActivity(intent)
            }
            
            10 -> {
                // No-Sig Script: Unlock
                val intent = Intent(this, NoSigScriptUnlockActivity::class.java)
                startActivity(intent)
            }
            
            13 -> {
                // Multisig: Address Generation
                val intent = Intent(this, MultisigGenerateActivity::class.java)
                startActivity(intent)
            }
            
            14 -> {
                // Multisig: Transfer
                val intent = Intent(this, MultisigTransferActivity::class.java)
                startActivity(intent)
            }
            
            21 -> {
                // Sign Message
                val intent = Intent(this, MessageSignActivity::class.java)
                startActivity(intent)
            }
            
            22 -> {
                // Verify Message
                val intent = Intent(this, MessageVerifyActivity::class.java)
                startActivity(intent)
            }
            
            else -> {
                // Feature not implemented yet
                android.util.Log.d("MainActivity", "Feature ID ${feature.id} not implemented yet")
            }
        }
    }
}

/**
 * Feature List Screen Composable
 */
@Composable
fun FeatureListScreen(
    sections: List<String>,
    features: List<List<MainActivity.Feature>>,
    onFeatureClick: (MainActivity.Feature) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        sections.forEachIndexed { sectionIndex, sectionTitle ->
            // Section Header
            item {
                SectionHeader(title = sectionTitle)
            }
            
            // Section Items
            items(features[sectionIndex]) { feature ->
                FeatureItem(
                    feature = feature,
                    onClick = { onFeatureClick(feature) }
                )
            }
        }
    }
}

/**
 * Section Header Composable
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Feature Item Composable
 */
@Composable
fun FeatureItem(
    feature: MainActivity.Feature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
