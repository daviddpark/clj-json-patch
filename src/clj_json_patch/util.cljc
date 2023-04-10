(ns clj-json-patch.util
  (:require #?(:clj  [cheshire.core :as json])
            [clojure.string :as c-str]))


(declare remove-patch-value)
(declare transform-moves)
(declare diff-vecs)
(declare gen-op)

(def ^:dynamic *keywordize* false)

(defn diff*
  "Prepares a JSON patch document representing the difference
   between two JSON objects."
  [obj1 obj2 prefix]
  (transform-moves obj1 obj2
                   (cond (and (vector? obj1) (vector? obj2))
                         (let [result (diff-vecs obj1 obj2 prefix)]
                           (if (vector? (first result))
                             (first result)
                             result))
                         (and (map? obj1) (map? obj2))
                         (vec
                           (flatten
                             (remove nil?
                                     (concat
                                       (for [[k v1] obj1]
                                         (let [k* (name k)
                                               v2 (get obj2 k)]
                                           (cond (and (vector? v1) (vector? v2))
                                                 (diff* v1 v2 (str prefix k* "/"))
                                                 (not (contains? obj2 k))
                                                 (gen-op ["remove" (str prefix k*)])
                                                 (and (map? v1) (map? v2))
                                                 (diff* v1 v2 (str prefix k* "/"))
                                                 (and (contains? obj2 k)
                                                      (not= v1 v2))
                                                 (gen-op ["replace" (str prefix k*) v2]))))
                                       (for [[k v2] obj2]
                                         (let [k* (name k)
                                               v1 (get obj1 k)]
                                           (cond (not (contains? obj1 k))
                                                 (gen-op ["add" (str prefix k*) v2])))))))))))

(defn eval-escape-characters
  [segment]
  (c-str/replace segment #"(~0|~1)"
                          (fn [[_ s]] (cond (= s "~0") "~"
                                            (= s "~1") "/"))))

; (defn inject-escape-characters
;   [segment]
;   (c-str/replace segment #"(~|\/)"
;                           (fn [[_ s]] (cond (= s "~") "~0"
;                                             (= s "/") "~1"))))

(defn ->key [seg]
  (if *keywordize*
    (keyword seg)
    seg))

(defn has-path?
  "Given the patch path, determines if the path exists in the obj"
  [obj path]
  (let [path (if (c-str/starts-with? path "#") (subs path 1) path)]
    (cond
      (and obj (or (= path "") (= path "#")))
      true
      (and (= path "/") (map? obj) (contains? obj ""))
      true
      :else
      (if-let [match (re-find #"^/([^/]+)(.*)" path)]
        (let [seg (eval-escape-characters (second match))
              segs (nth match 2)
              [h-path? val] (let [ky (if (vector? obj)
                                       #?(:clj (Integer/parseInt seg)
                                          :cljs (js/parseInt seg))
                                         (->key seg))]
                              [(contains? obj ky)
                               (get obj ky)])]
          ;;(println :HAS "seg:" seg "segs:" segs "val:" val "h-path?" h-path?)
          (if (and h-path? (not (empty? segs)))
            (has-path? val segs)
            h-path?))))))

(defn get-patch-value
  "Given the patch path, find the associated value."
  [obj path]
  (let [path (if (c-str/starts-with? path "#") (subs path 1) path)]
    (cond
      (or (= path "") (= path "#"))
      obj
      (and (= path "/") (map? obj) (get obj ""))
      (get obj "")
      :else
      (if-let [match (re-find #"^/([^/]+)(.*)" path)]
        (let [seg (eval-escape-characters (second match))
              segs (nth match 2)
              val (cond (map? obj)
                        (get obj (->key seg))
                        (vector? obj)
                        (nth obj #?(:clj  (Integer/parseInt seg)
                                    :cljs (js/parseInt seg))))]
                                        ;(println "seg:" seg "segs:" segs "val:" val)
          (if-not (empty? segs)
            (get-patch-value val segs)
            val))))))

(defn set-patch-value
  "Set val at path in obj"
  [obj path val & [insert]]
  (if-let [segs (re-seq #"/([^/]+)" path)]
    (if (> (count segs) 1)
      (if-let [path-exists (try (get-patch-value obj path)
                                #?(:clj  (catch Exception e
                                           (throw (Exception. (str "Unable to set value at '" path "'."))))
                                   :cljs (catch js/Object e
                                           (throw (js/Error. (str "Unable to set value at '" path "'."))))))]
        (let [parent-path (apply str (map first (take (dec (count segs)) segs)))
              parent (get-patch-value obj parent-path)]
          (set-patch-value obj parent-path
                           (set-patch-value parent (first (last segs)) val)))
        #?(:clj (throw (Exception. (str "Unable to set value at '" path
                                    "'. Consider adding a more explicit data "
                                        "structure as a child of an existing object.")))
           :cljs (throw (js/Error. (str "Unable to set value at '" path
                                        "'. Consider adding a more explicit data "
                                        "structure as a child of an existing object.")))))
      (cond (map? obj)
            (assoc obj (->key (eval-escape-characters (second (first segs))))
                   val)
            (vector? obj)
            (let [idx #?(:clj  (Integer/parseInt (second (re-find #"/(\d+)" path)))
                         :cljs (js/parseInt (second (re-find #"/(\d+)" path))))]
              (try
                (vec (concat (subvec obj 0 idx)
                         [val]
                         (subvec obj (if insert idx (inc idx)))))
                #?(:clj (catch Exception e
                          (throw (Exception. (str "Unable to set value at " idx "."))))
                   :cljs (catch js/Object e
                           (throw (js/Error. (str "Unable to set value at " idx ".")))))))))
    #?(:clj  (throw (Exception. "Patch path must start with '/'"))
       :cljs (throw (js/Error. "Patch path must start with '/'"))))) ;Should be throwing an exception

