(ns couchdb-extension.clutch
  (:use [com.ashafa.clutch :only (delete-database)])
  (:import [com.ashafa.clutch CouchDB]
           [java.lang IllegalStateException]
           [java.net ConnectException]))


(defn count-db [historianDB]
  (count historianDB))


(defprotocol CouchExtOps
  "Defines side-effecting operations on a CouchDB database.
  It extends clutch with some extra methods."
  (drop! [this] "DELETES a CouchDB database. It returns {:ok true} if it works")
  (up? [this] "Checks if the server is up" )
  (exist? [this] "Checks if the database exists"))

(extend-type CouchDB
  CouchExtOps
  (drop! [this] (delete-database (.url this)))
  (up? [this] (try (do
                     (count-db this)
                     true)
                (catch ConnectException _ false)
                (catch IllegalStateException _ true)))
  (exist? [this] (try (do
                        (count-db this)
                        true)
                   (catch IllegalStateException _ false))))

(defn couch
  "Returns an instance of an implementation of CouchOpsExt"
  ([url] (CouchDB. url nil))
  ([url meta] (CouchDB. url meta)))
