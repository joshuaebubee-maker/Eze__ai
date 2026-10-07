package com.example.service

import android.content.Intent
import android.speech.RecognitionService

class EzeVoiceRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Recognition callback handled via SpeechRecognizerHelper in assistant sessions
    }

    override fun onCancel(listener: Callback?) {}

    override fun onStopListening(listener: Callback?) {}
}
