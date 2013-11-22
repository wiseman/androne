(ns com.lemonodor.androne.speech
  (:import android.content.Intent
           android.os.Bundle
           android.speech.tts.UtteranceProgressListener
           (android.speech RecognitionListener RecognizerIntent SpeechRecognizer))
  (:require
   [neko.context :as context]
   [neko.log :as log]
   [neko.notify :as notify]
   [neko.threading :as threading]
   [neko.-utils :as utils]))


(defn utterance-progress-listener
  [& {:keys [on-done on-error on-start]}]
  (proxy [UtteranceProgressListener] []
    (onDone [this utterance-id]
      (utils/call-if-nnil on-done utterance-id))
    (onError [this utterance-id]
      (utils/call-if-nnil on-error utterance-id))
    (onStart [this utterance-id]
      (utils/call-if-nnil on-start utterance-id))))


(defn recognizer-listener
  [& {:keys [on-beginning-of-speech on-buffer-received on-end-of-speech on-error
             on-event on-partial-results on-ready-for-speech on-results on-rms-changed]}]
  (reify RecognitionListener
    (onBeginningOfSpeech [this]
      (utils/call-if-nnil on-beginning-of-speech))
    (onBufferReceived [this buffer]
      (utils/call-if-nnil on-buffer-received buffer))
    (onEndOfSpeech [this]
      (utils/call-if-nnil on-end-of-speech))
    (onError [this error]
      (utils/call-if-nnil on-error error))
    (onEvent [this event-type params]
      (utils/call-if-nnil on-event event-type params))
    (onPartialResults [this partial-results]
      (utils/call-if-nnil on-partial-results partial-results))
    (onReadyForSpeech [this params]
      (utils/call-if-nnil on-ready-for-speech params))
    (onResults [this results]
      (utils/call-if-nnil on-results results))
    (onRmsChanged [this rms-db]
      (utils/call-if-nnil on-rms-changed rms-db))))



(defn create-recognizer [listener]
  (let [recognizer (SpeechRecognizer/createSpeechRecognizer context/context)]
    (.setRecognitionListener recognizer listener)
    recognizer))


(defn start-listening [^ SpeechRecognizer recognizer]
  (let [^Intent intent (Intent. RecognizerIntent/ACTION_RECOGNIZE_SPEECH)]
    (.putExtra intent
               RecognizerIntent/EXTRA_LANGUAGE_MODEL
               RecognizerIntent/LANGUAGE_MODEL_FREE_FORM)
    (.putExtra intent
               RecognizerIntent/EXTRA_CALLING_PACKAGE "org.stuff.events.speech")
    (.putExtra intent
               RecognizerIntent/EXTRA_MAX_RESULTS 5)
    (.startListening recognizer intent)))


(defn speech-results [^Bundle results]
  (log/i "Results:" results)
  (let [data (.getStringArrayList results SpeechRecognizer/RESULTS_RECOGNITION)]
    (log/i "Data:" data)
    data))

(def error-messages
  {
   SpeechRecognizer/ERROR_AUDIO "audio"
   SpeechRecognizer/ERROR_CLIENT "client"
   SpeechRecognizer/ERROR_INSUFFICIENT_PERMISSIONS "insufficient permissions"
   SpeechRecognizer/ERROR_NETWORK "network"
   SpeechRecognizer/ERROR_NETWORK_TIMEOUT "network timeout"
   SpeechRecognizer/ERROR_NO_MATCH "no match"
   SpeechRecognizer/ERROR_RECOGNIZER_BUSY "recognizer busy"
   SpeechRecognizer/ERROR_SERVER "server"
   SpeechRecognizer/ERROR_SPEECH_TIMEOUT "speech timeout"
   })



(defn error-name [error-code]
  (or (error-messages error-code) "unknown"))
