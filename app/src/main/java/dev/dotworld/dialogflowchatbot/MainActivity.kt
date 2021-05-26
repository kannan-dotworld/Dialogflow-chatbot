package dev.dotworld.dialogflowchatbot

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2beta1.*
import com.google.common.collect.Lists
import java.io.InputStream
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener,
    TextToSpeech.OnUtteranceCompletedListener {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private var sessionsClient: SessionsClient? = null
    private var session: SessionName? = null
    private val uuid: String = UUID.randomUUID().toString()
    private var mAudioManager: AudioManager? = null
    private var mStreamVolume = 0

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    private var speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initChatBot()
        startListening()
        textToSpeech = TextToSpeech(this, this)
        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager?;
    }

    private fun initChatBot() {
        try {
            val stream: InputStream = resources.openRawResource(R.raw.credentials)
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"))
            val projectId = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings =
                settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            session = SessionName.of(projectId, uuid)
        } catch (e: Exception) {
            Log.e(TAG, "initV2ChatBot: $e")
        }
    }

    fun callbackChatBot(response: DetectIntentResponse?) {
        if (response != null) {
            runOnUiThread{
                speechRecognizer.destroy()
                speechRecognizer.cancel()
            }
            val botReply = response.queryResult.fulfillmentText
            mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
            playVoice(botReply)
        }
    }

    private fun playVoice(string: String) {
        if (string != null) {
            val myHashAlarm = HashMap<String, String>()
            myHashAlarm[TextToSpeech.Engine.KEY_PARAM_STREAM] = AudioManager.STREAM_ALARM.toString()
            myHashAlarm[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "SOME MESSAGE"
            textToSpeech?.speak(string, TextToSpeech.QUEUE_FLUSH, myHashAlarm)
        }
    }

    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        speechRecognizer.destroy()
        speechRecognizer.cancel()
        super.onDestroy()
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(bundle: Bundle) {
        }

        override fun onBeginningOfSpeech() {
        }

        override fun onRmsChanged(v: Float) {
        }

        override fun onBufferReceived(bytes: ByteArray) {
        }

        override fun onEndOfSpeech() {
        }

        override fun onError(i: Int) {
            Log.e(TAG, "onError: error code :$i")
            speechRecognizer.destroy()
            speechRecognizer.cancel()
            startListening()
        }

        override fun onResults(bundle: Bundle) {
            val matches =
                bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)//getting all the matches
            //displaying the first match
            if (matches != null) {
                Log.d(TAG, "onResults: STT=${matches.toString()}")
                val queryInput: QueryInput = QueryInput.newBuilder()
                    .setText(
                        TextInput.newBuilder().setText(matches.toString())
                            .setLanguageCode("en-US")
                    ).build()
                session?.let { it1 ->
                    sessionsClient?.let { it2 ->
                        DialogflowResponse(
                            this@MainActivity,
                            it1, it2, queryInput
                        ).execute()
                    }
                }

            }
        }

        override fun onPartialResults(bundle: Bundle) {}

        override fun onEvent(i: Int, bundle: Bundle) {}
    }

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC).also {
            if (it != null) {
                mStreamVolume = it
            }
        }
        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        speechRecognizer.setRecognitionListener(recognitionListener)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en")
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.packageName)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

        speechRecognizer.startListening(speechRecognizerIntent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.setOnUtteranceCompletedListener(this);
        }
    }
    override fun onUtteranceCompleted(utteranceId: String?) {
        runOnUiThread {
            startListening()
        }
    }


}