package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ImportAccountFromMnemonic: AppCompatActivity() {
    private var title: TextView? = null
    private var mnemonicEditText: EditText? = null
    private var walletDetailEditText: EditText? = null
    private var importAccountFromMnemonicBtn: Button? = null
    private var mWebView: WebView? = null
    private var bitcoin: Bitcoin? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_account_from_mnemonic)
        setupContent()
    }
    private fun setupContent() {
        title = findViewById(R.id.title)
        mnemonicEditText = findViewById(R.id.mnemonic)
        walletDetailEditText = findViewById(R.id.wallet_detail)
        importAccountFromMnemonicBtn = findViewById(R.id.btn_import_account_from_mnemonic)
        mWebView =  findViewById(R.id.webView)
        bitcoin = Bitcoin(this, _webView = mWebView!!)
        importAccountFromMnemonicBtn?.setOnClickListener{
            importAccountFromMnemonic()
        }
    }
    private fun importAccountFromMnemonic() {
        val onCompleted = {result : Boolean ->
            println("bitcoin setup Completed------->>>>>")
            println(result)
            importAccountFromMnemonicAction()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted = onCompleted)
        }  else  {
            importAccountFromMnemonicAction()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun importAccountFromMnemonicAction() {
        val mnemonic = mnemonicEditText?.getText().toString();
        if (mnemonic.isNotEmpty()) {
            val onCompleted = {state : Boolean, address: String,privateKey: String,error: String ->
                this.runOnUiThread {
                    if (state) {
                        val text =
                            "address: " + address + "\n\n" +
                            "privateKey: " + privateKey
                        walletDetailEditText?.setText(text)
                    } else {
                        walletDetailEditText?.setText(error)
                    }
                }
            }
            walletDetailEditText?.setText("Import Accounting.......")
            bitcoin?.importAccountFromMnemonic(mnemonic,onCompleted = onCompleted)
        }
    }
}