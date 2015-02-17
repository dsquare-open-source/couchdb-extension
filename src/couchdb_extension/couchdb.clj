(ns couchdb-extension.couchdb
  (:use [be.dsquare.clutch :only (couch drop! up? exist?)]
        [com.ashafa.clutch :only (create!)])
  (:require [com.ashafa.clutch :as clutch]
            [clojure.string :as string]
            [be.dsquare.clutch :as dsquare-clutch])
  (:import [clojure.lang Keyword]))

(defn count-db [historianDB]
  (count historianDB))

(defn server-is-up? [^String database]
  (let [historianDB (couch database)]
    (up? historianDB)))

(defn database-exist? [database]
  (let [historianDB (couch database)]
    (exist? historianDB)))

(defn create-db [^String database]
  (let [historianDB (couch database)]
    (if-not (exist? historianDB)
      (clutch/create! historianDB))))

(defn drop-db [^String database]
  (let [historianDB (couch database)]
    (drop! historianDB)))

(defn first-time? [^String database]
  (let [historianDB (couch database)]
    (if-not (exist? historianDB)
      (do
        (clutch/create! historianDB)
        true)
      false)))

(defn store [^String database key map]
  (let [historianDB (couch database)]
    (clutch/assoc! historianDB key map)))

(defn remove-value [^String database key]
  (let [historianDB (couch database)]
    (clutch/dissoc! historianDB key)))

(defn take-all [^String database]
  (let [historianDB (couch database)]
    (dsquare-clutch/take-all historianDB)))

(defn get-value [^String database ^Keyword key]
  (let [historianDB (couch database)]
    (get historianDB key)))

(defn update-value [^String database ^Keyword key map]
  (let [historianDB (couch database)
        keyValue (get historianDB key)]
    (->>
      (if (not (nil? keyValue))
        (assoc map :_rev (:_rev keyValue))
        map)
      (clutch/assoc! historianDB key))))

(defn create-view!
  "Creates a Javascript view. You have to specify the database name,
  the design name, the view name and the javascript function"
  [^String database ^String design-name ^String view-name ^String javascript-function]
  (let [historian-db (couch database)]
    (dsquare-clutch/create-view! historian-db design-name view-name javascript-function)))

(defn get-view
  "Returns a couchdb view. You have to specify the database name,
  the design name and the view name.
  Returns a lazy-seq on the couchdb view"
  [^String database ^String design-name ^String view-name]
  (clutch/get-view database design-name (keyword view-name)))

(defn create-user-view!
  "The design name it will be in the database name with '-' and username.
  The same for the view name. Creates a concrete view for the user"
  [^String database ^String username ^String javascript-function]
  (let [historian-db (couch database)]
    (dsquare-clutch/create-user-view! historian-db username javascript-function)))

(defn get-user-view
  "The design name it will be the database name with '-' and username.
  The same for the view name. Returns a lazy-seq on the couchdb view."
  [^String database ^String username]
  (let [historian-db (couch database)]
    (dsquare-clutch/get-user-view historian-db username)))

(defn remove-view!
  "Removes a concrete view for the user"
  [^String database ^String design-view ^String view-name]
  (let [historian-db (couch database)]
    (dsquare-clutch/remove-user-view! historian-db design-view view-name)))

(defn remove-user-view!
  "Removes a concrete view for the user"
  [^String database ^String username]
  (let [historian-db (couch database)]
    (dsquare-clutch/remove-user-view! historian-db database username)))

(defn remove-configuration-watch [^clojure.lang.IRef reference]
  (remove-watch reference :configuration))

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
      (when (first-time? (cast-namespace (:namespace this)))
        (store (cast-namespace (:namespace this)) :configuration @(:reference this)))
      (->
        (get-value (cast-namespace (:namespace this)) :configuration)
        (dissoc :_id :_rev)
        (override-reference (:reference this)))
      (add-configuration-watch (:namespace this) (:reference this)))))

(defn database [^clojure.lang.Namespace namespace
                ^clojure.lang.IRef reference]
  (Database. namespace reference))
