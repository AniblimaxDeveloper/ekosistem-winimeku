package id.winimeku.wibinime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlinx.coroutines.launch // <--- IMPORT PENTING

// --- RETROFIT API ---
data class LoginReq(val email: String, val password: String)
data class LoginRes(val token: String?, val role: String?, val error: String?)
data class Episode(val id: String, val episodeNumber: Int, val videoUrl: String)
data class Anime(val id: String, val title: String, val synopsis: String?, val coverImage: String?, val episodes: List<Episode>?)

interface ApiService {
    @POST("api/v1/auth/login") suspend fun login(@Body req: LoginReq): LoginRes
    @GET("api/v1/anime") suspend fun getAnime(): List<Anime>
}
object Retro {
    const val BASE_URL = "http://192.168.1.10:3000/" 
    val api: ApiService by lazy { Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java) }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var token by remember { mutableStateOf<String?>(null) }
            var animeSelected by remember { mutableStateOf<Anime?>(null) }
            var videoUrl by remember { mutableStateOf<String?>(null) }

            Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                when {
                    token == null -> LoginScreen { token = it }
                    animeSelected != null -> DetailScreen(animeSelected!!) { videoUrl = it }
                    videoUrl != null -> PlayerScreen(videoUrl!!) { videoUrl = null }
                    else -> HomeScreen { animeSelected = it }
                }
            }
        }
    }
}

@Composable fun LoginScreen(onSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("admin@winimeku.com") }
    var pass by remember { mutableStateOf("password123") }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // <--- GUNAKAN SCOPE COMPOSE
    
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("WIBINIME LOGIN", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        if (err.isNotEmpty()) Text(err, color = Color.Red)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            scope.launch { // <--- LAUNCH DI SINI
                try {
                    val res = Retro.api.login(LoginReq(email, pass))
                    if (res.token != null) onSuccess(res.token) else err = res.error ?: "Gagal"
                } catch (e: Exception) { err = "Error koneksi: ${e.message}" }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("MASUK") }
    }
}

@Composable fun HomeScreen(onSelect: (Anime) -> Unit) {
    var list by remember { mutableStateOf<List<Anime>>(emptyList()) }
    LaunchedEffect(Unit) {
        try { list = Retro.api.getAnime() } catch (e: Exception) {}
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("TRENDING ANIME", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp)) }
        items(list) { anime ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(anime) }.padding(vertical = 8.dp)) {
                AsyncImage(model = anime.coverImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp, 110.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(anime.title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(anime.synopsis ?: "", color = Color.Gray, maxLines = 2)
                }
            }
        }
    }
}

@Composable fun DetailScreen(anime: Anime, onPlay: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(anime.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(anime.synopsis ?: "", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text("EPISODES", color = Color.White, fontWeight = FontWeight.Bold)
        LazyColumn {
            items(anime.episodes ?: emptyList()) { ep ->
                Button(onClick = { onPlay(ep.videoUrl) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Episode ${ep.episodeNumber}")
                }
            }
        }
    }
}

@Composable fun PlayerScreen(url: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
        Button(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Text("Kembali") }
    }
}
