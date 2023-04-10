(ns clj-json-patch.core-test-cljs
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [clj-json-patch.core :refer [diff patch]]))

;; Apply patch
(deftest apply-patch
  (let [v (.stringify js/JSON (clj->js ["all" "grass" "cows" "eat" "slowly"]))
        p (.stringify js/JSON (clj->js [{"from" "/1" "op" "move" "path" "/3"}]))
        expected ["all" "cows" "eat" "grass" "slowly"]]
    (is (= (patch v p) expected))))

;; JSON diff
(deftest d-no-diff
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
             obj2 (.stringify js/JSON (clj->js {"foo" "bar"}))
             patches []]
         (is (= (diff obj1 obj2) patches))))

(deftest d-add-member
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
             obj2 (.stringify js/JSON (clj->js {"baz" "qux" "foo" "bar"}))
             patches [{"op" "add" "path" "/baz" "value" "qux"}]]
          (is (= (diff obj1 obj2) patches))))

(deftest d-add-array
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
               obj2 (.stringify js/JSON (clj->js {"foo" ["qux" "bar" "baz"]}))
               patches [{"op" "add" "path" "/foo/0" "value" "qux"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-add-second-array
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
               obj2 (.stringify js/JSON (clj->js {"foo" ["bar" "qux" "baz"]}))
               patches [{"op" "add" "path" "/foo/1" "value" "qux"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-remove-member
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar" "baz" "qux"}))
               obj2 (.stringify js/JSON (clj->js {"foo" "bar"}))
               patches [{"op" "remove" "path" "/baz"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-remove-first-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["qux" "bar" "baz"]}))
               obj2 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
               patches [{"op" "remove" "path" "/foo/0"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-remove-second-element
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "qux" "baz"]}))
             obj2 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
             patches [{"op" "remove" "path" "/foo/1"}]]
          (is (= (diff obj1 obj2) patches))))

(deftest d-remove-third-element
           (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz" "qux"]}))
                 obj2 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
                 patches [{"op" "remove" "path" "/foo/2"}]]
             (is (= (diff obj1 obj2) patches))))

