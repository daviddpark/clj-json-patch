(ns clj-json-patch.rfc6901-test
  (:require [clojure.test :refer :all]
            [clj-json-patch.util :refer :all])
  (:use [midje.sweet]))

;;https://tools.ietf.org/html/rfc6901

(facts "RFC6901"
       (let [doc {
                  "foo" ["bar" "baz"]
                  "" 0
                  "a/b" 1
                  "c%d" 2
                  "e^f" 3
                  "g|h" 4
                  "i\\j" 5
                  "k\"l" 6
                  " " 7
                  "m~n" 8}]

         (facts "basic strings"
                (fact "whole doc"
                      (get-patch-value doc "") => doc)
                (fact "/foo"
                      (get-patch-value doc "/foo") => ["bar" "baz"])
                (fact "/foo/0"
                      (get-patch-value doc "/foo/0") => "bar")
                (fact "/"
                      (get-patch-value doc "/") => 0)
                (fact "/a~1b"
                      (get-patch-value doc "/a~1b") => 1)
                (fact "/c%d"
                      (get-patch-value doc "/c%d") => 2)
                (fact "/e^f"
                      (get-patch-value doc "/e^f") => 3)
                (fact "/g|h"
                      (get-patch-value doc "/g|h") => 4)
                (fact "/i\\j"
                      (get-patch-value doc "/i\\j") => 5)
                (fact "/k\"l"
                      (get-patch-value doc "/k\"l") => 6)
                (fact "/ "
                      (get-patch-value doc "/ ") => 7)
                (fact "/m~0n"
                      (get-patch-value doc "/m~0n") => 8))
         (facts "fragment identifiers"
                (fact "# whole doc"
                      (get-patch-value doc "#") => doc)
                (fact "#/foo"
                      (get-patch-value doc "#/foo") => ["bar" "baz"])
                (fact "#/foo/0"
                      (get-patch-value doc "#/foo/0") => "bar")
                (fact "#/"
                      (get-patch-value doc "#/") => 0)
                (fact "#/a~1b"
                      (get-patch-value doc "#/a~1b") => 1)
                (fact "#/c%d"
                      (get-patch-value doc "#/c%d") => 2)
                (fact "#/e^f"
                      (get-patch-value doc "#/e^f") => 3)
                (fact "#/g|h"
                      (get-patch-value doc "#/g|h") => 4)
                (fact "#/i\\j"
                      (get-patch-value doc "#/i\\j") => 5)
                (fact "#/k\"l"
                      (get-patch-value doc "#/k\"l") => 6)
                (fact "#/ "
                      (get-patch-value doc "#/ ") => 7)
                (fact "#/m~0n"
                      (get-patch-value doc "#/m~0n") => 8))))
