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

(facts "JSON diff"
       (let [obj1 {"foo" "bar"}
             obj2 {"foo" "bar"}
             expected []]
         (fact "No difference"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" "bar"}
             obj2 {"baz" "qux" "foo" "bar"}
             expected [{"op" "add" "path" "/baz" "value" "qux"}]]
         (fact "Adding an Object Member"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["qux" "bar" "baz"]}
             expected [{"op" "add" "path" "/foo/0" "value" "qux"}]]
         (fact "Adding an Array Element"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["bar" "baz"]}
             obj2 {"foo" ["bar" "qux" "baz"]}
             expected [{"op" "add" "path" "/foo/1" "value" "qux"}]]
         (fact "Adding a second Array Element"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"foo" "bar"}
             expected [{"op" "remove" "path" "/baz"}]]
         (fact "Removing an Object Member"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["qux" "bar" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             expected [{"op" "remove" "path" "/foo/0"}]]
         (fact "Removing first Array Element"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["bar" "qux" "baz"]}
             obj2 {"foo" ["bar" "baz"]}
             expected [{"op" "remove" "path" "/foo/1"}]]
         (fact "Removing subsequent Array Element"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["bar" "baz" "qux"]}
             obj2 {"foo" ["bar" "baz"]}
             expected [{"op" "remove" "path" "/foo/2"}]]
         (fact "Removing subsequent Array Element"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" "bar" "baz" "qux"}
             obj2 {"baz" "boo" "foo" "bar"}
             expected [{"op" "replace" "path" "/baz" "value" "boo"}]]
         (fact "Replacing a Value"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" {"bar" "kill me"
                          "baz" "boo"}}
             obj2 {"foo" {"baz" "boo"}}
             expected [{"op" "remove" "path" "/foo/bar"}]]
         (fact "Adding a Nested Object Member"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" {"bar" "baz"
                          "waldo" "fred"}
                   "qux" {"corge" "grault"}}
             obj2 {"foo" {"bar" "baz"}
                   "qux" {"corge" "grault"
                          "thud" "fred"}}
             expected [{"op" "move" "from" "/foo/waldo" "path" "/qux/thud"}]]
         (fact "Moving a Value"
               (diff obj1 obj2) => expected))
       (let [obj1 {"foo" ["all" "grass" "cows" "eat"]}
             obj2 {"foo" ["all" "cows" "eat" "grass"]}
             expected [{"op" "move" "from" "/foo/1" "path" "/foo/3"}]]
         (fact "Moving an Array Element"
               (diff obj1 obj2) => expected)))

(facts "diff-vecs"
       (let [v1 ["all" "grass" "cows" "eat"]
             v2 ["all" "cows" "eat" "grass"]]
         (fact "two vectors with same elements in different order are not the same"
               (count (diff-vecs v1 v2 "/")) => 1
               (diff-vecs v1 v2 "/") => [{"from" "/1", "op" "move", "path" "/3"}])))

(facts "apply-op"
       (let [v ["all" "grass" "cows" "eat" "slowly"]
             patch {"from" "/1", "op" "move", "path" "/3"}
             expected ["all" "cows" "eat" "grass" "slowly"]]
         (apply-op v patch) => expected))

(facts "applying patch"
       (let [v ["all" "grass" "cows" "eat" "slowly"]
             p [{"from" "/1", "op" "move", "path" "/3"}]
             expected ["all" "cows" "eat" "grass" "slowly"]]
         (patch v p) => expected))

