package com.jarvis.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF070B18)
private val Panel = Color(0xFF111A2D)
private val Cyan = Color(0xFF53DFFF)
private val Violet = Color(0xFF9B6BFF)
private val Muted = Color(0xFF8FA3C2)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("jarvis_preferences", MODE_PRIVATE)
        val first = prefs.getBoolean("first_launch", true)
        val greeting = if (first) {
            prefs.edit().putBoolean("first_launch", false).apply()
            "Merhaba Baba."
        } else {
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Günaydın Baba."
                in 12..17 -> "İyi günler Baba."
                else -> "İyi akşamlar Baba."
            }
        }
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val language = tts?.setLanguage(Locale("tr", "TR"))
                if (language != TextToSpeech.LANG_MISSING_DATA &&
                    language != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "jarvis-greeting")
                }
            }
        }
        setContent {
            MaterialTheme {
                JarvisHome(greeting) { text ->
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-reply")
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun JarvisHome(greeting: String, speak: (String) -> Unit) {
    val context = LocalContext.current
    var command by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("Ana Sayfa") }
    var status by remember { mutableStateOf("HAZIR") }
    var messages by remember { mutableStateOf(loadMessages(context).ifEmpty { listOf("JARVIS" to greeting) }) }

    LaunchedEffect(messages) { saveMessages(context, messages) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.let { command = it }
        status = "HAZIR"
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchSpeech(speechLauncher) else status = "MİKROFON İZNİ GEREKLİ"
    }

    fun listen() {
        status = "DİNLİYOR"
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) {
            launchSpeech(speechLauncher)
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun send() {
        val sent = command.trim()
        if (sent.isBlank()) return
        val reply = handlePhoneCommand(context, sent) ?: localReply(sent)
        messages = (messages + ("BABA" to sent) + ("JARVIS" to reply)).takeLast(80)
        command = ""
        speak(reply)
    }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF0A1023), Bg, Color(0xFF090D1B)))
    )) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS", color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Text("PERSONAL AI SYSTEM", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Surface(
                    color = Color(0xFF14233A), shape = CircleShape,
                    modifier = Modifier.size(42.dp).clickable { section = "Ayarlar" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Muted)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111C31))
                    .border(1.dp, Color(0xFF274568), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = Cyan, fontSize = 23.sp)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = command, onValueChange = { command = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { inner ->
                        Box {
                            if (command.isEmpty()) Text("Nöral komutunu yaz...", color = Muted, fontSize = 13.sp)
                            inner()
                        }
                    }
                )
                Icon(Icons.Default.Mic, contentDescription = "Sesli komut", tint = Cyan,
                    modifier = Modifier.size(22.dp).clickable { listen() })
            }

            Spacer(Modifier.height(18.dp))
            Text("HOŞ GELDİN BABA", color = Muted, fontSize = 10.sp, letterSpacing = 3.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(greeting, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp))
            Text("İLK SÜRÜM · YEREL ARAYÜZ", color = Color(0xFFDAAB55), fontSize = 8.sp,
                letterSpacing = 1.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp))

            HologramOrb(Modifier.align(Alignment.CenterHorizontally))
            Text(status, color = Color(0xFF74DDB2), fontSize = 10.sp, letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(18.dp))
            val cards = listOf(
                Triple("Kod & Geliştirme", "Kod yaz · Analiz et", Icons.Default.Code),
                Triple("Kütüphane & Çeviri", "Dosyalar · Diller", Icons.Default.LibraryBooks),
                Triple("Eğitim & Ders", "Öğren · Pratik yap", Icons.Default.LibraryBooks),
                Triple("Medya & Otomasyon", "Medya · Görevler", Icons.Default.Movie)
            )
            cards.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { card ->
                        SectionCard(card.first, card.second, card.third, section == card.first,
                            Modifier.weight(1f)) { section = card.first }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE610192B)).padding(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SOHBET", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f))
                    Text("SOHBETİ TEMİZLE", color = Muted, fontSize = 9.sp,
                        modifier = Modifier.clickable {
                            messages = listOf("JARVIS" to "Sohbet temizlendi Baba. Size nasıl yardımcı olayım?")
                        })
                    Spacer(Modifier.width(10.dp))
                    Text(section.uppercase(Locale("tr")), color = Muted, fontSize = 9.sp)
                }
                LazyColumn(Modifier.heightIn(max = 140.dp).padding(top = 8.dp), reverseLayout = true) {
                    items(messages.reversed()) { message ->
                        Text("${message.first}: ${message.second}",
                            color = if (message.first == "JARVIS") Color(0xFFDCE8FF) else Cyan,
                            fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = command, onValueChange = { command = it },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A1221)).padding(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box {
                                if (command.isEmpty()) Text("JARVIS'e mesaj gönder...", color = Muted, fontSize = 12.sp)
                                inner()
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF174A70), shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp).clickable { send() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color.White)
                        }
                    }
                }
            }
            Text("JARVIS AI  •  SÜRÜM 0.1.0", color = Color(0xFF526783), fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
        }
    }
}