(deftest d-replace-value
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar" "baz" "qux"}))
               obj2 (.stringify js/JSON (clj->js {"baz" "boo" "foo" "bar"}))
               patches [{"op" "replace" "path" "/baz" "value" "boo"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-remove-nested-member
         (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" "kill me"
                                                             "baz" "boo"}}))
               obj2 (.stringify js/JSON (clj->js {"foo" {"baz" "boo"}}))
               patches [{"op" "remove" "path" "/foo/bar"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-add-nested-member
         (let [obj1 (.stringify js/JSON (clj->js {"foo" {"baz" "boo"}}))
               obj2 (.stringify js/JSON (clj->js {"child" {"grandchild" {}},
                                                   "foo" {"baz" "boo"}}))
               patches [{"op" "add" "path" "/child" "value" {"grandchild" {}}}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-move-value
         (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" "baz"
                                                             "waldo" "fred"},
                                                   "qux" {"corge" "grault"}}))
               obj2 (.stringify js/JSON (clj->js {"foo" {"bar" "baz"},
                                                   "qux" {"corge" "grault"
                                                             "thud" "fred"}}))
               patches [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-move-array-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
               obj2 (.stringify js/JSON (clj->js {"foo" ["all" "cows" "eat" "grass"]}))
               patches [{"op" "move" "from" "/foo/1" "path" "/foo/3"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-replace-value-with-array
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
               obj2 (.stringify js/JSON (clj->js {"foo" ["all" "cows" "eat" "grass"]}))
               patches [{"op" "replace" "path" "/foo"
                         "value" ["all" "cows" "eat" "grass"]}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-nil-vs-absent
         (let [obj1 (.stringify js/JSON (clj->js {"key1" null,
                                                   "key2" "val2"}))
               obj2 (.stringify js/JSON (clj->js {"key2" "val2"}))
               patches [{"op" "remove" "path" "/key1"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-nested-nil-vs-absent
         (let [obj1 (.stringify js/JSON (clj->js {"test" {"key1" null,
                                                              "key2" "val2"}}))
               obj2 (.stringify js/JSON (clj->js {"test" {"key2" "val2"}}))
               patches [{"op" "remove" "path" "/test/key1"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-replace-value-in-vector
         (let [obj1 (.stringify js/JSON (clj->js {"test" [{"key1" "val1"}]}))
               obj2 (.stringify js/JSON (clj->js {"test" [{"key1" "val1replaced"}]}))
               patches [{"op" "replace" "path" "/test/0/key1" "value" "val1replaced"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-nil-vs-absent-in-vector
         (let [obj1 (.stringify js/JSON (clj->js {"test" [{"key1" null,
                                                               "key2" "val2"}]}))
               obj2 (.stringify js/JSON (clj->js {"test" [{"key2" "val2"}]}))
               patches [{"op" "remove" "path" "/test/0/key1"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-vector-compare
         (let [obj1 (.stringify js/JSON (clj->js [{"key1" null,
                                                    "key2" "val2"}]))
               obj2 (.stringify js/JSON (clj->js [{"key2" "val2"}]))
               patches [{"op" "remove" "path" "/0/key1"}]]
           (is (= (diff obj1 obj2) patches))))

(deftest d-vector-different-maps
         (let [obj1 (.stringify js/JSON (clj->js {"baz" [{"first" "test"},
                                                             {"second" "test"},
                                                             {"third" "test"}]}))
               obj2 (.stringify js/JSON (clj->js {"baz" [{"first" "test"},
                                                             {"second" "second"},
                                                             {"third" "level"}]}))
               patches [{"op" "replace" "path" "/baz/1/second" "value" "second"}
                        {"op" "replace" "path" "/baz/2/third" "value" "level"}]]
           (is (= (diff obj1 obj2) patches))))

;; Happy path JSON patch
(deftest p-no-change
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
               patches (.stringify js/JSON (clj->js []))
               result {"foo" "bar"}]
           (is (= (patch obj1 patches) result))))

(deftest p-add-member
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
               obj2 {"baz" "qux" "foo" "bar"}
               patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/baz" "value" "qux"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-add-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
               obj2 {"foo" ["qux" "bar" "baz"]}
               patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/0" "value" "qux"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-add-second-array-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
               obj2 {"foo" ["bar" "qux" "baz"]}
               patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/1" "value" "qux"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-remove-member
         (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar" "baz" "qux"}))
               obj2 {"foo" "bar"}
               patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/baz"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-remove-array-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["qux" "bar" "baz"]}))
               obj2 {"foo" ["bar" "baz"]}
               patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/0"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-remove-second-array-element
         (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "qux" "baz"]}))
               obj2 {"foo" ["bar" "baz"]}
               patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/1"}]))]
           (is (= (patch obj1 patches) obj2))))

(deftest p-remove-third-array-element
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz" "qux"]}))
             obj2 {"foo" ["bar" "baz"]}
             patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/2"}]))]
         (is (= (patch obj1 patches) obj2))))

(deftest p-replace-value
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar" "baz" "qux"}))
             obj2 {"baz" "boo" "foo" "bar"}
             patches (.stringify js/JSON (clj->js [{"op" "replace" "path" "/baz" "value" "boo"}]))]
         (is (= (patch obj1 patches) obj2))))

(deftest p-replace-boolean
      (let [obj1 (.stringify js/JSON (clj->js {"foo" false, "baz" "qux"}))
            obj2 {"baz" "qux" "foo" true}
            patches (.stringify js/JSON (clj->js [{"op" "replace" "path" "/foo" "value" true}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-remove-nested-member
      (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" "kill me"
                                                          "baz" "boo"}}))
            obj2 {"foo" {"baz" "boo"}}
            patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/bar"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-add-nested-member
      (let [obj1 (.stringify js/JSON (clj->js {"foo" {"baz" "boo"}}))
            obj2 {"child" {"grandchild" {}}
                  "foo" {"baz" "boo"}}
            patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/child" "value" {"grandchild" {}}}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-add-nested-arry
      (let [obj1 (.stringify js/JSON (clj->js {"foo" ["baz" "boo"]}))
            obj2 {"foo" ["baz" "boo" "added"]}
            patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/2" "value" "added"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-add-to-end-nested-array
      (let [obj1 (.stringify js/JSON (clj->js {"foo" ["baz" "boo"]}))
            obj2 {"foo" ["baz" "boo" "added"]}
            patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/-" "value" "added"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-ignore-unregonized-elements
      (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
            obj2 {"foo" "bar" "baz" "qux"}
            patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/baz" "value" "qux" "xyz" 123}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-add-nonexistent-target
      (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
            patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/baz/bat" "value" "qux"}]))]
        (is (thrown-with-msg? js/Error #"Unable to set value at '/baz/bat'. Consider adding a more explicit data structure as a child of an existing object." (patch obj1 patches)))))

(deftest p-move-value
      (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" "baz"
                                                          "waldo" "fred"},
                                                "qux" {"corge" "grault"}}))
            obj2 {"foo" {"bar" "baz"}
                  "qux" {"corge" "grault"
                         "thud" "fred"}}
            patches (.stringify js/JSON (clj->js [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-move-array-element
      (let [obj1 (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
            obj2 {"foo" ["all" "cows" "eat" "grass"]}
            patches (.stringify js/JSON (clj->js [{"op" "move" "from" "/foo/1" "path" "/foo/3"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-test-object
      (let [obj (.stringify js/JSON (clj->js ["a" 2, "c"]))
            patches (.stringify js/JSON (clj->js [{"op" "test" "path" "/1" "value" 2}]))
            result ["a" 2 "c"]]
        (is (= (patch obj patches) result))))

(deftest p-test-multiple-objects
      (let [obj (.stringify js/JSON (clj->js {"baz" "qux" "foo" ["a" 2, "c"]}))
            patches (.stringify js/JSON (clj->js [{"op" "test" "path" "/baz" "value" "qux"},
                                                   {"op" "test" "path" "/foo/1" "value" 2}]))
            result {"baz" "qux" "foo" [ "a" 2 "c"]}]
        (is (= (patch obj patches) result))))

(deftest p-expected-patched
      (let [obj1 (.stringify js/JSON (clj->js {"k1" [{"s0k1" "s0v1"}, {"s1k1" "s1v1"}]}))
            obj2 {"k1" [{"s0k1" "new value"} {"s1k1" "s1v1"}]}
            patches (.stringify js/JSON (clj->js [{"op" "replace" "path" "/k1/0/s0k1" "value" "new value"}]))]
        (is (= (patch obj1 patches) obj2))))

(deftest p-path-with-escape
  (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" 42}}))
        obj2 {"foo" {"bar" 42 "baz/bar" "ohyeah"}}
        patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/baz~1bar" "value" "ohyeah"}]))]
    (is (= (patch obj1 patches) obj2))))

;         (fact "first nested object updated correctly"
;               (get (first (get patched "k1")) "s0k1") => "new value")
;         (fact "second nested object unchanged"
;               (get (second (get patched "k1")) "s1k1") => "s1v1")


; "Nested JSON patch"
(deftest p-patch-nested-map
       (let [obj1 (.stringify js/JSON (clj->js {"nested" {"foo" "bar"}}))
             obj2 {"nested" {"foo" "bar"
                             "zot" "baz"}}
             patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/nested/zot" "value" "baz"}]))]
         (is (= (patch obj1 patches) obj2))))

; "Remove from object within array"
(deftest p-remove-nested-vector-item
       (let [obj1    (.stringify js/JSON (clj->js [{"name" "item1" "foo" "bar"},
                                                    {"name" "item2" "foo" "bar"}]))
             obj2    [{"name" "item1"}
                      {"name" "item2" "foo" "bar"}]
             patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/0/foo"}]))]
         (is (= (patch obj1 patches) obj2))))
;
; "Add to object within array"
(deftest p-add-item-to-map-in-vector
       (let [obj (.stringify js/JSON (clj->js {"array" [{"id" "hello"}]}))
             expected {"array" [{"id" "hello" "foo" "bar"}]}
             patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/array/0/foo" "value" "bar"}]))]
         (is (= (patch obj patches) expected))))


;
; "Patch error conditions"
(deftest p-test-for-nonexistent-object
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
             patches (.stringify js/JSON (clj->js [{"op" "test" "path" "/baz" "value" "qux"}]))]
        (is (thrown-with-msg? js/Error #"The test failed." (patch obj1 patches)))))

(deftest p-test-for-nonexistent-object2
       (let [obj1 (.stringify js/JSON (clj->js ["foo" "bar"]))
             patches (.stringify js/JSON (clj->js [{"op" "test" "path" "/4" "value" "qux"}]))]
        (is (thrown-with-msg? js/Error #"The test failed." (patch obj1 patches)))))

(deftest p-remove-nonexisting-object
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar"}))
             patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/baz" "value" "qux"}]))]
         (is (thrown-with-msg? js/Error #"There is no value at '/baz' to remove." (patch obj1 patches)))))

(deftest p-remove-nonexisting-array-element
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
             patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/6" "value" "qux"}]))]
         (is (thrown-with-msg? js/Error #"There is no value at '/foo/6' to remove." (patch obj1 patches)))))

(deftest p-add-value-wrong-index
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["bar" "baz"]}))
             patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/3" "value" "qux"}]))]
         (is (thrown-with-msg? js/Error #"Unable to set value at 3." (patch obj1 patches)))))

(deftest p-replace-nonexsisting-value
       (let [obj1 (.stringify js/JSON (clj->js {"foo" "bar" "boo" "qux"}))
             patches (.stringify js/JSON (clj->js [{"op" "replace" "path" "/baz" "value" "boo"}]))]
         (is (thrown-with-msg? js/Error #"Can't replace a value that does not exist at '/baz'." (patch obj1 patches)))))

(deftest p-remove-nonexisting-nested-path
       (let [obj1 (.stringify js/JSON (clj->js {"foo" {"beer" "booze"
                                                       "baz" "boo"}}))
             patches (.stringify js/JSON (clj->js [{"op" "remove" "path" "/foo/bar"}]))]
         (is (thrown-with-msg? js/Error #"There is no value at '/foo/bar' to remove." (patch obj1 patches)))))

(deftest p-move-nonexisting-value
       (let [obj1 (.stringify js/JSON (clj->js {"foo" {"bar" "baz"
                                                       "waldorf" "fred"}
                                                "qux" {"corge" "grault"}}))
             patches (.stringify js/JSON (clj->js [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]))]
         (is (thrown-with-msg? js/Error #"Move attempted on value that does not exist at '/foo/waldo'." (patch obj1 patches)))))

(deftest p-illegal-move-of-array
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
             patches (.stringify js/JSON (clj->js [{"op" "move" "from" "/foo/1" "path" "/foo/-1"}]))]
         (is (thrown? js/Error (patch obj1 patches)))))

(deftest p-add-array-to-non-number-index
       (let [obj1 (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
             patches (.stringify js/JSON (clj->js [{"op" "add" "path" "/foo/two"}]))]
         (is (thrown-with-msg? js/Error #"Unable to determine array index from 'two'." (patch obj1 patches)))))
