package de.ashaysurya.myapplication.network

import de.ashaysurya.myapplication.MenuItem // Add this import
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("ocr/upload-image/")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<List<MenuItem>> // This line is changed

}