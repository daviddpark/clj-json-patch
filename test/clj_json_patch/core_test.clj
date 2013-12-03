(ns clj-json-patch.core-test
  (:require [clojure.test :refer :all]
            [clj-json-patch.core :refer :all])
  (:use [midje.sweet]))

(let [obj1 {"foo" "bar"}
             obj2 {"foo" {"bar" "baz"}}
             obj3 {"foo" {"bar" {"baz" "deep!"}}}
             obj4 ["foo" "bar"]
             obj5 {"foo" ["bar" "baz" "last"]}
             obj6 {"foo" {"bar" ["baz" "deeper!"]}}]
  (facts "get-patch-value"
         (fact "get value from simple map"
               (get-patch-value obj1 "/foo") => "bar")
         (fact "get value from nested map"
               (get-patch-value obj2 "/foo/bar") => "baz")
         (fact "get value from nested maps"
               (get-patch-value obj3 "/foo/bar/baz") => "deep!")
         (fact "get value from simple array"
               (get-patch-value obj4 "/0") => "foo")
         (fact "get value from simple map of array"
               (get-patch-value obj5 "/foo/1") => "baz")
         (fact "get value from map of map with array"
               (get-patch-value obj6 "/foo/bar/1") => "deeper!"))
  (facts "get-value-path"
         (fact "get path from simple map"
               (get-value-path obj1 "bar") => "/foo")
         (fact "get path from nested map"
               (get-value-path obj2 "baz") => "/foo/bar")
         (fact "get path from nested maps"
               (get-value-path obj3 "deep!") => "/foo/bar/baz")
         (fact "get path from simple array"
               (get-value-path obj4 "foo") => "/0")
         (fact "get path from simple map of array"
               (get-value-path obj5 "baz") => "/foo/1")
         (fact "get path from map of map with array"
               (get-value-path obj6 "deeper!") => "/foo/bar/1")))

(facts "diff-vecs"
       (let [v1 ["all" "grass" "cows" "eat"]
             v2 ["all" "cows" "eat" "grass"]]
         (fact "two vectors with same elements in different order are not the same"
               (count (diff-vecs v1 v2 "/")) => 1
               (diff-vecs v1 v2 "/") => [{"from" "/1", "op" "move", "path" "/3"}])))

(facts "apply-patch"
       (let [v ["all" "grass" "cows" "eat" "slowly"]
             patch {"from" "/1", "op" "move", "path" "/3"}
             expected ["all" "cows" "eat" "grass" "slowly"]]
         (apply-patch v patch) => expected))

(facts "applying patch"
       (let [v ["all" "grass" "cows" "eat" "slowly"]
             p [{"from" "/1", "op" "move", "path" "/3"}]
             expected ["all" "cows" "eat" "grass" "slowly"]]
         (patch v p) => expected))

(facts "JSON diff"
       (let [obj1 {"foo" "bar"}
             obj2 {"foo" "bar"}
             patches []]
         (fact "No difference"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" "bar"}
             obj2 {"baz" "qux" "foo" "bar"}
             patches [{"op" "add" "path" "/baz" "value" "qux"}]]
         (fact "Adding an Object Member"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["qux" "bar" "baz"]}
             patches [{"op" "add" "path" "/foo/0" "value" "qux"}]]
         (fact "Adding an Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["bar" "qux" "baz"]}
             patches [{"op" "add" "path" "/foo/1" "value" "qux"}]]
         (fact "Adding a second Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"foo" "bar"}
             patches [{"op" "remove" "path" "/baz"}]]
         (fact "Removing an Object Member"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["qux" "bar" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/0"}]]
         (fact "Removing first Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["bar" "qux" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/1"}]]
         (fact "Removing subsequent Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["bar" "baz" "qux"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/2"}]]
         (fact "Removing subsequent Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"baz" "boo" "foo" "bar"}
             patches [{"op" "replace" "path" "/baz" "value" "boo"}]]
         (fact "Replacing a Value"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" {"bar" "kill me"
                          "baz" "boo"}}
             obj2 {"foo" {"baz" "boo"}}
             patches [{"op" "remove" "path" "/foo/bar"}]]
         (fact "Adding a Nested Object Member"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" {"bar" "baz"
                          "waldo" "fred"}
                   "qux" {"corge" "grault"}}
             obj2 {"foo" {"bar" "baz"}
                   "qux" {"corge" "grault"
                          "thud" "fred"}}
             patches [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]]
         (fact "Moving a Value"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" ["all" "grass" "cows" "eat"]}
             obj2 {"foo" ["all" "cows" "eat" "grass"]}
             patches [{"op" "move" "from" "/foo/1" "path" "/foo/3"}]]
         (fact "Moving an Array Element"
               (diff obj1 obj2) => patches)))

