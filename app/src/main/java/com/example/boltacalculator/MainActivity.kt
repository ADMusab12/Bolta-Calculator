package com.example.boltacalculator

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var resultTextView: TextView
    private lateinit var speakButton: Button
    private lateinit var urduSpeakButton: Button
    private val openAIService = OpenAIService()
    private var currentLanguage = "en-US"

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        speakButton = findViewById(R.id.speakButton)
        urduSpeakButton = findViewById(R.id.urduSpeakButton)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupSpeechRecognizer()
        } else {
            Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_LONG).show()
            speakButton.isEnabled = false
            urduSpeakButton.isEnabled = false
        }

        speakButton.setOnClickListener {
            if (checkPermission()) {
                currentLanguage = "en-US"
                startVoiceRecognition()
            } else {
                requestPermission()
            }
        }

        urduSpeakButton.setOnClickListener {
            if (checkPermission()) {
                currentLanguage = "ur-PK"
                startVoiceRecognition()
            } else {
                requestPermission()
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    resultTextView.text = if (currentLanguage == "ur-PK")
                        "عمل کر رہا ہے: $spokenText"
                    else
                        "Processing: $spokenText"
                    processWithGemini(spokenText)
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {
                resultTextView.text = if (currentLanguage == "ur-PK") "سن رہا ہے..." else "Listening..."
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> if (currentLanguage == "ur-PK") "آڈیو ریکارڈنگ میں خرابی" else "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> if (currentLanguage == "ur-PK") "کلائنٹ سائیڈ خرابی" else "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> if (currentLanguage == "ur-PK") "ناکافی اجازت" else "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> if (currentLanguage == "ur-PK") "نیٹ ورک خرابی" else "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> if (currentLanguage == "ur-PK") "نیٹ ورک ٹائم آؤٹ" else "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> if (currentLanguage == "ur-PK") "کوئی میچ نہیں ملا" else "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (currentLanguage == "ur-PK") "ریکگنائزر مصروف ہے" else "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> if (currentLanguage == "ur-PK") "سرور خرابی" else "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (currentLanguage == "ur-PK") "کوئی آواز ان پٹ نہیں" else "No speech input"
                    else -> if (currentLanguage == "ur-PK") "سمجھ نہیں آیا، دوبارہ کوشش کریں" else "Didn't understand, please try again."
                }
                resultTextView.text = errorMessage
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }

            // Required but unused RecognitionListener methods
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processWithGemini(spokenText: String) {
        lifecycleScope.launch {
            try {
                val prompt = if (currentLanguage == "ur-PK") {
                    """
                    You are a calculator that responds in Urdu. For the calculation: $spokenText
                    Respond in this exact format:
                    سوال: [repeat the question in Urdu]
                    جواب: [give the answer in Urdu numerals]
                    """
                } else {
                    """
                    You are a calculator. For the calculation: $spokenText
                    Respond in this exact format:
                    Question: [repeat the question]
                    Answer: [give the answer]
                    """
                }

                openAIService.generateContent(prompt)
                    .onSuccess { response ->
                        resultTextView.text = response
                        speakResult(response)
                    }
                    .onFailure { error ->
                        val errorMessage = if (currentLanguage == "ur-PK")
                            "خرابی: ${error.message}"
                        else
                            "Error: ${error.message}"
                        resultTextView.text = errorMessage
                        speakResult(if (currentLanguage == "ur-PK")
                            "معذرت، میں حساب نہیں کر سکا"
                        else
                            "Sorry, I couldn't process that calculation")
                    }
            } catch (e: Exception) {
                val errorMessage = if (currentLanguage == "ur-PK")
                    "خرابی: ${e.message}"
                else
                    "Error: ${e.message}"
                resultTextView.text = errorMessage
                speakResult(if (currentLanguage == "ur-PK")
                    "معذرت، ایک خرابی پیش آگئی"
                else
                    "Sorry, an error occurred")
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (currentLanguage == "ur-PK")
                "اپنا حساب بولیں"
            else
                "Speak your calculation")
        }
        speechRecognizer.startListening(intent)
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    private fun speakResult(text: String) {
        if (currentLanguage == "ur-PK") {
            textToSpeech.language = Locale("ur")
        } else {
            textToSpeech.language = Locale.US
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text-to-Speech language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}