private fun handlePhoneCommand(context: android.content.Context, input: String): String? {
    val q = input.lowercase(Locale("tr", "TR"))

    fun launch(intent: Intent, success: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            success
        } catch (_: Exception) {
            "Bu işlem telefonunuzda açılamadı Baba."
        }
    }

    if (q.startsWith("google'da ara ") || q.startsWith("google da ara ") ||
        q.startsWith("internette ara ") || q.startsWith("web'de ara ")) {
        val query = input.substringAfter("ara", "").trim()
        if (query.isNotBlank()) {
            val uri = android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(query))
            return launch(Intent(Intent.ACTION_VIEW, uri), "İnternet aramasını açıyorum Baba.")
        }
    }

    if (q.contains("alarm kur") || q.contains("alarm ayarla")) {
        val match = Regex("(?:saat\\\\s*)?(\\\\d{1,2})[:.](\\\\d{2})").find(q)
        if (match != null) {
            val hour = match.groupValues[1].toIntOrNull()
            val minute = match.groupValues[2].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                val alarm = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                    .putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                    .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "JARVIS hatırlatması")
                    .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                return launch(alarm, "%02d:%02d için alarm ekranını açıyorum Baba.".format(hour, minute))
            }
        }
        return "Alarm saati belirtin. Örneğin: 07:30 için alarm kur Baba."
    }

    if (q.contains("takvimi aç") || q.contains("takvim aç")) {
        return launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
            "Takvimi açıyorum Baba.")
    }
    if (q.contains("hesap makinesi aç") || q.contains("hesap makinesini aç")) {
        return launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
            "Hesap makinesini açıyorum Baba.")
    }
    if (q.contains("telefon") && (q.contains("ara") || q.contains("arama ekranı"))) {
        return launch(Intent(Intent.ACTION_DIAL), "Telefon arama ekranını açıyorum Baba.")
    }
    val action = when {
        q.contains("wifi ayar") || q.contains("wi-fi ayar") -> android.provider.Settings.ACTION_WIFI_SETTINGS
        q.contains("bluetooth ayar") -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
        q.contains("internet ayar") || q.contains("ağ ayar") -> android.provider.Settings.ACTION_WIRELESS_SETTINGS
        q == "ayarları aç" || q.contains("telefon ayar") -> android.provider.Settings.ACTION_SETTINGS
        q.contains("ekran ayar") -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
        q.contains("ses ayar") -> android.provider.Settings.ACTION_SOUND_SETTINGS
        q.contains("konum ayar") -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
        else -> null
    }
    if (action != null) {
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "İstediğiniz sistem ayarlarını açıyorum Baba."
        } catch (_: Exception) {
            "Bu ayarlar ekranı cihazınızda açılamadı Baba."
        }
    }
    val app = when {
        q.contains("youtube") -> "com.google.android.youtube"
        q.contains("whatsapp") -> "com.whatsapp"
        q.contains("spotify") -> "com.spotify.music"
        q.contains("chrome") -> "com.android.chrome"
        q.contains("harita") -> "com.google.android.apps.maps"
        else -> null
    }
    if (app != null) {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(app)
            if (launch == null) "Bu uygulama telefonunuzda yüklü görünmüyor Baba."
            else {
                context.startActivity(launch)
                "Uygulamayı açıyorum Baba."
            }
        } catch (_: Exception) {
            "Uygulama açılamadı Baba."
        }
    }
    return null
}

