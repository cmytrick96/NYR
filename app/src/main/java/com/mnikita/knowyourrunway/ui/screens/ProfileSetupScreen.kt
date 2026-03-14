package com.mnikita.knowyourrunway.ui.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.data.UserProfile
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.ProfileSaveReq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onDone: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val scope = rememberCoroutineScope()

    // dd-MM-yyyy <-> yyyy-MM-dd
    fun isoToDisplay(iso: String): String {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").find(iso.trim()) ?: return iso
        return "${m.groupValues[3]}-${m.groupValues[2]}-${m.groupValues[1]}"
    }
    fun displayToIso(display: String): String? {
        val m = Regex("""^(\d{2})-(\d{2})-(\d{4})$""").find(display.trim()) ?: return null
        return "${m.groupValues[3]}-${m.groupValues[2]}-${m.groupValues[1]}"
    }

    fun computeAgeFromDisplayDate(display: String): Int? {
        val iso = displayToIso(display) ?: return null
        val parts = iso.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null

        val today = Calendar.getInstance()
        val hadBirthdayThisYear = when {
            today.get(Calendar.MONTH) + 1 > m -> true
            today.get(Calendar.MONTH) + 1 < m -> false
            else -> today.get(Calendar.DAY_OF_MONTH) >= d
        }
        var age = today.get(Calendar.YEAR) - y
        if (!hadBirthdayThisYear) age -= 1
        if (age < 0 || age > 130) return null
        return age
    }

    // Emoji flag from ISO2 code
    fun flagEmoji(code: String): String {
        if (code.length != 2) return ""
        val first = Character.codePointAt(code.uppercase(), 0) - 65 + 0x1F1E6
        val second = Character.codePointAt(code.uppercase(), 1) - 65 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    val allCountries = remember {
        Locale.getISOCountries()
            .map { c ->
                val name = Locale("", c).displayCountry
                Triple(name, c, flagEmoji(c))
            }
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    // ✅ Preset avatar URLs (stable seeds)
    val menAvatars = remember {
        listOf(
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m1",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m2",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m3",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m4",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m5",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m6",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m7",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m8",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m9",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_m10"
        )
    }
    val womenAvatars = remember {
        listOf(
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w1",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w2",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w3",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w4",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w5",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w6",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w7",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w8",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w9",
            "https://api.dicebear.com/7.x/avataaars/png?seed=nyr_w10"
        )
    }
    val neutralAvatars = remember {
        listOf(
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n1",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n2",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n3",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n4",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n5",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n6",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n7",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n8",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n9",
            "https://api.dicebear.com/7.x/adventurer/png?seed=nyr_n10"
        )
    }

    var imageUri by remember { mutableStateOf<String?>(null) }     // persisted local URI
    var avatarUrl by remember { mutableStateOf<String?>(null) }    // server URL or preset URL

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") } // dd-MM-yyyy
    var gender by remember { mutableStateOf("Prefer not to say") }

    var tag1 by remember { mutableStateOf("") }
    var tag2 by remember { mutableStateOf("") }
    var tag3 by remember { mutableStateOf("") }
    var tag4 by remember { mutableStateOf("") }
    var tag5 by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showGenderDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var countrySearch by remember { mutableStateOf("") }

    // ✅ Avatar preset dialog
    var showAvatarDialog by remember { mutableStateOf(false) }
    var avatarTab by remember { mutableStateOf(0) } // 0 men, 1 women, 2 neutral

    // ✅ Location auto-fill
    var locLoading by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // ✅ Quick tag picks lists
    val quickBrands = remember {
        listOf("Nike", "Adidas", "Puma", "Levi's", "Zara", "H&M", "Roadster", "HRX", "Allen Solly", "U.S. Polo", "Only", "Biba")
    }
    val quickStyles = remember {
        listOf("Streetwear", "Casual", "Formal", "Party", "Ethnic", "Sporty", "Minimal", "Vintage", "Oversized", "Denim")
    }
    val quickItems = remember {
        listOf("Sneakers", "Jeans", "Shirts", "T-Shirts", "Dresses", "Kurta", "Hoodies", "Watches", "Bags", "Sunglasses")
    }

    fun currentTags(): List<String> {
        return listOf(tag1, tag2, tag3, tag4, tag5)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(5)
    }

    fun setTags(tags: List<String>) {
        val t = tags.take(5)
        tag1 = t.getOrNull(0).orEmpty()
        tag2 = t.getOrNull(1).orEmpty()
        tag3 = t.getOrNull(2).orEmpty()
        tag4 = t.getOrNull(3).orEmpty()
        tag5 = t.getOrNull(4).orEmpty()
    }

    fun toggleQuickTag(tag: String) {
        val now = currentTags().toMutableList()
        val existingIndex = now.indexOfFirst { it.equals(tag, ignoreCase = true) }
        if (existingIndex >= 0) {
            now.removeAt(existingIndex)
            setTags(now)
            return
        }
        if (now.size >= 5) {
            Toast.makeText(ctx, "You can add max 5 tags", Toast.LENGTH_SHORT).show()
            return
        }
        now.add(tag)
        setTags(now)
    }

    // ✅ Avatar picker with persisted permission
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = uri.toString()
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        displayToIso(birthDate)?.split("-")?.let { p ->
            if (p.size == 3) {
                runCatching {
                    cal.set(Calendar.YEAR, p[0].toInt())
                    cal.set(Calendar.MONTH, p[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, p[2].toInt())
                }
            }
        }

        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val mm = (m + 1).toString().padStart(2, '0')
                val dd = d.toString().padStart(2, '0')
                birthDate = "$dd-$mm-$y"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun parseServerError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val m = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return m?.groupValues?.getOrNull(1)
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
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

    suspend fun reverseGeocodeCityCountry(lat: Double, lon: Double): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(ctx, Locale.getDefault())
                @Suppress("DEPRECATION")
                val res = geocoder.getFromLocation(lat, lon, 1)
                val addr = res?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                val ctry = addr?.countryName
                Pair(city, ctry)
            }.getOrElse { Pair(null, null) }
        }
    }

    suspend fun fillCountryFromLocation() {
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
                    ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return
            }

            val loc = bestLastKnownLocation(ctx)
            if (loc == null) {
                Toast.makeText(ctx, "Couldn’t get location. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }

            val (city, ctry) = reverseGeocodeCityCountry(loc.latitude, loc.longitude)
            val label = when {
                !city.isNullOrBlank() && !ctry.isNullOrBlank() -> "$city, $ctry"
                !ctry.isNullOrBlank() -> ctry
                else -> null
            }

            if (label.isNullOrBlank()) {
                Toast.makeText(ctx, "Couldn’t detect city/country. Try again.", Toast.LENGTH_SHORT).show()
                return
            }

            country = label
            Toast.makeText(ctx, "Auto-filled: $label", Toast.LENGTH_SHORT).show()
        } finally {
            locLoading = false
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val granted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (granted) scope.launch { fillCountryFromLocation() }
        else Toast.makeText(ctx, "Location permission denied.", Toast.LENGTH_SHORT).show()
    }

    fun requestLocationPermission() {
        locationPermLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Prefill
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (prefilled) return@LaunchedEffect
        prefilled = true

        runCatching {
            val local = tokenStore.profileFlow.first()
            imageUri = local.imageUri
            avatarUrl = local.avatarUrl
            if (local.name.isNotBlank()) {
                name = local.name
                username = local.username
                country = local.country
                birthDate = local.birthDate
                gender = local.gender
                val tags = local.tags
                tag1 = tags.getOrNull(0).orEmpty()
                tag2 = tags.getOrNull(1).orEmpty()
                tag3 = tags.getOrNull(2).orEmpty()
                tag4 = tags.getOrNull(3).orEmpty()
                tag5 = tags.getOrNull(4).orEmpty()
            }
        }

        runCatching {
            val token = tokenStore.tokenFlow.first().orEmpty()
            if (token.isNotBlank() && name.isBlank() && username.isBlank()) {
                val res = api.profileGet("Bearer $token")
                val p = res.profile
                if (res.ok == true && p != null) {
                    name = p.name.orEmpty()
                    username = p.username.orEmpty()
                    country = p.country.orEmpty()
                    birthDate = isoToDisplay(p.birthDate.orEmpty())
                    gender = p.gender ?: "Prefer not to say"
                    avatarUrl = p.avatarUrl
                    val tags = p.tags
                    tag1 = tags.getOrNull(0).orEmpty()
                    tag2 = tags.getOrNull(1).orEmpty()
                    tag3 = tags.getOrNull(2).orEmpty()
                    tag4 = tags.getOrNull(3).orEmpty()
                    tag5 = tags.getOrNull(4).orEmpty()
                }
            }
        }
    }

    val filteredCountries = remember(countrySearch, allCountries) {
        val q = countrySearch.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) allCountries
        else allCountries.filter { it.first.lowercase(Locale.getDefault()).contains(q) }
    }

    // ✅ Country dialog
    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text("Select country") },
            text = {
                Column {
                    OutlinedTextField(
                        value = countrySearch,
                        onValueChange = { countrySearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.heightIn(max = 360.dp)) {
                        LazyColumn {
                            items(filteredCountries.take(250)) { item ->
                                val cname = item.first
                                val flag = item.third
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            country = cname
                                            countrySearch = ""
                                            showCountryDialog = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(flag, modifier = Modifier.width(32.dp))
                                    Text(cname)
                                }
                                Divider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountryDialog = false }) { Text("Close") } }
        )
    }

    // ✅ Gender dialog
    val genders = listOf("Male", "Female", "Non-binary", "Prefer not to say")
    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text("Select gender") },
            text = {
                Column {
                    genders.forEach { g ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    gender = g
                                    showGenderDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) { Text(g) }
                        Divider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGenderDialog = false }) { Text("Close") } }
        )
    }

    // ✅ Avatar preset dialog
    if (showAvatarDialog) {
        val tabs = listOf("Men", "Women", "Neutral")
        val currentList = when (avatarTab) {
            0 -> menAvatars
            1 -> womenAvatars
            else -> neutralAvatars
        }

        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose an avatar") },
            text = {
                Column {
                    TabRow(selectedTabIndex = avatarTab) {
                        tabs.forEachIndexed { idx, t ->
                            Tab(
                                selected = avatarTab == idx,
                                onClick = { avatarTab = idx },
                                text = { Text(t) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Box(modifier = Modifier.heightIn(max = 360.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            gridItems(currentList) { url ->
                                val selected = (avatarUrl == url) && imageUri.isNullOrBlank()
                                Surface(
                                    shape = CircleShape,
                                    color = cs.surface,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .size(66.dp)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) cs.primary else cs.outline.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable {
                                            imageUri = null
                                            avatarUrl = url
                                            showAvatarDialog = false
                                        }
                                ) {
                                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Tip: You can still upload a real photo anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAvatarDialog = false }) { Text("Close") } }
        )
    }

    // ✅ Location confirm dialog (ProfileSetup)
    if (showLocationDialog) {
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } == true

        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Use your location?") },
            text = {
                Text(
                    if (shouldShowRationale) {
                        "We’ll auto-fill your city & country. Tap Allow."
                    } else {
                        "Allow location while using the app to auto-fill your city & country."
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
            dismissButton = { TextButton(onClick = { showLocationDialog = false }) { Text("Not now") } }
        )
    }

    @Composable
    fun PickerField(label: String, value: String, placeholder: String, onClick: () -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, color = cs.onBackground.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onClick() },
                color = cs.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline.copy(alpha = 0.55f))
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = if (value.isBlank()) placeholder else value,
                        color = if (value.isBlank()) cs.onSurfaceVariant else cs.onSurface
                    )
                }
            }
        }
    }

    @Composable
    fun QuickChipRow(title: String, chips: List<String>) {
        val selectedNow = remember(tag1, tag2, tag3, tag4, tag5) { currentTags() }
        val scroll = rememberScrollState()

        Column(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chips.forEach { chip ->
                    val selected = selectedNow.any { it.equals(chip, ignoreCase = true) }
                    FilterChip(
                        selected = selected,
                        onClick = { toggleQuickTag(chip) },
                        label = { Text(chip) }
                    )
                }
            }
        }
    }

    val scroll = rememberScrollState()
    val derivedAge = remember(birthDate) { birthDate.trim().takeIf { it.isNotBlank() }?.let { computeAgeFromDisplayDate(it) } }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create your profile", fontWeight = FontWeight.SemiBold, color = cs.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(cs.surface)
                            .border(1.dp, cs.outline.copy(alpha = 0.4f), CircleShape)
                            .clickable { pickImage.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !imageUri.isNullOrBlank() -> AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                            !avatarUrl.isNullOrBlank() -> AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                            else -> Text("Add\nPhoto", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Profile photo", fontWeight = FontWeight.Medium, color = cs.onBackground)
                        Text("Upload a real photo or choose a preset avatar.", color = cs.onBackground.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { pickImage.launch(arrayOf("image/*")) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Upload") }

                            OutlinedButton(
                                onClick = { showAvatarDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Choose avatar") }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        username = input.lowercase(Locale.getDefault())
                            .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                            .take(20)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true
                )

                // Country picker + auto-fill button
                PickerField("Country", country, "Select country") { showCountryDialog = true }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (locLoading) return@OutlinedButton
                            if (hasLocationPermission(ctx)) scope.launch { fillCountryFromLocation() }
                            else showLocationDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (locLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (locLoading) "Detecting..." else "Use my location")
                    }

                    Text("Optional", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }

                PickerField("Birth date (optional)", birthDate, "DD-MM-YYYY") { openDatePicker() }
                if (derivedAge != null) {
                    Text(
                        "Age: $derivedAge",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onBackground.copy(alpha = 0.65f),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                PickerField("Gender", gender, "Select gender") { showGenderDialog = true }

                // ✅ Quick tag picks card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Quick tag picks", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Tap to add/remove (max 5). These personalize your feed.",
                                    color = cs.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { setTags(emptyList()) }) { Text("Clear") }
                        }

                        QuickChipRow("Brands", quickBrands)
                        QuickChipRow("Styles", quickStyles)
                        QuickChipRow("Items", quickItems)
                    }
                }

                // Tags (manual)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your fashion tags (up to 5)", fontWeight = FontWeight.SemiBold)
                        Text("Brands, styles, categories — used to personalize your feed.", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

                        OutlinedTextField(value = tag1, onValueChange = { tag1 = it }, label = { Text("Tag 1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tag2, onValueChange = { tag2 = it }, label = { Text("Tag 2") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tag3, onValueChange = { tag3 = it }, label = { Text("Tag 3") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tag4, onValueChange = { tag4 = it }, label = { Text("Tag 4") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tag5, onValueChange = { tag5 = it }, label = { Text("Tag 5") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }

                if (error != null) Text(error!!, color = cs.error)
                Spacer(Modifier.height(12.dp))
            }

            // Bottom button pinned
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 10.dp)
            ) {
                Button(
                    onClick = {
                        error = null
                        val n = name.trim()
                        val u = username.trim()
                        val c = country.trim()

                        if (n.isBlank()) { error = "Please enter your full name."; return@Button }
                        if (u.isBlank()) { error = "Please choose a username."; return@Button }
                        if (c.isBlank()) { error = "Please select your country."; return@Button }

                        val tags = currentTags()

                        val birthIso = birthDate.trim().takeIf { it.isNotBlank() }?.let { displayToIso(it) }
                        if (birthDate.isNotBlank() && birthIso == null) {
                            error = "Birth date must be DD-MM-YYYY"
                            return@Button
                        }

                        loading = true
                        scope.launch {
                            try {
                                val token = tokenStore.tokenFlow.first().orEmpty()
                                if (token.isBlank()) {
                                    error = "Session expired. Please sign in again."
                                    return@launch
                                }
                                val bearer = "Bearer $token"

                                // Upload avatar if selected locally
                                var uploadedUrl: String? = avatarUrl
                                val localUri = imageUri?.let { Uri.parse(it) }
                                if (localUri != null) {
                                    val mime = ctx.contentResolver.getType(localUri) ?: "image/jpeg"
                                    val bytes = ctx.contentResolver.openInputStream(localUri)?.use { it.readBytes() }
                                    if (bytes == null || bytes.isEmpty()) {
                                        error = "Could not read selected image."
                                        return@launch
                                    }
                                    val ext = when (mime) {
                                        "image/png" -> "png"
                                        "image/webp" -> "webp"
                                        else -> "jpg"
                                    }
                                    val rb = bytes.toRequestBody(mime.toMediaTypeOrNull())
                                    val part = MultipartBody.Part.createFormData("avatar", "avatar.$ext", rb)

                                    val up = api.profileUploadAvatar(bearer, part)
                                    if (up.ok == true && !up.avatarUrl.isNullOrBlank()) {
                                        uploadedUrl = up.avatarUrl
                                    } else {
                                        error = up.error ?: "Avatar upload failed"
                                        return@launch
                                    }
                                }

                                val saveRes = api.profileSave(
                                    bearer = bearer,
                                    body = ProfileSaveReq(
                                        name = n,
                                        username = u,
                                        country = c,
                                        birthDate = birthIso,
                                        gender = gender,
                                        avatarUrl = uploadedUrl,
                                        tags = tags
                                    )
                                )

                                if (saveRes.ok == true) {
                                    avatarUrl = uploadedUrl

                                    tokenStore.saveProfile(
                                        UserProfile(
                                            imageUri = imageUri,
                                            avatarUrl = avatarUrl,
                                            name = n,
                                            username = u,
                                            country = c,
                                            birthDate = birthDate.trim(),
                                            gender = gender,
                                            tags = tags
                                        )
                                    )

                                    Toast.makeText(ctx, "Profile saved", Toast.LENGTH_SHORT).show()
                                    onDone()
                                } else {
                                    error = saveRes.error ?: "Failed to save profile"
                                }
                            } catch (he: HttpException) {
                                val body = runCatching { he.response()?.errorBody()?.string() }.getOrNull()
                                val parsed = parseServerError(body)
                                error = when {
                                    he.code() == 409 -> parsed ?: "Username already taken"
                                    parsed != null -> parsed
                                    else -> "Server error (${he.code()})"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Network error"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    if (loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = cs.onPrimary)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (loading) "Saving..." else "Continue", color = cs.onPrimary)
                }
            }
        }
    }
}