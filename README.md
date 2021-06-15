clj-json-patch
==============

Clojure(script) implementation of JSON Patch as specified in
http://tools.ietf.org/html/rfc6902 with support for
JSON Pointer https://tools.ietf.org/html/rfc6901

[![Build Status](https://travis-ci.org/daviddpark/clj-json-patch.png)](https://travis-ci.org/daviddpark/clj-json-patch)


Usage
-----
```clojure
[clj-json-patch 0.1.7]

;; From some example namespace:
(ns example.namespace
  (:require [clj-json-patch.core :refer :all]))
```

Generating patches with the diff function
-----------------------------------------

```clojure
clj-json-patch.core=> (diff {"foo" "bar"} {"foo" ["bar"]})
[{"op" "replace", "path" "/foo", "value" ["bar"]}]

clj-json-patch.core=> (diff {"foo" ["all" "grass" "cows" "eat"]}
                            {"foo" ["all" "cows" "eat" "grass"]})
[{"op" "move", "from" "/foo/1", "path" "/foo/3"}]
```

```clojure
clj-json-patch.core=> (diff (.stringify js/JSON (clj->js {"foo" "bar"}))
                            (.stringify js/JSON (clj->js  {"foo" ["bar"]})))
[{"op" "replace", "path" "/foo", "value" ["bar"]}]

clj-json-patch.core=> (diff (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
                            (.stringify js/JSON (clj->js {"foo" ["all" "cows" "eat" "grass"]})))
[{"op" "move", "from" "/foo/1", "path" "/foo/3"}]
```

Applying patches with the patch function
-----------------------------------------

```clojure
clj-json-patch.core=> (patch {"foo" "bar"} [{"op" "replace", "path" "/foo", "value" ["bar"]}])
{"foo" ["bar"]}

clj-json-patch.core=> (patch {"foo" ["all" "grass" "cows" "eat"]}
                             [{"op" "move", "from" "/foo/1", "path" "/foo/3"}])
{"foo" ["all" "cows" "eat" "grass"]}
```

```clojure
clj-json-patch.core=> (patch (.stringify js/JSON (clj->js {"foo" "bar"}))
                             (.stringify js/JSON (clj->js [{"op" "replace", "path" "/foo", "value" ["bar"]}])))
{"foo" ["bar"]}

clj-json-patch.core=> (patch (.stringify js/JSON (clj->js {"foo" ["all" "grass" "cows" "eat"]}))
                             (.stringify js/JSON (clj->js [{"op" "move", "from" "/foo/1", "path" "/foo/3"}])))
{"foo" ["all" "cows" "eat" "grass"]}
```

Run Unit Tests for Clojure
--------------

```shell
lein midje
```

Run Unit Tests for ClojureScript (depends on firefox)
--------------

```shell
lein doo once
```
