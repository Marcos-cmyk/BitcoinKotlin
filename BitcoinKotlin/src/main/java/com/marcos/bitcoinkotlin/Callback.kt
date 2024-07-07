package com.marcos.bitcoinkotlin

interface Callback {
    fun call(map: HashMap<String, Any>?)
}