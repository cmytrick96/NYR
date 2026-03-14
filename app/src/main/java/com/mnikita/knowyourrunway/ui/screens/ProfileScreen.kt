package com.mnikita.knowyourrunway.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.AccentPreset
import com.mnikita.knowyourrunway.data.CartStore
import com.mnikita.knowyourrunway.data.FeedCache
import com.mnikita.knowyourrunway.data.ShopSession
import com.mnikita.knowyourrunway.data.ThemeMode
import com.mnikita.knowyourrunway.data.ThemeStore
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.data.UserProfile
import com.mnikita.knowyourrunway.data.WishlistStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.ProfileSaveReq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Calendar
import java.util.Locale

private enum class ProfileInfoSheet {
    HELP, PRIVACY, TERMS, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChooseCategory: () -> Unit,
    onOpenWishlist: () -> Unit,
    onLogoutDone: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    val profile by tokenStore.profileFlow.collectAsState(initial = UserProfile())

    // ✅ Theme preferences
    val themeMode by ThemeStore.themeModeFlow(ctx).collectAsState(initial = ThemeMode.SYSTEM)
    val accentPreset by ThemeStore.accentFlow(ctx).collectAsState(initial = AccentPreset.COFFEE)
    var showThemeSheet by remember { mutableStateOf(false) }

    fun themeLabel(m: ThemeMode) = when (m) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    fun accentLabel(a: AccentPreset) = when (a) {
        AccentPreset.COFFEE -> "Coffee"
        AccentPreset.RED -> "Red"
        AccentPreset.BLUE -> "Blue"
        AccentPreset.PURPLE -> "Purple"
        AccentPreset.GREEN -> "Green"
        AccentPreset.PINK -> "Pink"
    }

    // ✅ Info sheets (Help / Privacy / Terms / About)
    var activeInfoSheet by remember { mutableStateOf<ProfileInfoSheet?>(null) }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // ✅ Location flow UI state
    var showLocationDialog by remember { mutableStateOf(false) }
    var locLoading by remember { mutableStateOf(false) }

    val wishCount = WishlistStore.items.size
    val badgeText = when {
        wishCount <= 0 -> ""
        wishCount >= 100 -> "99+"
        else -> wishCount.toString()
    }

    fun isoToDisplay(iso: String): String {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").find(iso.trim()) ?: return iso
        val y = m.groupValues[1]
        val mm = m.groupValues[2]
        val dd = m.groupValues[3]
        return "$dd-$mm-$y"
    }

    fun displayToIso(display: String): String? {
        val m = Regex("""^(\d{2})-(\d{2})-(\d{4})$""").find(display.trim()) ?: return null
        return "${m.groupValues[3]}-${m.groupValues[2]}-${m.groupValues[1]}"
    }

    fun computeAgeFromDisplayDate(display: String): Int? {
        val m = Regex("""^(\d{2})-(\d{2})-(\d{4})$""").find(display.trim()) ?: return null
        val dd = m.groupValues[1].toIntOrNull() ?: return null
        val mm = m.groupValues[2].toIntOrNull() ?: return null
        val yy = m.groupValues[3].toIntOrNull() ?: return null

        val today = Calendar.getInstance()
        val hadBirthdayThisYear = when {
            today.get(Calendar.MONTH) + 1 > mm -> true
            today.get(Calendar.MONTH) + 1 < mm -> false
            else -> today.get(Calendar.DAY_OF_MONTH) >= dd
        }
        var age = today.get(Calendar.YEAR) - yy
        if (!hadBirthdayThisYear) age -= 1

        if (age < 0 || age > 130) return null
        return age
    }

