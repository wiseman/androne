(ns com.lemonodor.androne.main
  (:use
   [com.lemonodor.androne.speech :as speech]
   [neko.activity :only [defactivity set-content-view!]]
   [neko.context :as context]
   [neko.log :as log]
   [neko.threading :only [on-ui]]
   [neko.ui :as ui]
   [neko.ui.mapping :as mapping]
   [neko.ui.traits :as traits]
   )
  (:import
   (android.app Activity)
   (android.content Context)
   (android.media AudioManager)
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

;;(traits/deftrait :type-face
;;  [^TextView wdg, {:keys [typeface]} _]


(def recognizer (atom nil))
(def tts (atom nil))
(def audio-manager (atom nil))


(defn listen []
  (log/i "STARTING LISTENING")
  (.setStreamMute @audio-manager AudioManager/STREAM_SYSTEM true)
  (speech/start-listening @recognizer))


(declare ^android.widget.LinearLayout mylayout)

(def main-layout
  [:linear-layout {:id-holder true
                   :def `mylayout
                   :orientation :vertical}
   [:text-view {:id ::audio-level
                :text ""
                :typeface android.graphics.Typeface/MONOSPACE
                }]
   [:text-view {:id ::recognized-text
                :text ""}]
   [:text-view {:id ::speech-status
                :text ""}]])


(defn set-elmt [elmt s]
  (on-ui (config (elmt (.getTag mylayout)) :text s)))


(defn handle-speech-results [^Bundle results]
  (let [texts (speech/speech-results results)]
    (log/i :on-speech-results (str "\n\n\n" texts "\n\n\n"))
    (set-elmt ::recognized-text (first texts))
    (set-elmt ::speech-status "")
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
  (set-elmt ::speech-status (speech/error-name error))
  (when (not (= error SpeechRecognizer/ERROR_RECOGNIZER_BUSY))
    (listen)))


(def audio-rms-bounds (atom [nil nil]))

(defn update-rms [rms]
  (swap! audio-rms-bounds
         (fn [bounds]
           [(if-let [prev-lower-bound (bounds 0)]
              (min rms prev-lower-bound)
              rms)
            (if-let [prev-upper-bound (bounds 1)]
              (max rms prev-upper-bound)
              rms)]))
  (let [[lower-bound upper-bound] @audio-rms-bounds
        mag (- rms lower-bound)
        frac (/ mag (max 1 (- upper-bound lower-bound)))
        width 40
        filled (unchecked-int (* width frac))
        unfilled (- width filled)]
    (set-elmt ::audio-level
              (str "|"
                   (apply str (repeat filled "="))
                   (apply str (repeat unfilled " "))
                   "|"))))

(defn handle-ready-for-speech [_]
  (.setStreamMute @audio-manager AudioManager/STREAM_SYSTEM false))


(defactivity com.lemonodor.androne.AndroneActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (ui/make-ui main-layout))
     (reset! audio-manager
             (.getSystemService context/context Context/AUDIO_SERVICE))
     (reset! tts (android.speech.tts.TextToSpeech. context/context nil))
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
        :on-rms-changed update-rms)))
     (log/i "Created speech recognizer")
     (speech/start-listening @recognizer)
     (log/i "Started listening"))))
