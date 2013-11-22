(ns com.lemonodor.androne.main
  (:use [neko.activity :only [defactivity set-content-view!]]
        [neko.threading :only [on-ui]]
        [neko.ui :only [make-ui]]
        [neko.ui.mapping :as mapping]
        [neko.ui.traits :as traits]
        )
  (:import
   (android.app Activity)
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


(defactivity com.lemonodor.androne.AndroneActivity
  :def a
  :on-create
  (fn [this bundle]
    (on-ui
     (set-content-view! a
      (make-ui [:linear-layout {}
                [:text-view {:text "Hello from Clojure!"}]])))))
