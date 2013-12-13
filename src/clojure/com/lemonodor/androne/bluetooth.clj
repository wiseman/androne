(ns com.lemonodor.androne.bluetooth
  (:require
   [neko.-utils :as utils]
   [neko.context :as context]
   [neko.log :as log])
  (:import
   [android.bluetooth
    BluetoothAdapter BluetoothProfile BluetoothProfile
    BluetoothProfile$ServiceListener]
   [com.lemondronor BluetoothHeadsetUtils]))


(defn bluetooth-helper [context & {:keys [on-sco-audio-disconnected
                                          on-sco-audio-connected
                                          on-headset-disconnected
                                          on-headset-connected]}]
  (proxy [BluetoothHeadsetUtils]
      [context]
    (onScoAudioDisconnected []
      (utils/call-if-nnil on-sco-audio-disconnected))
    (onScoAudioConnected []
      (utils/call-if-nnil on-sco-audio-connected))
    (onHeadsetDisconnected []
      (utils/call-if-nnil on-headset-disconnected))
    (onHeadsetConnected []
      (utils/call-if-nnil on-headset-connected))))


(defn start-helper [^BluetoothHeadsetUtils helper]
  (.start helper))

(defn stop-helper [^BluetoothHeadsetUtils helper]
  (.stop helper))


(defn service-listener
  [& {:keys [on-connected on-disconnected]}]
  (reify BluetoothProfile$ServiceListener
    (onServiceConnected [this profile proxy]
      (utils/call-if-nnil on-connected profile proxy))
    (onServiceDisconnected [this profile]
      (utils/call-if-nnil on-disconnected profile))))


(defn get-proxy [listener]
  (let [^BluetoothAdapter adapter
        (BluetoothAdapter/getDefaultAdapter)]
    (log/i "BluetoothAdapter" adapter)
    (.getProfileProxy
     adapter
     context/context
     listener
     BluetoothProfile/HEADSET)))