    val derivedAge = remember(profile.birthDate) {
        profile.birthDate.trim().takeIf { it.isNotBlank() }?.let { computeAgeFromDisplayDate(it) }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun reverseGeocodeCityCountry(lat: Double, lon: Double): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(ctx, Locale.getDefault())
                @Suppress("DEPRECATION")
                val res = geocoder.getFromLocation(lat, lon, 1)
                val addr = res?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                val country = addr?.countryName
                Pair(city, country)
            }.getOrElse { Pair(null, null) }
        }
    }

    fun bestLastKnownLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val candidates = providers.mapNotNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        }
        return candidates.maxByOrNull { it.time }
    }

    fun parseServerError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val m = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return m?.groupValues?.getOrNull(1)
    }

    suspend fun saveLocationToServerAndLocal(label: String) {
        tokenStore.saveProfile(
            UserProfile(
                imageUri = profile.imageUri,
                avatarUrl = profile.avatarUrl,
                name = profile.name,
                username = profile.username,
                country = label,
                birthDate = profile.birthDate,
                gender = profile.gender,
                tags = profile.tags
            )
        )

        val token = tokenStore.tokenFlow.first().orEmpty()
        if (token.isBlank()) return

        val n = profile.name.trim()
        val u = profile.username.trim()
        if (n.isBlank() || u.isBlank()) return

        val birthIso = profile.birthDate.trim().takeIf { it.isNotBlank() }?.let { displayToIso(it) }

        try {
            val bearer = "Bearer $token"
            val res = api.profileSave(
                bearer = bearer,
                body = ProfileSaveReq(
                    name = n,
                    username = u,
                    country = label,
                    birthDate = birthIso,
                    gender = profile.gender.ifBlank { "Prefer not to say" },
                    avatarUrl = profile.avatarUrl,
                    tags = profile.tags
                )
            )
            if (res.ok != true) {
                Toast.makeText(ctx, res.error ?: "Failed to save location", Toast.LENGTH_SHORT).show()
            }
        } catch (he: HttpException) {
            val body = runCatching { he.response()?.errorBody()?.string() }.getOrNull()
            val parsed = parseServerError(body)
            Toast.makeText(ctx, parsed ?: "Server error (${he.code()})", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun fetchAndSaveLocation() {
        locLoading = true
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val enabled = runCatching {
                lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            }.getOrDefault(true)

            if (!enabled) {
                Toast.makeText(ctx, "Turn on Location (GPS) and try again.", Toast.LENGTH_SHORT).show()
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                return
            }

            val loc = bestLastKnownLocation(ctx)
            if (loc == null) {
                Toast.makeText(ctx, "Couldn’t get location. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                return
            }

            val (city, country) = reverseGeocodeCityCountry(loc.latitude, loc.longitude)
            val label = when {
                !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                !country.isNullOrBlank() -> country
                else -> null
            }

            if (label.isNullOrBlank()) {
                Toast.makeText(ctx, "Couldn’t detect city/country. Try again.", Toast.LENGTH_SHORT).show()
                return
            }

            saveLocationToServerAndLocal(label)
            Toast.makeText(ctx, "Location saved: $label", Toast.LENGTH_SHORT).show()
        } finally {
            locLoading = false
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val granted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (granted) {
            scope.launch { fetchAndSaveLocation() }
        } else {
            Toast.makeText(ctx, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocationPermission() {
        locationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        runCatching {
            val token = tokenStore.tokenFlow.first().orEmpty()
            if (token.isNotBlank()) {
                val res = api.profileGet("Bearer $token")
                val p = res.profile
                if (res.ok == true && p != null) {
                    tokenStore.saveProfile(
                        UserProfile(
                            imageUri = profile.imageUri,
                            avatarUrl = p.avatarUrl,
                            name = p.name.orEmpty(),
                            username = p.username.orEmpty(),
                            country = p.country.orEmpty(),
                            birthDate = isoToDisplay(p.birthDate.orEmpty()),
                            gender = p.gender ?: "Prefer not to say",
                            tags = p.tags
                        )
                    )
                }
            }
        }
    }

    // ✅ Theme bottom sheet
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            containerColor = cs.surface
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Theme & colors",
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface
                )

                Text("Theme", color = cs.onSurfaceVariant)
                Column {
                    ThemeOptionRow(
                        label = "System",
                        selected = themeMode == ThemeMode.SYSTEM
                    ) { scope.launch { ThemeStore.setThemeMode(ctx, ThemeMode.SYSTEM) } }

                    ThemeOptionRow(
                        label = "Light",
                        selected = themeMode == ThemeMode.LIGHT
                    ) { scope.launch { ThemeStore.setThemeMode(ctx, ThemeMode.LIGHT) } }

                    ThemeOptionRow(
                        label = "Dark",
                        selected = themeMode == ThemeMode.DARK
                    ) { scope.launch { ThemeStore.setThemeMode(ctx, ThemeMode.DARK) } }
                }

                Text("Accent color", color = cs.onSurfaceVariant)
                val scroll = rememberScrollState()
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val presets = listOf(
                        AccentPreset.COFFEE,
                        AccentPreset.RED,
                        AccentPreset.BLUE,
                        AccentPreset.PURPLE,
                        AccentPreset.GREEN,
                        AccentPreset.PINK
                    )

                    presets.forEach { preset ->
                        FilterChip(
                            selected = accentPreset == preset,
                            onClick = { scope.launch { ThemeStore.setAccent(ctx, preset) } },
                            label = { Text(accentLabel(preset)) }
                        )
                    }
                }

                Button(
                    onClick = { showThemeSheet = false },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Text("Done", color = cs.onPrimary)
                }

                Spacer(androidx.compose.ui.Modifier.height(8.dp))
            }
        }
    }

    // ✅ Info bottom sheet (Help / Privacy / Terms / About)
    if (activeInfoSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeInfoSheet = null },
            containerColor = cs.surface
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SheetHeader(
                    title = when (activeInfoSheet!!) {
                        ProfileInfoSheet.HELP -> "Help & Support"
                        ProfileInfoSheet.PRIVACY -> "Privacy Policy"
                        ProfileInfoSheet.TERMS -> "Terms & Conditions"
                        ProfileInfoSheet.ABOUT -> "About"
                    },
                    onClose = { activeInfoSheet = null }
                )

                Divider(color = cs.outline.copy(alpha = 0.18f))
                Spacer(androidx.compose.ui.Modifier.height(10.dp))

                val contentScroll = rememberScrollState()
                Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .verticalScroll(contentScroll)
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeInfoSheet!!) {
                        ProfileInfoSheet.HELP -> HelpSupportContent()
                        ProfileInfoSheet.PRIVACY -> PrivacyPolicyContent()
                        ProfileInfoSheet.TERMS -> TermsContent()
                        ProfileInfoSheet.ABOUT -> AboutContent()
                    }

                    Spacer(androidx.compose.ui.Modifier.height(6.dp))
                }
            }
        }
    }

    // ✅ Logout confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        scope.launch {
                            runCatching { tokenStore.clearToken() }
                            runCatching { tokenStore.clearProfile() }
                            runCatching { FeedCache.clear() }
                            runCatching { WishlistStore.clear() }
                            runCatching { CartStore.clear() }
                            onLogoutDone()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) { Text("Logout", color = cs.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Logout?") },
            text = { Text("You will be signed out from this device.") }
        )
    }

    // ✅ Location permission dialog
    if (showLocationDialog) {
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } == true

        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Enable location?") },
            text = {
                Text(
                    if (shouldShowRationale) {
                        "We use your location to save your city & country in your profile. Please tap Allow."
                    } else {
                        "Allow location while using the app to save your city & country."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationDialog = false
                        requestLocationPermission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) { Text("Allow", color = cs.onPrimary) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showLocationDialog = false }) { Text("Not now") }
                    Spacer(androidx.compose.ui.Modifier.width(6.dp))
                    TextButton(onClick = {
                        showLocationDialog = false
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${ctx.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(intent)
                        }
                    }) { Text("Settings") }
                }
            }
        )
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Your Profile", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenWishlist) {
                        BadgedBox(badge = { if (wishCount > 0) Badge { Text(badgeText) } }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Wishlist", tint = cs.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(cs.surfaceVariant)
                            .border(1.dp, cs.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = profile.imageUri,
                                contentDescription = "Profile image",
                                modifier = androidx.compose.ui.Modifier.fillMaxSize()
                            )
                        } else if (!profile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "Profile image",
                                modifier = androidx.compose.ui.Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = cs.onSurfaceVariant)
                        }
                    }

                    Spacer(androidx.compose.ui.Modifier.width(14.dp))

                    Column(androidx.compose.ui.Modifier.weight(1f)) {
                        Text(
                            profile.name.ifBlank { "—" },
                            style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface
                        )
                        Text("@${profile.username.ifBlank { "—" }}", color = cs.onSurfaceVariant)
                        Spacer(androidx.compose.ui.Modifier.height(6.dp))
                        Text(profile.country.ifBlank { "—" }, color = cs.onSurfaceVariant)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Details",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text("Birth date: ${profile.birthDate.ifBlank { "—" }}", color = cs.onSurfaceVariant)
                    if (derivedAge != null) Text("Age: $derivedAge", color = cs.onSurfaceVariant)
                    Text("Gender: ${profile.gender.ifBlank { "—" }}", color = cs.onSurfaceVariant)
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Your tags",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    if (profile.tags.isEmpty()) {
                        Text("—", color = cs.onSurfaceVariant)
                    } else {
                        profile.tags.take(5).forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = cs.primary.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    t,
                                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = cs.primary
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onEdit,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                Text("Edit profile", color = cs.onPrimary)
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(androidx.compose.ui.Modifier.padding(vertical = 6.dp)) {

                    SettingsRow(
                        icon = Icons.Default.LocationOn,
                        title = "Location",
                        subtitle = if (locLoading) "Saving location..." else "Allow while using app • Save city & country",
                        onClick = {
                            if (locLoading) return@SettingsRow
                            if (hasLocationPermission(ctx)) {
                                scope.launch { fetchAndSaveLocation() }
                            } else {
                                showLocationDialog = true
                            }
                        }
                    )

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Theme & colors",
                        subtitle = "${themeLabel(themeMode)} • ${accentLabel(accentPreset)}",
                        onClick = { showThemeSheet = true }
                    )

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.ShoppingBag,
                        title = "Change shopping category",
                        subtitle = "Men • Women • Kids • Jewelry • Accessories",
                        onClick = onChooseCategory
                    )

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.HelpOutline,
                        title = "Help & Support",
                        subtitle = "FAQs and contact"
                    ) { activeInfoSheet = ProfileInfoSheet.HELP }

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "How we use your data"
                    ) { activeInfoSheet = ProfileInfoSheet.PRIVACY }

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.Description,
                        title = "Terms & Conditions",
                        subtitle = "Rules and usage terms"
                    ) { activeInfoSheet = ProfileInfoSheet.TERMS }

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "App information"
                    ) { activeInfoSheet = ProfileInfoSheet.ABOUT }

                    Divider(color = cs.outline.copy(alpha = 0.18f))

                    SettingsRow(
                        icon = Icons.Default.Logout,
                        title = "Logout",
                        subtitle = "Sign out of this device",
                        danger = true
                    ) {
                        showLogoutConfirm = true
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(androidx.compose.ui.Modifier.width(10.dp))
        Text(label, color = cs.onSurface)
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (danger) cs.error.copy(alpha = 0.10f) else cs.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (danger) cs.error else cs.primary)
        }

        Spacer(androidx.compose.ui.Modifier.width(12.dp))

        Column(androidx.compose.ui.Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (danger) cs.error else cs.onSurface
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = cs.onSurface,
            modifier = androidx.compose.ui.Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = cs.onSurface
    )
}

