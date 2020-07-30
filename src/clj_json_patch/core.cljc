(ns clj-json-patch.core
  (:use [clj-json-patch.util :only [apply-patch diff* *keywordize*]]))
(def log #?(:cljs (.-log js/console)))

(defn call-apply-patch
  [obj patches]
  #?(:clj  (reduce #(apply-patch %1 %2) obj patches)
     :cljs (reduce #(apply-patch %1 %2) (js->clj (.parse js/JSON obj))
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
  #?(:clj  (diff* obj1 obj2 prefix)
     :cljs (diff* (js->clj (.parse js/JSON obj1))
                  (js->clj (.parse js/JSON obj2))
                  prefix)))

(defn diff
  "Prepares a JSON patch document representing the difference
  between two JSON objects."
  ([obj1 obj2]
   (call-diff* obj1 obj2 "/"))
  ([obj1 obj2 keywordize?]
   (binding [*keywordize* keywordize?])
   (call-diff* obj1 obj2 "/")))

