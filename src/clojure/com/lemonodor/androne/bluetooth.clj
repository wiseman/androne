(ns com.lemonodor.androne.bluetooth
  (:require
   [neko.-utils :as utils]
   [neko.context :as context]
   [neko.log :as log])
  (:import
   [android.bluetooth BluetoothAdapter BluetoothProfile BluetoothProfile BluetoothProfile$ServiceListener]))


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
