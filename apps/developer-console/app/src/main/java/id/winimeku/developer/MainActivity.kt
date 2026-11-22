package id.winimeku.developer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // <--- INI YANG KETINGGALAN TADI BROW
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlinx.coroutines.launch

data class LoginReq(val email: String, val password: String)
data class LoginRes(val token: String?, val error: String?)
data class AnimeReq(val title: String, val synopsis: String, val coverImage: String)

interface AdminApi {
    @POST("api/v1/auth/login") suspend fun login(@Body req: LoginReq): LoginRes
    @POST("api/v1/admin/anime") suspend fun addAnime(@Header("Authorization") token: String, @Body req: AnimeReq): Any
}
object AdminRetro {
    const val BASE_URL = "http://192.168.1.8:3000/" 
    val api: AdminApi by lazy { Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(AdminApi::class.java) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var token by remember { mutableStateOf<String?>(null) }
            Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                if (token == null) LoginAdmin { token = it } else DashboardAdmin(token!!)
            }
        }
    }
}

@Composable fun LoginAdmin(onSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("admin@winimeku.com") }
    var pass by remember { mutableStateOf("password123") }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("WINIMEKU ADMIN", color = Color.Red, fontSize = 24.sp)
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        TextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Text(msg, color = Color.Red)
        Button(onClick = {
            scope.launch { 
                try {
                    val res = AdminRetro.api.login(LoginReq(email, pass))
                    if (res.token != null) onSuccess(res.token) else msg = "Login Gagal"
                } catch (e: Exception) { msg = e.message ?: "Error" }
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("LOGIN") }
    }
}

@Composable fun DashboardAdmin(token: String) {
    var title by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("TAMBAH ANIME BARU", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        TextField(value = title, onValueChange = { title = it }, label = { Text("Judul Anime") }, modifier = Modifier.fillMaxWidth())
        TextField(value = synopsis, onValueChange = { synopsis = it }, label = { Text("Sinopsis") }, modifier = Modifier.fillMaxWidth())
        TextField(value = cover, onValueChange = { cover = it }, label = { Text("URL Gambar") }, modifier = Modifier.fillMaxWidth())
        Text(status, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = {
            scope.launch { 
                try {
                    AdminRetro.api.addAnime("Bearer $token", AnimeReq(title, synopsis, cover))
                    status = "Sukses menambah Anime!"
                } catch (e: Exception) { status = "Error: ${e.message}" }
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("SIMPAN ANIME") }
    }
}