@Composable
private fun Body(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = cs.onSurfaceVariant
    )
}

@Composable
private fun Bullet(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Text("•  ", color = cs.onSurfaceVariant)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = androidx.compose.ui.Modifier.weight(1f)
        )
    }
}

@Composable
private fun HelpSupportContent() {
    SectionTitle("Quick help")
    Body("Here are a few common issues and fixes. (You’ll share your final Help & Support text and I’ll replace this section.)")

    SectionTitle("Account & OTP")
    Bullet("If you don’t receive an OTP, check network signal and try again after 30–60 seconds.")
    Bullet("Make sure you entered the correct phone/email used during sign up.")
    Bullet("If OTP still doesn’t arrive, wait a bit and request a new OTP.")

    SectionTitle("Profile & category")
    Bullet("Category selection is required on every app open (session-only). You can change it from Profile.")
    Bullet("If profile details look outdated, reopen the app or update your profile and save again.")

    SectionTitle("Wishlist & cart")
    Bullet("Wishlist and Cart are currently stored locally on your device (not synced).")
    Bullet("Logging out clears local data on this device.")

    SectionTitle("Contact support")
    Body("Email us at: nikitamotwani42@gmail.com")
}

@Composable
private fun PrivacyPolicyContent() {
    SectionTitle("Privacy Policy")
    Body("This Privacy Policy explains how KnowYourRunway (NYR) collects, uses, and protects your information when you use the app.")

    SectionTitle("Information we collect")
    Bullet("Account details: information you provide during sign up / login (and OTP verification).")
    Bullet("Profile details: name, username, gender, birth date, tags, and avatar image URL (if you add one).")
    Bullet("Location (optional): if you allow location access, we can save your city/country in your profile.")
    Bullet("Usage data: basic app interactions needed to operate features (for example, searching, swiping/liking).")
    Bullet("Device/diagnostic data: crash logs or network error logs (to help improve stability).")

    SectionTitle("How we use your information")
    Bullet("To create and manage your account and authenticate you.")
    Bullet("To save and show your profile details across sessions.")
    Bullet("To deliver core app features such as product feeds, search, and swipe experiences.")
    Bullet("To provide support and fix issues, if you contact us.")
    Bullet("To improve performance, reliability, and user experience.")

    SectionTitle("How information is shared")
    Body("We do not sell your personal information. We may share information only in these cases:")
    Bullet("With service providers that help run the app (for example, hosting/back-end infrastructure).")
    Bullet("When required by law or to respond to valid legal requests.")
    Bullet("To protect the safety, rights, or security of users and the platform.")

    SectionTitle("Storage and security")
    Bullet("Your sign-in token and some settings (like theme/accent) are stored locally on your device using secure app storage.")
    Bullet("Profile information is stored on our servers to allow the app to function.")
    Bullet("We use reasonable safeguards, but no system is 100% secure.")

    SectionTitle("Your choices")
    Bullet("You can edit your profile from the Profile screen.")
    Bullet("You can enable/disable location permission anytime in device Settings.")
    Bullet("You can log out to remove the token from this device.")
    Bullet("If you want your account data removed, contact support (add your contact details).")

    SectionTitle("Children’s privacy")
    Body("NYR is not intended for children under 13. If you believe a child has provided personal information, contact support.")

    SectionTitle("Changes to this policy")
    Body("We may update this policy from time to time. If changes are significant, we may notify you in-app.")

    SectionTitle("Contact")
    Body("Support contact: nikitamotwani42@gmail.com")
    Body("Last updated: March 2026")
}

