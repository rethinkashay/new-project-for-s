package de.ashaysurya.myapplication.network

import com.google.gson.annotations.SerializedName

data class OcrResponse(
    @SerializedName("text")
    val detectedText: String
)