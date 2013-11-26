(ns clj-json-patch.core
  (:require [cheshire.core :as json])
  (:use ;[clj-diff.core :only [clj-diff]]
        [clojure.walk :only [walk]]))

(defn gen-op [t]
  [(let [result {"op" (first t) "path" (second t)}]
    (if (> (count t) 2)
      (assoc result "value" (nth t 2))
      result))] )

(defn diff-vecs [obj1 obj2 prefix]
  (loop [v1 obj1
         v2 obj2
         i 0]
    ;(println "diff-vecs" obj1 obj2 prefix)
    (cond (= v1 (rest v2))
          (gen-op ["add" (str prefix i) (first v2)])
          (= (rest v1) v2)
          (gen-op ["remove" (str prefix i)])
          (and (= (first v1) (first v2))
               (not= (rest v1) (rest v2)))
          (recur (rest v1) (rest v2) (inc i)))))

(defn get-value-path
  "Traverses obj, looking for a value that matches val, returns path to value."
  ([obj val] (get-value-path obj val "/"))
  ([obj val prefix]
     (cond (map? obj)
           (some identity
                 (concat
                  (for [[k v] obj]
                    (if (= v val)
                      (str prefix k)
                      (if-not (string? v)
                        (get-value-path v val (str prefix k "/")))))))
           (vector? obj)
           (if-let [idx (some identity (map-indexed #(if (= val %2) %1) obj))]
               (str prefix idx)
               (map-indexed #(get-value-path %2 val (str prefix %1 "/")) obj)))))

(defn get-patch-value
  "Given the patch path, find the associated value."
  [obj path]
  (if-let [match (re-find #"^/([^/]+)(.*)" path)]
    (let [seg (second match)
          segs (nth match 2)
          val (cond (map? obj)
                    (get obj seg)
                    (vector? obj)
                    (nth obj (Integer/parseInt seg)))]
      (if-not (empty? segs)
        (get-patch-value val segs)
        val))))

(defn transform-moves
  "Attempt to reconcile add/remove patch entries
   to a single move entry"
  [obj1 obj2 patch]
  (loop [adds (filter #(= "add" (get % "op")) patch)
         removes (filter #(= "remove" (get % "op")) patch)
         p patch]
    (if (or (empty? adds) (empty? removes))
      p
      (let [f-add (first adds)
            f-path (get "path" f-add)
            f-val (get "value" f-add)
            ;p-no-fval (filter #(let  (get-patch-value obj1 f-path)))
            ]
        
        (loop [])))))

(defn diff
  "Prepares a JSON patch document representing the difference
   between two JSON objects."
  ([obj1 obj2] (diff obj1 obj2 "/"))
  ([obj1 obj2 prefix]
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
                                     (not= v1 v2)
                                     (= (type v1) (type v2)))
                                (gen-op ["replace" (str prefix k) v2]))))
                      (for [[k v2] obj2]
                        (let [v1 (get obj1 k)]
                          (cond (not (contains? obj1 k))
                                (gen-op ["add" (str prefix k) v2])))))))))))

(defn patch
  "Applies a JSON patch document to JSON object"
  [obj patch])
