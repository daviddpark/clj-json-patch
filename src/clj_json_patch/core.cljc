(ns clj-json-patch.core
  (:use [clj-json-patch.util :only [apply-patch diff* *keywordize*]]))


(defn call-apply-patch
  [obj patches]
  (reduce #(apply-patch %1 %2)
          #?(:clj  obj patches
             :cljs (js->clj (.parse js/JSON obj))
                   (js->clj (.parse js/JSON patches)))))


(defn patch
  "Applies a JSON patch document (multiple patches) to JSON object."
  ([obj patches]
   (call-apply-patch obj patches))
  ([obj patches keywordize?]
   (binding [*keywordize* keywordize?]
     (call-apply-patch obj patches))))

(defn call-diff*
  [obj1 obj2 prefix]
  #?(:clj   (diff* obj1
                   obj2
                   "/")
     :cljs  (diff* (js->clj (.parse js/JSON obj1))
                   (js->clj (.parse js/JSON obj2))
                   "/")))

(defn diff
  "Prepares a JSON patch document representing the difference
  between two JSON objects."
  ([obj1 obj2]
   (call-diff* obj1 obj2 "/"))
  ([obj1 obj2 keywordize?]
   (binding [*keywordize* keywordize?]
    (call-diff* obj1 obj2 "/"))))
