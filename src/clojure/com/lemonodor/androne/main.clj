(ns com.lemonodor.androne.main
  (:use
   [clojure.string :as string]
   [com.lemonodor.androne.fdl :as fdl]
   [com.lemonodor.androne.icp :as icp]
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
   [android.app Activity]
   [android.content Context]
   [android.media AudioManager]
   [android.os Bundle]
   [android.speech SpeechRecognizer]
   [android.util Log]
   ))


(def world
  (fdl/defworld
    [take-off
     :index-sets
     [[take off]
      [takeoff]]
     :action do-take-off]
    [land
     :index-sets
     [[land] [abort] [emergency]]
     :action do-land]
    [forward
     :parent relative-direction
     :index-sets
     [[forward]]]
    [backward
     :parent relative-direction
     :index-sets
     [[backward]]]
    ;; [move
    ;;  :constraints
    ;;  [[direction relative-direction]]
    ;;  :phrases
    ;;  [[move (direction)]]]
    ))


(def speech-recognizer (atom nil))
(def tts (atom nil))
(def audio-manager (atom nil))


(defn set-mute! [^AudioManager audio-manager mute?]
  (.setStreamMute audio-manager AudioManager/STREAM_SYSTEM mute?))


(defn listen []
  (log/i "STARTING LISTENING")
  (set-mute! @audio-manager true)
  (speech/start-listening @speech-recognizer))


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
   [:text-view {:id ::parse
                :text ""}]
   [:text-view {:id ::speech-status
                :text ""}]])


(defn set-elmt [elmt s]
  (on-ui (config (elmt (.getTag mylayout)) :text s)))


(defn handle-speech-results [^Bundle results]
  (set-elmt ::speech-status "")
  (let [texts (speech/speech-results results)]
    (log/i :on-speech-results (str "\n\n\n" texts "\n\n\n"))
    (let [best-text (first texts)
          parses (icp/icp world best-text)]
      (set-elmt ::recognized-text best-text)
      (when parses
        (set-elmt ::parse (first (first parses)))))
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
  (when-not (= error SpeechRecognizer/ERROR_RECOGNIZER_BUSY)
    (listen)))


(def audio-rms-bounds (atom [nil nil]))

(defn update-bounds [[prev-lower-bound prev-upper-bound] v]
  [(if prev-lower-bound
     (min v prev-lower-bound)
     v)
   (if prev-upper-bound
     (max v prev-upper-bound)
     v)])

(defn update-rms [rms]
  (let [[lower-bound upper-bound] (swap! audio-rms-bounds update-bounds)
        mag (- rms lower-bound)
        frac (/ mag (max 1 (- upper-bound lower-bound)))
        width 40
        filled (unchecked-int (* width frac))
        unfilled (- width filled)]
    (set-elmt ::audio-level
              (str "|"
                   (string/join (repeat filled "="))
                   (string/join (repeat unfilled " "))
                   "|"))))

(defn handle-ready-for-speech [_]
  (set-mute! @audio-manager false))


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
      speech-recognizer
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
     (speech/start-listening @speech-recognizer)
     (log/i "Started listening"))))
