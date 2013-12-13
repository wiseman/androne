(ns com.lemonodor.androne.main
  (:require
   [com.lemondronor.ar-drone.core :as ar-drone]
   [clojure.string :as string]
   ;[clojure.tools.logging :as log]
   [com.lemonodor.androne.fdl :as fdl]
   [com.lemonodor.androne.icp :as icp]
   [com.lemonodor.androne.speech :as speech]
   [neko.activity :as activity]
   [neko.context :as context]
   [neko.log :as log]
   [neko.threading :as threading]
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


(defn log-it [& args]
  (let [msg (apply str (interpose " "args))]
    (log/i "WOO>>>> " msg)))


(def world
  (fdl/defworld
    [take-off
     :index-sets [[take off]
                  [takeoff]]
     :action :do-take-off]
    [land
     :index-sets [[land] [abort] [emergency]]
     :action :do-land]
    [forward
     :parent relative-direction
     :index-sets [[forward]]]
    [backward
     :parent relative-direction
     :index-sets [[backward]]]
    ;; [move
    ;;  :constraints
    ;;  [[direction relative-direction]]
    ;;  :phrases
    ;;  [[move (direction)]]]
    ))


(def drone (agent false))

(defn act-on-drone [drone fn args]
  (when-not drone
    (log-it "Initializing drone.")
    (ar-drone/drone-initialize))
  (apply fn args)
  true)

(defn act-on-drone! [fn & args]
  (send-off drone act-on-drone fn args))


(defn do-land [parse]
  (log-it "Drone is landing.")
  (act-on-drone! #(ar-drone/drone :land)))

(defn do-take-off [parse]
  (log-it "Drone is taking off.")
  (act-on-drone! #(ar-drone/drone :take-off)))


(def drone-actions
  {:do-land do-land
   :do-take-off do-take-off})


(defn action-for-concept [concept]
  (drone-actions
   (fdl/get-slot world concept :action)))


(defn perform-drone-action-for-concept [concept]
  (act-on-drone! (action-for-concept concept) (list concept)))


(defn perform-action-for-concept [world concept]
  (log-it "Performing action" concept)
  (let [action-sym (fdl/get-slot world concept :action)
        action-function (drone-actions action-sym)]
    (log-it "Action sym:" action-sym
           "Action function:" action-function)
    (when action-function
      (log-it "Calling" action-function)
      (apply action-function (list concept)))))


(def speech-recognizer (atom nil))
(def tts (atom nil))
(def audio-manager (atom nil))


(defn set-mute! [^AudioManager audio-manager mute?]
  (.setStreamMute audio-manager AudioManager/STREAM_SYSTEM mute?))


(defn listen []
  (log-it "STARTING LISTENING")
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
  (threading/on-ui (ui/config (elmt (.getTag mylayout)) :text s)))


(defn handle-speech-results [^Bundle results]
  (set-elmt ::speech-status "")
  (let [texts (speech/speech-results results)]
    (log-it :on-speech-results (str "\n\n\n" texts "\n\n\n"))
    (let [best-text (first texts)
          parses (icp/icp world (string/split best-text #"\s+"))]
      (log-it "Best text" best-text)
      (log-it "Parses" parses)
      (set-elmt ::recognized-text best-text)
      (let [best-parse (first parses)]
        (when best-parse
          (set-elmt ::parse (str best-parse))
          (perform-action-for-concept world (first best-parse)))))

    ;; (log-it "Speaking")
    ;; (.setOnUtteranceProgressListener
    ;;  @tts
    ;;  (speech/utterance-progress-listener
    ;;   :on-done (fn [utterance-id]
    ;;              (log-it "TTS :on-done" utterance-id)
    ;;              (listen))
    ;;   :on-error (fn [utterance-id]
    ;;               (log-it "TTS :on-error" utterance-id)
    ;;               (listen))
    ;;   :on-start (fn [utterance-id]
    ;;               (log-it "TTS :on-start" utterance-id))))
    ;; (.speak
    ;;  @tts
    ;;  (first texts)
    ;;  android.speech.tts.TextToSpeech/QUEUE_ADD
    ;;  (java.util.HashMap.
    ;;   {android.speech.tts.TextToSpeech$Engine/KEY_PARAM_UTTERANCE_ID "1"}))
    (listen)
    ))


(defn handle-speech-error [error]
  (log-it :on-speech-error error (str "(" (speech/error-name error) ")"))
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
  (let [[lower-bound upper-bound] (swap! audio-rms-bounds update-bounds rms)
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


(activity/defactivity com.lemonodor.androne.AndroneActivity
  :def a
  :on-create
  (fn [this bundle]
    (threading/on-ui
     (activity/set-content-view! a
      (ui/make-ui main-layout))
     (log-it "WORLD" world)
     (reset! audio-manager
             (.getSystemService context/context Context/AUDIO_SERVICE))
     (reset!
      speech-recognizer
      (speech/create-recognizer
       (speech/recognizer-listener
        :on-beginning-of-speech #(log-it :on-beginning-of-speech)
        :on-buffer-received #(log-it :on-buffer-received %1)
        :on-end-of-speech #(log-it :on-end-of-speech)
        :on-error handle-speech-error
        :on-event #(log-it :on-event %1 %2)
        :on-partial-results #(log-it :on-partial-results %1)
        :on-ready-for-speech #(log-it :on-ready-for-speech %1)
        :on-results handle-speech-results
        :on-rms-changed update-rms)))
     (log-it "Created speech recognizer")
     (reset! tts (android.speech.tts.TextToSpeech. context/context nil))
     (speech/start-listening @speech-recognizer)
     (log-it "Started listening"))))
