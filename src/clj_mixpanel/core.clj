(ns clj-mixpanel.core
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-http.client :as http]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]))

(def config (atom {:api-key nil
                   :api-secret nil}))

(defn configure [c]
  (swap! config merge c))

; stolen from https://gist.github.com/1302024
(defn md5
  "Generate a md5 checksum for the given string"
  [token]
  (let [hash-bytes
         (doto (java.security.MessageDigest/getInstance "MD5")
               (.reset)
               (.update (.getBytes token)))]
       (.toString
         (new java.math.BigInteger 1 (.digest hash-bytes)) ; Positive and the size of the number
         16))) ; Use base16 i.e. hex

(defn calculate-sig [params]
  "Calculate the `sig` query parameter for an api request based on the provided parameters.
   See: https://mixpanel.com/docs/api-documentation/data-export-api#auth-implementation"
  (let [mashed (string/join (map (fn [[k v]] (str k "=" v)) (sort params)))
        with-secret (str mashed (:api-secret @config))]
    (md5 with-secret)))

(defn unsigned-query-params [extra-params]
  (merge {"api_key" (:api-key @config)
          "expire" (-> (t/now) (t/plus (t/minutes 60)) (tc/to-long))}
         extra-params))

(defn generate-query-params [params]
  "Merge user-provided query parameters with defaults and calculated signature resulting in all required params"
  (let [unsigned (unsigned-query-params params)]
    (merge unsigned {"sig" (calculate-sig unsigned)})))

(defn fetch 
  "Fetch a set of records from the mixpanel api that match the provided conditions.
   Conditions currently must be specified in url encoded format.
   See: https://mixpanel.com/docs/api-documentation/data-export-api#segmentation-expressions"
  ([conditions]
    (try+
      (let [response (http/get "http://mixpanel.com/api/2.0/engage/"
                               {:query-params (generate-query-params conditions)})]
        (json/read-json (:body response)))
      (catch Object o (prn (:body (:object &throw-context))))))
  ([] (fetch {})))

(defn fetch-with-retry [conditions max-retries]
  (first (filter #(not (nil? %)) (repeatedly max-retries #(fetch conditions)))))

(defn record-seq
  "Get all records that match the provided conditions, including handling pagination.
   Returns a seq over the records."
  ([conditions page-size total]
    (let [params conditions
          response (fetch-with-retry params 3)]
      (lazy-cat 
        (:results response)
        (let [current-page-size (or page-size (:page_size response))
              current-total (or total (:total response))]
          (when (< (* (:page response) current-page-size) current-total)
            (record-seq
              (merge params {"page" (+ 1 (:page response)) "session_id" (:session_id response)})
              current-page-size
              current-total))))))
  ([conditions] (record-seq {} nil nil))
  ([] (record-seq {})))
