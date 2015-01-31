(ns clj-json-patch.core
  (:use [clj-json-patch.util :only [apply-patch diff*]]))


(defn patch
  "Applies a JSON patch document (multiple patches) to JSON object."
  [obj patches]
  (reduce #(apply-patch %1 %2) obj patches))

(defn diff
  "Prepares a JSON patch document representing the difference
  between two JSON objects."
  [obj1 obj2] (diff* obj1 obj2 "/"))
