(ns clj-json-patch.middleware
  (:require [cheshire.core :as json]))

(defn- json-patch-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/json-patch+json" type)))))

(defn- read-json-patch [request & [keywords?]]
  (if (json-patch-request? request)
    (if-let [body (:body request)]
      (json/parse-string (slurp body) keywords?))))

(defn wrap-json-patch-params
  "Middleware that converts request bodies in JSON-patch format to a
  map containing the key of :patch and the value representing the
  json serialized vector of patch operations."
  [handler & [{:keys [keywords?]}]]
  (fn [request]
    (let [json (read-json-patch request keywords?)]
      (if (and json (vector? json))
        (let [patches {:patches json}]
          (handler (-> request
                       (assoc :json-params patches)
                       (update-in [:params] merge patches))))
        (handler request)))))
