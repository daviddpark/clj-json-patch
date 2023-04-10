(ns clj-json-patch.runner-cljs
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [clj-json-patch.core-test-cljs]))

(doo-tests 'clj-json-patch.core-test-cljs)
