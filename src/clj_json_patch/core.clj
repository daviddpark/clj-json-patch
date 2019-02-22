(ns clj-json-patch.core
  (:use [clj-json-patch.util :only [apply-patch diff* *keywordize*]]))


(defn patch
  "Applies a JSON patch document (multiple patches) to JSON object."
  ([obj patches]
   (reduce #(apply-patch %1 %2) obj patches))
  ([obj patches keywordize?]
   (binding [*keywordize* keywordize?]
     (reduce #(apply-patch %1 %2) obj patches))))

(defn diff
  "Prepares a JSON patch document representing the difference
  between two JSON objects."
  ([obj1 obj2]
   (diff* obj1 obj2 "/"))
  ([obj1 obj2 keywordize?]
   (binding [*keywordize* keywordize?]
     (diff* obj1 obj2 "/"))))
