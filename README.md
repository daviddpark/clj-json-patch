clj-json-patch
==============

Clojure implementation of http://tools.ietf.org/html/rfc6902

Mostly implemented, though the last few examples have yet to be completed.

[![Build Status](https://travis-ci.org/daviddpark/clj-json-patch.png)](https://travis-ci.org/daviddpark/clj-json-patch)


Usage
-----

Generating patches with the diff function
-----------------------------------------

```clojure
user> (clj-json-patch.core/diff {"foo" "bar"} {"foo" ["bar"]})
[{"op" "replace", "path" "/foo", "value" ["bar"]}]

user> (clj-json-patch.core/diff {"foo" ["all" "grass" "cows" "eat"]}
                                {"foo" ["all" "cows" "eat" "grass"]})
[{"op" "move", "from" "/foo/1", "path" "/foo/3"}]
```

Applying patches with the patch function
-----------------------------------------

```clojure
user> (clj-json-patch.core/patch {"foo" "bar"} [{"op" "replace", "path" "/foo", "value" ["bar"]}])
{"foo" ["bar"]}

user> (clj-json-patch.core/patch {"foo" ["all" "grass" "cows" "eat"]}
                                 [{"op" "move", "from" "/foo/1", "path" "/foo/3"}])
{"foo" ["all" "cows" "eat" "grass"]}
```
