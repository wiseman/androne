(ns com.lemonodor.androne.main
  (:use
   [com.lemonodor.androne.speech :as speech]
   [neko.activity :only [defactivity set-content-view!]]
   [neko.log :as log]
   [neko.threading :only [on-ui]]
   [neko.ui :only [make-ui]]
   [neko.ui.mapping :as mapping]
   [neko.ui.traits :as traits]
   )
  (:import
   (android.app Activity)
   (android.os Bundle)
   (android.speech SpeechRecognizer)
   (android.util Log)
   (android.widget ProgressBar)))


(mapping/defelement :progress-bar
  :classname ProgressBar
  :inherits :view)

(traits/deftrait :progress
  [^ProgressBar wdg, {:keys [progress]} _]
  (.setProgress wdg progress))

(traits/deftrait :max
  [^ProgressBar wdg, {:keys [max]} _]
  (.setMax wdg max))

(traits/deftrait :indeterminate
  [^ProgressBar wdg, {:keys [indeterminate?]} _]
  (.setIndeterminate wdg indeterminate?))


(def recognizer (atom nil))
(def tts (atom nil))


(defn listen []
  (log/i "STARTING LISTENING")
  (speech/start-listening @recognizer))


(defn handle-speech-results [^Bundle results]
  (log/i :on-speech-results results)
  (let [texts (speech/speech-results results)]
    ;; (log/i "Speaking")
    ;; (.setOnUtteranceProgressListener
    ;;  @tts
    ;;  (speech/utterance-progress-listener
    ;;   :on-done (fn [utterance-id]
    ;;              (log/i "TTS :on-done" utterance-id)
    ;;              (listen))
    ;;   :on-error (fn [utterance-id]
    ;;               (log/i "TTS :on-error" utterance-id)
    ;;               (listen))
    ;;   :on-start (fn [utterance-id]
    ;;               (log/i "TTS :on-start" utterance-id))))
    ;; (.speak
    ;;  @tts
    ;;  (first texts)
    ;;  android.speech.tts.TextToSpeech/QUEUE_ADD
    ;;  (java.util.HashMap.
    ;;   {android.speech.tts.TextToSpeech$Engine/KEY_PARAM_UTTERANCE_ID "1"}))
    (listen)
    ))


(defn handle-speech-error [error]
  (log/i :on-speech-error error (str "(" (speech/error-name error) ")"))
  (when (not (= error SpeechRecognizer/ERROR_RECOGNIZER_BUSY))
    (listen)))


(defactivity com.lemonodor.androne.AndroneActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (make-ui [:linear-layout {}
                [:progress-bar {:indeterminate false :progress 50 :max 100}]
                [:text-view {:text "Hello from Clojure!"}]]))
     (reset! tts (android.speech.tts.TextToSpeech. neko.context/context nil))
     (reset!
      recognizer
      (speech/create-recognizer
       (speech/recognizer-listener
        :on-beginning-of-speech #(log/i :on-beginning-of-speech)
        :on-buffer-received #(log/i :on-buffer-received %1)
        :on-end-of-speech #(log/i :on-end-of-speech)
        :on-error handle-speech-error
        :on-event #(log/i :on-event %1 %2)
        :on-partial-results #(log/i :on-partial-results %1)
        :on-ready-for-speech #(log/i :on-ready-for-speech %1)
        :on-results handle-speech-results
        :on-rms-changed #(log/i :on-rms-changed %1))))
     (log/i "Created speech recognizer")
     (speech/start-listening @recognizer)
     (log/i "Started listening"))))
