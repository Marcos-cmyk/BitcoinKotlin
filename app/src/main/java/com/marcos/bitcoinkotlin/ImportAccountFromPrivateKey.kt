package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ImportAccountFromPrivateKey: AppCompatActivity() {
    private var title: TextView? = null
    private var privateKeyEditText: EditText? = null
    private var walletDetailEditText: EditText? = null
    private var importAccountFromPrivateKeyBtn: Button? = null
    private var mWebView: WebView? = null
    private var bitcoin: Bitcoin? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_account_from_privatekey)
        setupContent()
    }
    private fun setupContent() {
        title = findViewById(R.id.title)
        privateKeyEditText = findViewById(R.id.privateKey)
        walletDetailEditText = findViewById(R.id.wallet_detail)
        importAccountFromPrivateKeyBtn = findViewById(R.id.btn_import_account_from_privateKey)
        mWebView =  findViewById(R.id.webView)
        bitcoin = Bitcoin(this, _webView = mWebView!!)
        importAccountFromPrivateKeyBtn?.setOnClickListener{
            importAccountFromPrivateKey()
        }
    }
    private fun importAccountFromPrivateKey() {
        val onCompleted = {result : Boolean ->
            println("bitcoin setup Completed------->>>>>")
            println(result)
            importAccountFromPrivateKeyAction()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted = onCompleted)
        }  else  {
            importAccountFromPrivateKeyAction()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun importAccountFromPrivateKeyAction() {
        val privateKey = privateKeyEditText?.getText().toString();
        if (privateKey.isNotEmpty()) {
            val onCompleted = {state : Boolean, address: String,error: String ->
                this.runOnUiThread {
                    if (state) {
                        val text = "address: " + address
                        walletDetailEditText?.setText(text)
                    } else {
                        walletDetailEditText?.setText(error)
                    }
                }
            }
            walletDetailEditText?.setText("Import Accounting.......")
            bitcoin?.importAccountFromPrivateKey(privateKey,onCompleted = onCompleted)
        }
    }
}