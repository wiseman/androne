(ns com.lemonodor.androne.icp)


(defn index-concept [concept frame]
  (apply merge-with concat
         (map #(into {} (map (fn [word] [(str word) [concept]]) %1))
              (frame :index-sets))))

(defn index-concepts [world]
  (apply merge-with concat
         (map #(apply index-concept %1) world)))

(defn find-indices [world words]
  [])

(defn score-index-sets [world indices]
  [])

(defn icp [world words]
  (-> world
      (find-indices words)
      score-index-sets))