(facts "Happy path JSON patch"
       (let [obj1 {"foo" "bar"}
             patches []]
         (fact "No change"
               (patch obj1 patches) => obj1))
       (let [obj1 {"foo" "bar"}
             obj2 {"baz" "qux" "foo" "bar"}
             patches [{"op" "add" "path" "/baz" "value" "qux"}]]
         (fact "Adding an Object Member"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["qux" "bar" "baz"]}
             patches [{"op" "add" "path" "/foo/0" "value" "qux"}]]
         (fact "Adding an Array Element"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["bar" "qux" "baz"]}
             patches [{"op" "add" "path" "/foo/1" "value" "qux"}]]
         (fact "Adding a second Array Element"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"foo" "bar"}
             patches [{"op" "remove" "path" "/baz"}]]
         (fact "Removing an Object Member"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["qux" "bar" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/0"}]]
         (fact "Removing first Array Element"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["bar" "qux" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/1"}]]
         (fact "Removing subsequent Array Element"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["bar" "baz" "qux"]}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/2"}]]
         (fact "Removing subsequent Array Element"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"baz" "boo" "foo" "bar"}
             patches [{"op" "replace" "path" "/baz" "value" "boo"}]]
         (fact "Replacing a Value"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" {"bar" "kill me"
                          "baz" "boo"}}
             obj2 {"foo" {"baz" "boo"}}
             patches [{"op" "remove" "path" "/foo/bar"}]]
         (fact "Adding a Nested Object Member"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" {"bar" "baz"
                          "waldo" "fred"}
                   "qux" {"corge" "grault"}}
             obj2 {"foo" {"bar" "baz"}
                   "qux" {"corge" "grault"
                          "thud" "fred"}}
             patches [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]]
         (fact "Moving a Value"
               (patch obj1 patches) => obj2))
       (let [obj1 {"foo" ["all" "grass" "cows" "eat"]}
             obj2 {"foo" ["all" "cows" "eat" "grass"]}
             patches [{"op" "move" "from" "/foo/1" "path" "/foo/3"}]]
         (fact "Moving an Array Element"
               (patch obj1 patches) => obj2))
       (let [obj [ "a" 2 "c"]
             patches [{ "op" "test" "path" "/1" "value" 2}]]
         (fact "Successful test ops return object"
               (patch obj patches) => obj))
       (let [obj {"baz" "qux" "foo" [ "a" 2 "c"]}
             patches [{ "op" "test" "path" "/baz" "value" "qux"}
                      { "op" "test" "path" "/foo/1" "value" 2}]]
         (fact "Successful test ops return object"
               (patch obj patches) => obj)))

(facts "Patch error conditions"
       (let [obj1 {"foo" "bar"}
             patches [{"op" "test" "path" "/baz" "value" "qux"}]]
         (fact "Testing an Object Member"
               (patch obj1 patches) => (throws Exception "The test failed. 'qux' is not found at '/baz'.")))
       (let [obj1 ["foo" "bar"]
             patches [{"op" "test" "path" "/4" "value" "qux"}]]
         (fact "Testing an Array Member"
               (patch obj1 patches) => (throws Exception "The test failed. 'qux' is not found at '/4'.")))
       (let [obj1 {"foo" "bar"}
             patches [{"op" "remove" "path" "/baz" "value" "qux"}]]
         (fact "Removing an Object Member"
               (patch obj1 patches) => (throws Exception "There is no value at '/baz' to remove.")))       
       (let [obj1 {"foo" ["bar" "baz"]}
             patches [{"op" "remove" "path" "/foo/6" "value" "qux"}]]
         (fact "Removing an Array Element"
               (patch obj1 patches) => (throws Exception "There is no value at '/foo/6' to remove.")))
       (let [obj1 {"foo" ["bar" "baz"]}
             patches [{"op" "add" "path" "/foo/3" "value" "qux"}]]
         (fact "Adding a second Array Element"
               (patch obj1 patches) => (throws Exception "Unable to set value at 3")))
       (let [obj1 {"foo" "bar" "boo" "qux"}
             patches [{"op" "replace" "path" "/baz" "value" "boo"}]]
         (fact "Replacing a Value that doesn't exist"
               (patch obj1 patches) => (throws Exception "Can't replace a value that does not exist at '/baz'.")))
       (let [obj1 {"foo" {"beer" "kill me"
                          "baz" "boo"}}
             patches [{"op" "remove" "path" "/foo/bar"}]]
         (fact "Removing a nested path that does not exist"
               (patch obj1 patches) => (throws Exception "There is no value at '/foo/bar' to remove.")))
       (let [obj1 {"foo" {"bar" "baz"
                          "waldorf" "fred"}
                   "qux" {"corge" "grault"}}
             patches [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]]
         (fact "Moving a Value that does not exist"
               (patch obj1 patches) => (throws Exception "Move attempted on value that does not exist at '/foo/waldo'.")))
       (let [obj1 {"foo" ["all" "grass" "cows" "eat"]}
             patches [{"op" "move" "from" "/foo/1" "path" "/foo/-1"}]]
         (fact "Moving an Array Element to an illegal value"
               (patch obj1 patches) => (throws Exception "Move attempted on value that does not exist at '/-1'."))))
