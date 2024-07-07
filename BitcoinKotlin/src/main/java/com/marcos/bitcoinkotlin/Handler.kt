package com.marcos.bitcoinkotlin

interface Handler {
    fun handler(map: HashMap<String, Any>?, callback: Callback)
}