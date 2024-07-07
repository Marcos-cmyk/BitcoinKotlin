package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GenerateAccount: AppCompatActivity() {

    private var title: TextView? = null
    private var walletDetail: EditText? = null
    private var generateAccountBtn: Button? = null
    private var mWebView: WebView? = null
    private var bitcoin: Bitcoin? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_account)
        setupContent()
    }

    private fun setupContent() {
        title = findViewById(R.id.title)
        walletDetail = findViewById(R.id.wallet_detail)
        generateAccountBtn = findViewById(R.id.btn_generate_account)
        mWebView =  findViewById(R.id.webView)
        bitcoin = Bitcoin(this, _webView = mWebView!!)
        generateAccountBtn?.setOnClickListener{
            generateAccount()
        }
    }
    private fun generateAccount() {
        val onCompleted = {result : Boolean ->
            println("bitcoin setup Completed------->>>>>")
            println(result)
            generateAccountAction()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted = onCompleted)
        }  else  {
            generateAccountAction()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun generateAccountAction() {
        val onCompleted = {state : Boolean,address:String,privateKey:String,mnemonic:String,error: String ->
            this.runOnUiThread {
                if (state) {
                    val text =
                    "address: " + address + "\n\n" +
                    "mnemonic: " + mnemonic + "\n\n" +
                    "privateKey: " + privateKey
                    walletDetail?.setText(text)
                } else {
                    walletDetail?.setText(error)
                }
            }
        }
        walletDetail?.setText("generate Accounting.......")
        bitcoin?.generateAccount(onCompleted = onCompleted)
    }
}