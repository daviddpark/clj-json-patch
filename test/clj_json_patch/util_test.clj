(ns clj-json-patch.util-test
  (:require [clojure.test :refer :all]
            [clj-json-patch.util :refer :all])
  (:use [midje.sweet]))

(let [obj1 {"foo" "bar"}
      obj2 {"foo" {"bar" "baz"}}
      obj3 {"foo" {"bar" {"baz" "deep!"}}}
      obj4 ["foo" "bar"]
      obj5 {"foo" ["bar" "baz" "last"]}
      obj6 {"foo" {"bar" ["baz" "deeper!"]}}
      obj7 {"foo/bar" "baz"}]
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
               (get-patch-value obj6 "/foo/bar/1") => "deeper!")
         (fact "escape / with ~1"
               (get-patch-value obj7 "/foo~1bar") => "baz"))
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
               (get-value-path obj6 "deeper!") => "/foo/bar/1")
         (fact "get path from key with /"
               (get-value-path obj7 "baz") => "/foo~1bar")))

(facts "escaping characters"
       (fact "must not depend on ordering"
             (eval-escape-characters "~01") => "~1"))

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

(facts "get-patch-value and replace-patch-value"
       (let [obj {"k1" [{"s0k1" "s0v1"} {"s1k1" "s1v1"}]}
             p {"op" "replace" "path" "/k1/0/s0k1" "value" "new value"}
             patched (apply-patch obj p)]
         (fact "anticipated retrieval"
               (get-patch-value obj "/k1/0/s0k1") => "s0v1"
               (get-patch-value obj "/k1/1/s1k1") => "s1v1")
         (fact "mutation of object nested in array"
               (replace-patch-value obj "/k1/0/s0k1" "new value")
               => {"k1" [{"s0k1" "new value"} {"s1k1" "s1v1"}]})
         (fact "first nested object updated correctly"
               (get-patch-value patched "/k1/0/s0k1") => "new value")
         (fact "second nested object unchanged"
               (get-patch-value patched "/k1/1/s1k1") => "s1v1")))
