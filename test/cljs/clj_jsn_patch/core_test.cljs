(ns clj-json-patch.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [clj-json-patch.core :refer [diff patch]]))


(deftest test-patch
  (let [v ["all" "grass" "cows" "eat" "slowly"]
        p [{"from" "/1", "op" "move", "path" "/3"}]
        expected ["all" "cowst" "eat" "grass" "slowly"]
        patched (patch v p)]
    (is (= patched expected))))
