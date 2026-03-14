package com.mnikita.knowyourrunway.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("auth_register.php")
    suspend fun register(@Body body: RegisterReq): SimpleRes

    @POST("auth_verify_signup.php")
    suspend fun verifySignup(@Body body: VerifyCodeReq): LoginRes

    @POST("auth_login.php")
    suspend fun login(@Body body: LoginReq): LoginRes

    @GET("feed.php")
    suspend fun feed(
        @Header("Authorization") bearer: String,
        @Query("q") q: String? = null,
        @Query("limit") limit: Int = 20
    ): FeedRes

    @POST("swipe.php")
    suspend fun swipe(
        @Header("Authorization") bearer: String,
        @Body body: SwipeReq
    ): SimpleRes

    @POST("auth_forgot.php")
    suspend fun forgot(@Body body: ForgotReq): SimpleRes

    @POST("auth_reset_password.php")
    suspend fun resetPassword(@Body body: ResetPasswordReq): SimpleRes

    // Profile
    @GET("profile_get.php")
    suspend fun profileGet(@Header("Authorization") bearer: String): ProfileGetRes

    @POST("profile_save.php")
    suspend fun profileSave(
        @Header("Authorization") bearer: String,
        @Body body: ProfileSaveReq
    ): SimpleRes

    // ✅ Avatar upload (NEW)
    @Multipart
    @POST("profile_upload_avatar.php")
    suspend fun profileUploadAvatar(
        @Header("Authorization") bearer: String,
        @Part avatar: MultipartBody.Part
    ): AvatarUploadRes
}