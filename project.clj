(defproject androne/androne "0.0.1-SNAPSHOT"
  :description "FIXME: Android project description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java" "gen"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :dependencies [;[ar-drone "0.1.9a"]
                 [com.taoensso/timbre "3.1.1"]
                 [instaparse "1.2.13"]
                 [org.clojure-android/clojure "1.5.1-jb" :use-resources true]
                 [org.clojure/tools.logging "0.2.6"]
                 [neko/neko "3.0.0"]]
  :profiles {:dev {:dependencies [[android/tools.nrepl "0.2.0-bigstack"]
                                  [midje "1.4.0"]
                                  [org.clojure/math.numeric-tower "0.0.2"]
                                  ;[compliment "0.0.2"]
                                  ]
                   :plugins [[lein-droid "0.2.2"]]
                   :android {:aot :all-with-unused}}
             :release {:android
                       {;; Specify the path to your private keystore
                        ;; and the the alias of the key you want to
                        ;; sign APKs with. Do it either here or in
                        ;; ~/.lein/profiles.clj
                        ;; :keystore-path "/home/user/.android/private.keystore"
                        ;; :key-alias "mykeyalias"

                        :ignore-log-priority [:debug :verbose]
                        :aot :all}}}

  :android {;; Specify the path to the Android SDK directory either
            ;; here or in your ~/.lein/profiles.clj file.
            ;; :sdk-path "/home/user/path/to/android-sdk/"

            ;; Uncomment this if dexer fails with
            ;; OutOfMemoryException. Set the value according to your
            ;; available RAM.
            :dex-opts ["-JXmx4096M"]

            ;; If previous option didn't work, uncomment this as well.
            ;; :force-dex-optimize true

            :target-version "17"
            :aot-exclude-ns ["bench.set"
                             "clojure.core.reducers"
                             "clojure.parallel"
                             "taoensso.timbre.appenders.carmine"
                             "taoensso.timbre.appenders.irc"
                             "taoensso.timbre.appenders.mongo"
                             "taoensso.timbre.appenders.postal"
                             "taoensso.timbre.appenders.socket"
                             "taoensso.timbre.tools.logging"]})
