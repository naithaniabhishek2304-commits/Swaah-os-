package com.swaah.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Gold = Color(0xFFD6B15E)
private val Ink = Color(0xFF0B0D10)
private val Panel = Color(0xFF171A20)
private val Soft = Color(0xFFB8BBC3)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var speechReady = false

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        setContent {
            SWAAHScreen(
                onListen = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) startListening()
                    else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                },
                onSpeak = { speak(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val result = engine.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale("hi", "IN"))
            }
            // Android chooses an installed voice. Voice gender is not exposed consistently
            // across TTS engines, so the exact female voice depends on installed voice data.
            speechReady = true
        }
    }

    private fun speak(text: String) {
        if (speechReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "swaah_reply")
    }

    private fun startListening() {
        val r = recognizer ?: return
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val phrase = words?.firstOrNull().orEmpty()
                if (phrase.isNotBlank()) onRecognized?.invoke(phrase)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to SWAAH")
            // Ask supported recognizers to detect a language switch automatically.
            putExtra("android.speech.extra.LANGUAGE_DETECTION_ALLOWED", true)
        }
        r.startListening(intent)
    }


    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    @Composable
    private fun SWAAHScreen(onListen: () -> Unit, onSpeak: (String) -> Unit) {
        val scope = rememberCoroutineScope()
        var apiKey by remember { mutableStateOf(getPreferences(0).getString("gemini_key", "") ?: "") }
        var input by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("Namaste! Main SWAAH hoon. Boliye, aaj kya karna hai?") }
        var status by remember { mutableStateOf("Ready") }
        var busy by remember { mutableStateOf(false) }

        // Deliver recognised speech into Compose state.

        MaterialTheme(colorScheme = darkColorScheme(
            primary = Gold, background = Ink, surface = Panel,
            onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
        )) {
            Surface(Modifier.fillMaxSize(), color = Ink) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))
                    Text("SWAAH", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("PERSONAL AI OPERATOR", color = Soft, fontSize = 11.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(32.dp))
                    Box(
                        Modifier.size(170.dp).background(Panel, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Gold, fontSize = 88.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(status.uppercase(), color = Gold, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onListen,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(58.dp)
                    ) { Text("🎙  TALK TO SWAAH", color = Ink, fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        label = { Text("Type or speak in Hindi / English") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, focusedLabelColor = Gold,
                            unfocusedBorderColor = Soft, focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = input.isNotBlank() && !busy,
                        onClick = {
                            val question = input.trim()
                            busy = true
                            status = "Thinking…"
                            scope.launch {
                                val result = askGemini(apiKey, question)
                                answer = result
                                status = if (result.startsWith("SWAAH setup")) "Setup needed" else "Response ready"
                                busy = false
                                onSpeak(result)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "THINKING…" else "ASK SWAAH", color = Ink) }

                    Spacer(Modifier.height(22.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("SWAAH RESPONSE", color = Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(answer, color = Color.White, lineHeight = 23.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("GEMINI API KEY", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = { Text("Paste your Gemini API key") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, focusedLabelColor = Gold,
                            unfocusedBorderColor = Soft, focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            getPreferences(0).edit().putString("gemini_key", apiKey.trim()).apply()
                            status = "API key saved on this device"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Panel)
                    ) { Text("SAVE API KEY", color = Gold) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Prototype note: voice recognition may use Google's speech service. Gemini requests go online. Keep your API key private.",
                        color = Soft, fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }
        }
    }

    private suspend fun askGemini(key: String, prompt: String): String = withContext(Dispatchers.IO) {
        if (key.isBlank()) return@withContext "SWAAH setup: Pehle Gemini API key add karke Save API Key dabaiye."
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val systemPrompt = "You are SWAAH, a helpful personal AI operator. Reply naturally in the same language as the user, switching between Hindi and English when they do. Be concise, practical and friendly. Do not claim to perform phone actions unless actually integrated."
            val body = JSONObject()
                .put("system_instruction", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", systemPrompt))))
                .put("contents", org.json.JSONArray().put(
                    JSONObject().put("role", "user").put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
                ))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (code !in 200..299) return@withContext "Gemini error ($code): ${runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull() ?: "Request failed"}"
            val json = JSONObject(response)
            json.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Connection issue: ${e.localizedMessage ?: "Please try again."}"
        }
    }
}