(defn add-patch-value
  "Add val at path in obj"
  [obj path val]
  (if-let [segs (re-seq #"/([^/]+)" path)]
    (if (> (count segs) 1)
      (let [parent-path (apply str (map first (take (dec (count segs)) segs)))
            parent (get-patch-value obj parent-path)]
        (if (vector? parent)
          (let [str-idx (last (last segs))]
            (if (or (= "-" str-idx)
                    (= (count parent) #?(:clj (try (Integer/parseInt str-idx)
                                               (catch java.lang.NumberFormatException e
                                                     (throw (Exception. (str "Unable to determine array index from '" str-idx "'.")))))
                                         :cljs (if (js/isNaN (js/parseInt str-idx))
                                                 (throw (js/Error. (str "Unable to determine array index from '" str-idx "'.")))
                                                 (js/parseInt str-idx)))))
              (set-patch-value obj parent-path
                               (conj parent val))
              (let [first-last-seg (first (last segs))
                    insert (.endsWith path str-idx)]
                (set-patch-value obj parent-path
                                 (set-patch-value parent (first (last segs)) val insert)))))
          (if-let [path-exists (try (get-patch-value obj parent-path)
                                    #?(:clj (catch Exception e
                                              (throw (Exception. (str "Unable to set value at '" path "'."))))
                                       :cljs (catch js/Object e
                                               (throw (js/Error. (str "Unable to set value at '" path "'."))))))]
            (set-patch-value obj parent-path
                             (set-patch-value parent (first (last segs)) val))
            #?(:clj (throw (Exception. (str "Unable to set value at '" path
                                        "'. Consider adding a more explicit data "
                                            "structure as a child of an existing object.")))
               :cljs (throw (js/Error. (str "Unable to set value at '" path
                                            "'. Consider adding a more explicit data "
                                            "structure as a child of an existing object.")))))))
      (set-patch-value obj path val))
    #?(:clj (throw (Exception. "Patch path must start with '/'"))
       :cljs (throw (js/Error. "Patch path must start with '/'")))))

