package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import coil.compose.rememberAsyncImagePainter
import android.Manifest
import android.content.Intent
import androidx.core.app.ActivityCompat
import com.example.androidhw.ui.theme.SensorService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.ui.platform.LocalContext


class MainActivity : ComponentActivity() {

    private val NOTIFICATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()


        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = this@MainActivity
                var imageUri by remember { mutableStateOf(loadUri(context))}
                var name by remember { mutableStateOf(loadName(context))}
                NavHost(
                    navController = navController,
                    startDestination = "conversationPage"
                ){
                    composable("conversationPage") {ConversationPage(navController, name, imageUri)}
                    composable("settingsPage"){SettingsPage(
                        navController,
                        context,
                        name,
                        imageUri,
                        onNameChange = {
                        name = it
                        saveName(context, it)
                    }, onImageChange = {
                        imageUri = it
                        saveUri(context, it)
                    }
                        )}
                }
            }

        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_HEALTH) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_HEALTH)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            println("Requesting permissions: $permissionsToRequest")
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        } else {
            println("All permissions are already granted.")
            startSensorService()
        }
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }
}

data class Message(val author: String, val body: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPage(navController: NavController, userName: String, imageUri: Uri?) {
    val context = LocalContext.current
    var inputMessage by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(loadMessages(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Conversations") },
                actions = {
                    IconButton(onClick = { navController.navigate("settingsPage") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (inputMessage.isNotEmpty()) {
                        val newMessage = Message(author = userName, body = inputMessage)
                        messages = messages + newMessage
                        inputMessage = ""
                        saveMessages(context, messages)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Send")
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(messages) { message ->
                    MessageCard(message, userName = userName, imageUri = imageUri)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavController,
                 context: Context,
                 name: String,
                 imageUri: Uri?,
                 onNameChange: (String) -> Unit,
                 onImageChange: (Uri) -> Unit){
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri: Uri? -> uri?.let{onImageChange(it)} }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings")},
                navigationIcon = {
                    IconButton(onClick = {navController.popBackStack()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageUri?.let{
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { imagePicker.launch("image/*") }
                )
            }?: run{
                Image (
                    painter = painterResource(id = R.drawable.androidd),
                    contentDescription = "Default profile picture",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { imagePicker.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = name,
                onValueChange = { onNameChange(it) },
                modifier = Modifier.padding(8.dp),
                textStyle = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun saveUri(context: Context, uri: Uri){
    val pref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val file = File(context.filesDir, "profile_image.jpg")

    try {
        val input: InputStream? = context.contentResolver.openInputStream(uri)
        val output = FileOutputStream(file)
        input?.copyTo(output)

        input?.close()
        output.close()

        pref.edit().putString("image_uri", file.absolutePath).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadUri(context: Context): Uri? {
    val pref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val path = pref.getString("image_uri", null)
    return path?.let {Uri.fromFile(File(it))}
}

fun saveName(context: Context, name: String){
    val pref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return pref.edit().putString("user_name", name).apply()
}

fun loadName(context: Context): String{
    val pref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return pref.getString("user_name", "Default user")?: "User"
}

@Composable
fun MessageCard(msg: com.example.myapplication.Message, userName: String? = null, imageUri: Uri? = null) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        val painter: Painter = imageUri?.let {
            coil.compose.rememberAsyncImagePainter(it)
        } ?: painterResource(R.drawable.androidd)

        Image(
            painter = painter,
            contentDescription = "null",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        var isExpanded by remember { mutableStateOf(false)}
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = userName ?: msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if(isExpanded)Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun saveMessages(context: Context, messages: List<Message>) {
    val gson = Gson()
    val json = gson.toJson(messages)
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("messages", json).apply()
}

fun loadMessages(context: Context): List<Message> {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("messages", null)
    return if (json != null) {
        val gson = Gson()
        val type = object : TypeToken<List<Message>>() {}.type
        gson.fromJson(json, type)
    } else {
        emptyList()
    }
}

@Preview(name = "Light mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark mode"
)

@Preview
@Composable
fun PreviewMessageCard() {
    MyApplicationTheme{
        Surface {
            MessageCard(
                msg = Message("lexi", "the compose"))
        }
    }
}

@Composable
fun Conversation(messages: List<Message>, userName: String, imageUri: Uri?){
    LazyColumn {
        items(messages) {message ->
            MessageCard(message, userName = userName, imageUri = imageUri)
        }
    }
}



