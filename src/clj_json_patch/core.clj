(ns clj-json-patch.core
  (:use [clj-json-patch.util]))


(defn patch
  "Applies a JSON patch document (multiple patches) to JSON object."
  [obj patches]
  
  (comment (loop [patches patches
         result obj]
    (if (empty? patches)
      result
      (recur (rest patches)
             (apply-patch result (first patches))))))
  (reduce #(apply-patch %1 %2) obj patches))

(defn diff
  "Prepares a JSON patch document representing the difference
   between two JSON objects."
  ([obj1 obj2] (diff obj1 obj2 "/"))
  ([obj1 obj2 prefix]
     (transform-moves obj1 obj2
                      (cond (and (vector? obj1) (vector? obj2))
                            (diff-vecs obj1 obj2 prefix)
                            (and (map? obj1) (map? obj2))
                            (vec
                             (flatten
                              (remove nil?
                                      (concat
                                       (for [[k v1] obj1]
                                         (let [v2 (get obj2 k)]
                                           (cond (and (vector? v1) (vector? v2))
                                                 (diff v1 v2 (str prefix k "/"))
                                                 (not (contains? obj2 k))
                                                 (gen-op ["remove" (str prefix k)])
                                                 (and (map? v1) (map? v2))
                                                 (diff v1 v2 (str prefix k "/"))
                                                 (and (contains? obj2 k)
                                                      (not= v1 v2))
                                                 (gen-op ["replace" (str prefix k) v2]))))
                                       (for [[k v2] obj2]
                                         (let [v1 (get obj1 k)]
                                           (cond (not (contains? obj1 k))
                                                 (gen-op ["add" (str prefix k) v2]))))))))))))