@Composable
private fun TermsContent() {
    SectionTitle("Terms & Conditions")
    Body("These Terms govern your use of KnowYourRunway (NYR). By using the app, you agree to these Terms.")

    SectionTitle("Using the app")
    Bullet("You must provide accurate information when creating an account.")
    Bullet("You are responsible for maintaining the confidentiality of your account/token on your device.")
    Bullet("Do not misuse the app (no scraping, reverse engineering, abusing APIs, or attempting unauthorized access).")

    SectionTitle("Product information")
    Bullet("NYR displays product listings fetched from our servers and/or partners.")
    Bullet("Product details (price, availability, images, descriptions) may change and may not always be accurate.")
    Bullet("We do not guarantee that any product will be available or that information is error-free.")

    SectionTitle("Wishlist, cart, and checkout")
    Bullet("Wishlist and Cart features may be stored locally on your device and may not be synced across devices.")
    Bullet("Checkout may be a placeholder until full ordering/payment is launched.")
    Bullet("When the app links you to external product pages, those third-party sites may have their own terms and policies.")

    SectionTitle("Intellectual property")
    Bullet("NYR, its UI, branding, and app content are protected by applicable intellectual property laws.")
    Bullet("You may not copy, modify, distribute, sell, or lease any part of the app without permission.")

    SectionTitle("Disclaimers")
    Body("The app is provided “as is” and “as available.” We do not make warranties of uninterrupted service or error-free operation.")

    SectionTitle("Limitation of liability")
    Body("To the maximum extent permitted by law, NYR will not be liable for indirect or consequential damages arising from your use of the app.")

    SectionTitle("Termination")
    Body("We may suspend or terminate access if we believe there is misuse or violation of these Terms.")

    SectionTitle("Contact")
    Body("Questions about these Terms: Email us at: nikitamotwani42@gmail.com")
    Body("Last updated: March 2026")
}

@Composable
private fun AboutContent() {
    SectionTitle("KnowYourRunway (NYR)")
    Body("Premium fashion discovery and shopping experience — swipe, search, save, and explore trends. (You’ll share your final About text and I’ll replace this section.)")

    SectionTitle("What you can do")
    Bullet("Browse a premium feed of fashion products")
    Bullet("Swipe to like and save items")
    Bullet("Search products by name/brand")
    Bullet("Manage your profile and theme preferences")

    SectionTitle("Created Solely and Proudly by")
    Body("Nikita Motwani")
}