private fun loadMessages(context: android.content.Context): List<Pair<String, String>> {
    return try {
        val raw = context.getSharedPreferences("jarvis_preferences", android.content.Context.MODE_PRIVATE)
            .getString("chat_history", null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            item.getString("role") to item.getString("text")
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveMessages(context: android.content.Context, messages: List<Pair<String, String>>) {
    val array = JSONArray()
    messages.takeLast(80).forEach { (role, text) ->
        array.put(JSONObject().put("role", role).put("text", text))
    }
    context.getSharedPreferences("jarvis_preferences", android.content.Context.MODE_PRIVATE)
        .edit().putString("chat_history", array.toString()).apply()
}

private fun localReply(input: String): String {
    val q = input.lowercase(Locale("tr", "TR"))
    val now = Calendar.getInstance()
    return when {
        listOf("saat kaç", "şu an saat", "saat nedir").any { q.contains(it) } ->
            "Şu an saat %02d:%02d Baba.".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        listOf("bugün günlerden", "hangi gün", "tarih ne", "bugünün tarihi").any { q.contains(it) } ->
            "Bugünün tarihi %02d.%02d.%04d Baba.".format(
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR))
        q.contains("sen kimsin") ->
            "Ben JARVIS, kişisel asistanınızım Baba. Şu an temel yerel özelliklerle çalışıyorum; gerçek yapay zekâ bağlantısı henüz eklenmedi."
        listOf("ne yapabilirsin", "yardım", "özelliklerin").any { q.contains(it) } ->
            "Şu an sohbet geçmişini saklayabilir, saat ve tarih gibi temel sorulara cevap verebilir, Türkçe sesli giriş ve sesli yanıt kullanabilirim. İnternet araştırması ve gelişmiş AI henüz bağlı değil Baba."
        q.contains("merhaba") || q.contains("selam") ->
            "Merhaba Baba. Size nasıl yardımcı olabilirim?"
        q.contains("teşekkür") ->
            "Rica ederim Baba. Her zaman yardımcı olmaya hazırım."
        else ->
            "Baba, mesajınızı aldım: \"$input\". Bu sürümde henüz gerçek AI bağlantısı olmadığı için kapsamlı cevap üretemiyorum. Saat, tarih veya mevcut özelliklerim hakkında soru sorabilirsiniz."
    }
}

private fun launchSpeech(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS sizi dinliyor")
    }
    try { launcher.launch(intent) } catch (_: Exception) { }
}

@Composable
private fun HologramOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(modifier.size(208.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension * 0.36f * pulse
            drawCircle(Brush.radialGradient(
                listOf(Color(0x5545CFFF), Color(0x222D68FF), Color.Transparent), c, r * 1.65f
            ), r * 1.65f, c)
            drawCircle(Color(0xFF327DBB).copy(alpha = .28f), r * 1.28f, c, style = Stroke(1.2.dp.toPx()))
            drawCircle(Cyan.copy(alpha = .75f), r, c, style = Stroke(1.6.dp.toPx()))
            drawCircle(Violet.copy(alpha = .65f), r * .78f, c, style = Stroke(1.dp.toPx()))
            drawCircle(Brush.radialGradient(
                listOf(Color(0xFF1A6E9D), Color(0xFF102C58), Color(0xFF090F22)), c, r
            ), r * .68f, c)
            drawArc(Cyan, -25f, 115f, false,
                Offset(c.x - r * 1.12f, c.y - r * 1.12f),
                androidx.compose.ui.geometry.Size(r * 2.24f, r * 2.24f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Violet, 155f, 105f, false,
                Offset(c.x - r * 1.2f, c.y - r * 1.2f),
                androidx.compose.ui.geometry.Size(r * 2.4f, r * 2.4f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = .8f), 3.dp.toPx(),
                c + Offset(r * .36f, -r * .34f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J", color = Color.White, fontSize = 55.sp, fontWeight = FontWeight.Light)
            Text("A I", color = Cyan, fontSize = 10.sp, letterSpacing = 5.sp)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF172B49) else Panel)
            .border(1.dp, if (selected) Cyan.copy(alpha = .65f) else Color(0xFF23334D),
                RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}
