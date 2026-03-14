package com.mnikita.knowyourrunway.network

import com.squareup.moshi.Json

data class RegisterReq(val name: String, val email: String, val password: String)
data class VerifyCodeReq(val email: String, val code: String)

data class LoginReq(val email: String, val password: String)

data class LoginRes(
    val token: String? = null,
    val user: UserRes? = null,
    val error: String? = null
)

data class UserRes(val id: Long, val name: String, val email: String)

data class FeedRes(val items: List<ProductRes> = emptyList())

data class ProductRes(
    val id: Long,
    val title: String,
    val brand: String?,
    @Json(name = "price_inr") val priceInr: Int?,
    val gender: String?,
    @Json(name = "cover_url") val coverUrl: String?,

    @Json(name = "asin") val asin: String?,
    @Json(name = "mrp_inr") val mrpInr: Int?,
    val rating: Double?,
    @Json(name = "rating_total") val ratingTotal: Int?,
    @Json(name = "discount_pct") val discountPct: Int?,
    @Json(name = "seller_name") val sellerName: String?,
    @Json(name = "product_url") val productUrl: String?,

    val score: Int? = null
)

data class SwipeReq(@Json(name = "product_id") val productId: Long, val direction: String)

data class ForgotReq(val email: String)
data class ResetPasswordReq(
    val email: String,
    val code: String,
    @Json(name = "new_password") val newPassword: String
)

data class SimpleRes(val ok: Boolean? = null, val error: String? = null)

// PROFILE
data class ProfileSaveReq(
    val name: String,
    val username: String,
    val country: String,
    @Json(name = "birth_date") val birthDate: String? = null, // YYYY-MM-DD
    val gender: String = "Prefer not to say",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val tags: List<String> = emptyList()
)

data class ProfileRes(
    val name: String? = null,
    val username: String? = null,
    val country: String? = null,
    @Json(name = "birth_date") val birthDate: String? = null,
    val gender: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val tags: List<String> = emptyList()
)

data class ProfileGetRes(
    val ok: Boolean? = null,
    val profile: ProfileRes? = null,
    val error: String? = null
)

// ✅ Avatar upload response (NEW)
data class AvatarUploadRes(
    val ok: Boolean? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val error: String? = null
)