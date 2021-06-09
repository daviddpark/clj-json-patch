(ns clj-json-patch.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [clj-json-patch.core-test-cljs]))


(doo-tests 'clj-json-patch.core-test-cljs)