(defn move-patch-value
  "Move value located at 'from' to the 'path'."
  [obj from path]
  (if-let [to-segs (re-seq #"/([^/]+)" path)]
    (if-let [from-segs (re-seq #"/([^/]+)" from)]
      (let [val (get-patch-value obj from)]
        (if (some? val)
          (if (> (count to-segs) 1)
            (let [from-parent-path (apply str (map first (take (dec (count from-segs)) from-segs)))
                  to-parent-path (apply str (map first (take (dec (count to-segs)) to-segs)))
                  parent (get-patch-value obj to-parent-path)]
              (if (= from-parent-path to-parent-path)
                (set-patch-value obj from-parent-path
                                 (move-patch-value (get-patch-value obj from-parent-path)
                                                   (first (last from-segs))
                                                   (first (last to-segs))))
                (set-patch-value (remove-patch-value obj from)
                                 to-parent-path
                                 (set-patch-value parent (first (last to-segs)) val))))
            (cond (map? obj)
                  (-> obj
                      (assoc (->key (second (first to-segs)))  val)
                      (dissoc (->key (second (first from-segs)))))
                  (vector? obj)
                  (let [from-int #?(:clj (try (Integer/parseInt (second (re-find #"/(\d+)" from)))
                                          (catch Exception e
                                               (throw (Exception. (str "Move attempted on value that does not exist at '" from "'.")))))
                                    :cljs (try (js/parseInt (second (re-find #"/(\d+)" from)))
                                            (catch js/Object e
                                              (throw (js/Error. (str "Move attempted on value that does not exist at '" from "'."))))))
                          to-int #?(:clj (try (Integer/parseInt (second (re-find #"/(\d+)" path)))
                                          (catch Exception e
                                                   (throw (Exception. (str "Move attempted on value that does not exist at '" path "'.")))))
                                    :cljs (try (js/parseInt (second (re-find #"/(\d+)" path)))
                                            (catch js/Object e
                                              (throw (js/Error. (str "Move attempted on value that does not exist at '" path "'."))))))]
                      (vec (concat (subvec obj 0 from-int) (subvec obj (inc from-int) (inc to-int))
                                   [(get obj from-int)] (subvec obj (inc to-int)))))))
          #?(:clj (throw (Exception. (str "Move attempted on value that does not exist at '" from "'.")))
             :cljs (throw (js/Error. (str "Move attempted on value that does not exist at '" from "'."))))))
      #?(:clj (throw (Exception. "Patch 'from' value must start with '/'"))
         :cljs (throw (js/Error. "Patch 'from' value must start with '/'"))))
    #?(:clj (throw (Exception. "Patch 'path' value must start with '/'"))
       :cljs (throw (js/Error. "Patch 'path' value must start with '/'")))))

(defn replace-patch-value
  "Replace the value found at 'path' with that bound to 'val'."
  [obj path val]
  ;;(println "path" obj path val (has-path? obj path))
  (if (has-path? obj path)
    (if-let [segs (re-seq #"/([^/]+)" path)]
      (if (> (count segs) 1)
        (let [parent-path (apply str (map first (take (dec (count segs)) segs)))
              parent (get-patch-value obj parent-path)]
          (replace-patch-value obj parent-path
                               (replace-patch-value parent (first (last segs)) val)))
        (cond (map? obj)
              (assoc obj (->key (second (first segs))) val)
              (vector? obj)
              (let [idx #?(:clj (Integer/parseInt (second (re-find #"/(\d+)" path)))
                           :cljs (js/parseInt (second (re-find #"/(\d+)" path))))]
                (vec (concat (subvec obj 0 idx)
                             [val]
                             (subvec obj (inc idx)))))))
      #?(:clj (throw (Exception. "Patch path must start with '/'"))
         :cljs (throw (js/Error. "Patch path must start with '/'"))))
    #?(:clj (throw (Exception. (str "Can't replace a value that does not exist at '" path "'.")))
       :cljs (throw (js/Error. (str "Can't replace a value that does not exist at '" path "'."))))))


(defn remove-patch-value-func
  "Remove the value at 'path' from obj."
  [obj path]
  (if (has-path? obj path)
    (when-let [segs (re-seq #"/([^/]+)" path)]
      (if (> (count segs) 1)
        (let [parent-path (apply str (map first (take (dec (count segs)) segs)))
              parent      (get-patch-value obj parent-path)]
          (replace-patch-value obj parent-path
                           (remove-patch-value parent (first (last segs)))))
        (cond (map? obj)
              (dissoc obj (second (first segs)))
              (vector? obj)
              (let [idx #?(:clj (Integer/parseInt (second (re-find #"/(\d+)" path)))
                           :cljs (js/parseInt (second (re-find #"/(\d+)" path))))]
                  (vec (concat (subvec obj 0 idx) (subvec obj (inc idx))))))))
    #?(:clj (throw (Exception. (str "There is no value at '" path "' to remove.")))
       :cljs (throw (js/Error. (str "There is no value at '" path "' to remove."))))))


(defn remove-patch-value
  "Remove the value at 'path' from obj."
  [obj path]
  (try (remove-patch-value-func obj path)
    #?(:clj  (catch Exception e
               (throw (Exception. (str "There is no value at '" path "' to remove."))))
       :cljs (catch js/Object e
               (throw (js/Error. (str "There is no value at '" path "' to remove.")))))))

(defn test-patch-value
  "Ensure that the value located at 'path' in obj is equal to 'val'."
  [obj path val]
  (try
    (let [value (get-patch-value obj path)]
      (if (not= val value)
        #?(:clj (throw (Exception. (str "The value is: " (cheshire.core/generate-string value))))
           :cljs (throw (js/Error. (str "The value is: " (.stringify js/JSON (clj->js value))))))
        obj))
    #?(:clj (catch Exception e
             (throw (Exception.
                     (str "The test failed. "
                          (cheshire.core/generate-string val)
                          " is not found at " path ". "
                          (.getMessage e)))))
       :cljs (catch js/Object e
               (throw (js/Error.
                        (str "The test failed. "
                           (.stringify js/JSON (clj->js val))
                           " is not found at " path ". "
                           e)))))))


(defn apply-patch [obj patch]
  "Apply the patch operation in patch to obj, returning the new obj representation."
  (let [op (or (get patch "op")
               (get patch :op))
        path (or (get patch "path")
                 (get patch :path))
        from (or (get patch "from")
                 (get patch :from))
        value (or (get patch "value")
                  (get patch :value))]
    (cond (= "add" op)
          (add-patch-value obj path value)
          (= "move" op)
          (move-patch-value obj from path)
          (= "remove" op)
          (remove-patch-value obj path)
          (= "replace" op)
          (replace-patch-value obj path value)
          (= "test" op)
          (test-patch-value obj path value))))

(defn gen-op [t]
  [(let [result {"op" (first t) "path" (second t)}]
    (if (> (count t) 2)
      (assoc result "value" (nth t 2))
      result))])

(defn clean-prefix
  [prefix path]
  (c-str/replace path (re-pattern prefix) "/"))

(defn sanitize
  [prefix patch]
  (let [path (get patch "path")
        cleaned-path (assoc patch "path" (clean-prefix prefix path))]
    (if-let [from (get patch "from")]
      (assoc cleaned-path "from" (clean-prefix prefix from))
      cleaned-path)))

(defn sanitize-prefix-in-patch
  [prefix idx patch]
  (if (vector? patch)
    (map (partial sanitize (str prefix idx)) patch)
    (sanitize prefix patch)))


(defn diff-vecs [obj1 obj2 prefix]
  (loop [v1 obj1
         v2 obj2
         i 0
         ops []]
    (cond (and (empty? v1) (empty? v2))
          (if (vector? (first ops))
            (vec (apply concat ops))
            ops)
          (= v1 (rest v2))
          (conj ops (gen-op ["add" (str prefix i) (first v2)]))
          (= (rest v1) v2)
          (conj ops (gen-op ["remove" (str prefix i)]))
          (not= (first v1) (first v2))
          (if (and (map? (first v1)) (map? (first v2)))
            (recur (rest v1) (rest v2) (inc i)
                   (conj ops (diff* (first v1) (first v2) (str prefix i "/"))))
            (recur (rest v1) (rest v2) (inc i)
                   (conj ops (gen-op ["replace" (str prefix i) (first v2)]))))
          (and (> (count ops) 0)
               (= v2
                  (reduce
                   #(apply-patch %1 %2) v1
                   (map (partial sanitize-prefix-in-patch prefix (dec i)) ops))))
          ops
          (= (set v1) (set v2))
          (cond (= i (count v1))
                ops
                (= (get v1 i) (get v2 i))
                (recur v1 v2 (inc i) ops)
                (not= (get v1 i) (get v2 i))
                (let [moved-idx (first (filter (complement nil?) (map-indexed #(if (= (get v1 i) %2) %1) v2)))]
                  (recur v1 v2 (inc i)
                         (conj ops {"op" "move" "from" (str prefix i) "path" (str prefix moved-idx)}))))
          (and (= (first v1) (first v2))
               (not= (rest v1) (rest v2)))
          (recur (rest v1) (rest v2) (inc i) ops))))

; (defn get-value-path
;   "Traverses obj looking for a value that matches val, returns path to value."
;   ([obj val] (get-value-path obj val "/"))
;   ([obj val prefix]
;    (cond (map? obj)
;          (some identity
;                (concat
;                 (for [[k v] obj]
;                   (if (= v val)
;                     (str prefix (inject-escape-characters k))
;                     (if-not (string? v)
;                       (get-value-path v val (str prefix (inject-escape-characters k) "/")))))))
;          (vector? obj)
;          (if-let [idx (some identity (map-indexed #(if (= val %2) %1) obj))]
;            (str prefix idx)
;            (map-indexed #(get-value-path %2 val (str prefix %1 "/")) obj)))))

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
            f-path (get f-add "path")
            f-val (get f-add "value")
            moved (filter #(= f-val (get-patch-value obj1 (get % "path"))) removes)]
        (if-let [fmoved (first moved)]
          (recur (rest adds) (filter #(not= fmoved %) removes)
                 (conj (filter #(not= f-add %) (filter #(not= fmoved %) patch))
                       {"op" "move" "from" (get fmoved "path") "path" f-path}))
          (recur (rest adds) removes p))))))
