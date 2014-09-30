(ns couchdb-extension.couchdb
  (:require [com.ashafa.clutch :as clutch]
            [clojure.string :as string])
  (:import [java.lang IllegalStateException]
           [java.net ConnectException]
           [clojure.lang Keyword]))

(defn count-db [historianDB]
  (count historianDB))

(defn server-is-up? [^String database]
  (let [historianDB (clutch/couch database)]
    (try (do
           (count-db historianDB)
           true)
      (catch ConnectException _ false)
      (catch IllegalStateException _ true))))

(defn database-exists? [historianDB]
  (try (do
         (count-db historianDB)
         true)
    (catch IllegalStateException _ false)))

(defn create-db [^String database]
  (let [historianDB (clutch/couch database)]
    (if-not (database-exists? historianDB)
      (clutch/create! historianDB))))

(defn first-time? [^String database]
  (let [historianDB (clutch/couch database)]
    (if-not (database-exists? historianDB)
      (do
        (clutch/create! historianDB)
        true)
      false)))

(defn store [^String database key map]
  (let [historianDB (clutch/couch database)]
    (clutch/assoc! historianDB key map)))

(defn remove-value [^String database key]
  (let [historianDB (clutch/couch database)]
    (clutch/dissoc! historianDB key)))

(defn take-all [^String database]
  (let [historianDB (clutch/couch database)]
    (take (count historianDB) historianDB)))

(defn get-value [^String database ^Keyword key]
  (let [historianDB (clutch/couch database)]
    (get historianDB key)))

(defn update-value [^String database ^Keyword key map]
  (let [historianDB (clutch/couch database)
        keyValue (get historianDB key)]
    (->>
      (if (not (nil? keyValue))
        (assoc map :_rev (:_rev keyValue))
        map)
      (clutch/assoc! historianDB key))))

(defn remove-configuration-watch [^clojure.lang.IRef reference]
  (remove-watch reference :configuration ))

(defn cast-namespace [^clojure.lang.Namespace namespace]
  (->
    namespace
    str
    (string/replace #"\." "-")))

(defn override-reference [map ^clojure.lang.IRef reference]
  (swap! reference (fn [old] map)))

(defn add-configuration-watch [^clojure.lang.Namespace namespace
                               ^clojure.lang.IRef reference]
  (add-watch reference
    :configuration (fn [key reference old-state new-state]
                     (update-value (cast-namespace namespace) :configuration new-state))))

(defprotocol DatabaseHandler
  (init [this])
  (destroy [this]))

(defrecord Database [^clojure.lang.Namespace namespace
                     ^clojure.lang.IRef reference]
  DatabaseHandler

  (destroy [this]
    (when (server-is-up? (cast-namespace (:namespace this)))
      (remove-configuration-watch (:reference this))))

  (init [this]
    (when (server-is-up? (cast-namespace (:namespace this)))
      (do
        (when (first-time? (cast-namespace (:namespace this)))
          (store (cast-namespace (:namespace this)) :configuration @(:reference this)))
        (->
          (get-value (cast-namespace (:namespace this)) :configuration )
          (dissoc :_id :_rev )
          (override-reference (:reference this)))
        (add-configuration-watch (:namespace this) (:reference this))))))

(defn database [^clojure.lang.Namespace namespace
                ^clojure.lang.IRef reference]
  (Database. namespace reference))