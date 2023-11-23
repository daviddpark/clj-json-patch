(ns clj-json-patch.core-test
  (:require [clojure.test :refer :all]
            [clj-json-patch.core :refer :all])
  (:use [midje.sweet]))

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
       (let [obj1 {"foo" []}
             obj2 {"foo" ["bar" "baz"]}
             patches [{"op" "add" "path" "/foo/0" "value" "bar"}
                      {"op" "add" "path" "/foo/1" "value" "baz"}]]
         (fact "Adding two Array Elements"
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
         (fact "Removing a Nested Object Member"
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" {"baz" "boo"}}
             obj2 {"child" {"grandchild" {}}
                   "foo" {"baz" "boo"}}
             patches [{"op" "add" "path" "/child" "value" {"grandchild" {}}}]]
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
               (diff obj1 obj2) => patches))
       (let [obj1 {"foo" "bar"}
             obj2 {"foo" ["all" "cows" "eat" "grass"]}
             patches [{"op" "replace" "path" "/foo"
                       "value" ["all" "cows" "eat" "grass"]}]]
         (fact "Replace a value with an Array Element"
               (diff obj1 obj2) => patches))
       (let [obj1 {"key1" nil
                   "key2" "val2"}
             obj2 {"key2" "val2"}]
         (fact "nil key vs absent key"
               (diff obj1 obj2) => [{"op" "remove", "path" "/key1"}]))
       (let [obj1 {"test" {"key1" nil
                           "key2" "val2"}}
             obj2 {"test" {"key2" "val2"}}]
         (fact "nil key vs absent key, nested"
               (diff obj1 obj2) => [{"op" "remove", "path" "/test/key1"}]))
       (let [obj1 {"test" [{"key1" "val1"}]}
             obj2 {"test" [{"key1" "val1replaced"}]}]
         (fact "object val within a vector replaced"
               (diff obj1 obj2) => [{"op" "replace", "path" "/test/0/key1", "value" "val1replaced"}]))
       (let [obj1 {"test" [{"key1" nil
                            "key2" "val2"}]}
             obj2 {"test" [{"key2" "val2"}]}]
         (fact "nil key vs absent key within a vector"
               (diff obj1 obj2) => [{"op" "remove", "path" "/test/0/key1"}])))

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
      (let [obj1 {"foo" false "baz" "qux"}
             obj2 {"baz" "qux" "foo" true}
             patches [{"op" "replace" "path" "/foo" "value" true}]]
         (fact "Replacing a boolean false Value"
               (patch obj1 patches) => obj2))
      (let [obj1 {"foo" {"bar" "kill me"
                         "baz" "boo"}}
            obj2 {"foo" {"baz" "boo"}}
            patches [{"op" "remove" "path" "/foo/bar"}]]
        (fact "Removing a Nested Object Member"
              (patch obj1 patches) => obj2))
      (let [obj1 {"foo" {"baz" "boo"}}
            obj2 {"child" {"grandchild" {}}
                  "foo" {"baz" "boo"}}
            patches [{"op" "add" "path" "/child" "value" {"grandchild" {}}}]]
        (fact "Adding a Nested Object Member"
              (patch obj1 patches) => obj2))
      (let [obj1 {"foo" ["baz" "boo"]}
            obj2 {"foo" ["baz" "boo" "added"]}
            patches [{"op" "add" "path" "/foo/2" "value" "added"}]]
        (fact "Adding to a nested array"
              (patch obj1 patches) => obj2))
      (let [obj1 {"foo" ["baz" "boo"]}
            obj2 {"foo" ["baz" "boo" "added"]}
            patches [{"op" "add" "path" "/foo/-" "value" "added"}]]
        (fact "Adding to the end of a nested array"
              (patch obj1 patches) => obj2))
      (let [obj1 {"foo" "bar"}
            obj2 {"foo" "bar" "baz" "qux"}
            patches [{"op" "add" "path" "/baz" "value" "qux" "xyz" 123}]]
        (fact "Ignoring unrecognized elements"
              (patch obj1 patches) => obj2))
      (let [obj1 {"foo" "bar"}
            patches [{"op" "add" "path" "/baz/bat" "value" "qux"}]]
        (fact "Adding to a nonexistent target"
              (patch obj1 patches) => (throws Exception "Unable to set value at '/baz/bat'. Consider adding a more explicit data structure as a child of an existing object.")))
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
              (patch obj patches) => obj))
      (let [obj {"k1" [{"s0k1" "s0v1"} {"s1k1" "s1v1"}]}
            patches [{"op" "replace" "path" "/k1/0/s0k1" "value" "new value"}]
            patched (patch obj patches)]
        (fact "expected patched object"
              patched => {"k1" [{"s0k1" "new value"} {"s1k1" "s1v1"}]})
        (fact "first nested object updated correctly"
              (get (first (get patched "k1")) "s0k1") => "new value")
        (fact "second nested object unchanged"
              (get (second (get patched "k1")) "s1k1") => "s1v1"))
      (fact "Patch with escape characters"
            (patch {"foo" {"bar" 42}}
                   [{"op" "add", "path" "/foo/baz~1bar", "value" "ohyeah"}]) => {"foo" {"bar" 42 "baz/bar" "ohyeah"}}))

(facts "Nested JSON patch"
       (let [obj1 {"nested" {"foo" "bar"}}
             obj2 {"nested" {"foo" "bar"
                             "zot" "baz"}}
             patches [{"op" "add" "path" "/nested/zot" "value" "baz"}]]
         (fact "nested object is patched"
               (patch obj1 patches) => obj2)))

(facts "Remove from object within array"
       (let [obj1    [{"name" "item1" "foo" "bar"}
                      {"name" "item2" "foo" "bar"}]
             obj2    [{"name" "item1"}
                      {"name" "item2" "foo" "bar"}]
             patches [{"op" "remove" "path" "/0/foo"}]]
         (fact "nested object is patched"
               (patch obj1 patches) => obj2)))

(facts "Add to object within array"
       (let [obj {"array" [{"id" "hello"}]}
             expected {"array" [{"id" "hello" "foo" "bar"}]}
             patches [{"op" "add" "path" "/array/0/foo" "value" "bar"}]]
         (fact "obj with two keys as only element of array"
               (patch obj patches) => expected)))

(facts "Patch error conditions"
       (let [obj1 {"foo" "bar"}
             patches [{"op" "test" "path" "/baz" "value" "qux"}]]
         (fact "Testing an Object Member"
               (patch obj1 patches) => (throws Exception "The test failed. \"qux\" is not found at /baz. The value is: null")))
       (let [obj1 ["foo" "bar"]
             patches [{"op" "test" "path" "/4" "value" "qux"}]]
         (fact "Testing an Array Member"
               (patch obj1 patches) => (throws Exception "The test failed. \"qux\" is not found at /4. ")))
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
               (patch obj1 patches) => (throws Exception "Unable to set value at 3.")))
       (let [obj1 {"foo" "bar" "boo" "qux"}
             patches [{"op" "replace" "path" "/baz" "value" "boo"}]]
         (fact "Replacing a Value that doesn't exist"
               (patch obj1 patches) => (throws Exception "Can't replace a value that does not exist at '/baz'.")))
       (let [obj1 {"foo" {"beer" "booze"
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
               (patch obj1 patches) => (throws Exception "Move attempted on value that does not exist at '/-1'.")))
       (let [obj1 {"foo" ["all" "grass" "cows" "eat"]}
             patches [{"op" "add" "path" "/foo/two"}]]
         (fact "Adding an Array Element to an non-number index"
               (patch obj1 patches) => (throws Exception "Unable to determine array index from 'two'."))))
