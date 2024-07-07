package com.marcos.bitcoinkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private var generateAccountBtn: Button? = null
    private var importAccountFromPrivateKeyBtn: Button? = null
    private var importAccountFromMnemonicBtn: Button? = null
    private var estimateFeeBtn: Button? = null
    private var getBTCBalance: Button? = null
    private var btcTransferBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupContent()
    }
    private fun setupContent(){
        generateAccountBtn = findViewById(R.id.generateAccount)
        estimateFeeBtn = findViewById(R.id.btn_estimateFee)
        importAccountFromPrivateKeyBtn = findViewById(R.id.importAccountFromPrivateKey)
        importAccountFromMnemonicBtn = findViewById(R.id.importAccountFromMnemonic)
        getBTCBalance = findViewById(R.id.getBTCBalance)
        btcTransferBtn = findViewById(R.id.btn_BTCTransfer)

        generateAccountBtn?.setOnClickListener{
            generateAccount()
        }
        estimateFeeBtn?.setOnClickListener{
            estimateFee()
        }
        importAccountFromMnemonicBtn?.setOnClickListener{
            importAccountFromMnemonic()
        }
        importAccountFromPrivateKeyBtn?.setOnClickListener{
            importAccountFromPrivateKey()
        }
        getBTCBalance?.setOnClickListener{
            getBalance()
        }

        btcTransferBtn?.setOnClickListener{
            transfer()
        }
    }

    private fun estimateFee(){
        val intent = Intent(this@MainActivity, EstimateFeeActivity::class.java)
        startActivity(intent)
    }

    private fun generateAccount(){
        val intent = Intent(this@MainActivity, GenerateAccount::class.java)
        startActivity(intent)
    }
    private fun importAccountFromPrivateKey(){
        val intent = Intent(this@MainActivity, ImportAccountFromPrivateKey::class.java)
        startActivity(intent)
    }
    private fun importAccountFromMnemonic(){
        val intent = Intent(this@MainActivity, ImportAccountFromMnemonic::class.java)
        startActivity(intent)
    }
    private fun getBalance(){
        val intent = Intent(this@MainActivity, GetBalanceActivity::class.java)
        startActivity(intent)
    }
    private fun transfer(){
        val intent = Intent(this@MainActivity, TransferActivity::class.java)
        startActivity(intent)
    }

}