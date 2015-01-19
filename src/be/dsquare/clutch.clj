(ns be.dsquare.clutch
  (:use [com.ashafa.clutch :only (delete-database save-view view-server-fns)]
        [com.ashafa.clutch.view-server :only (view-server-exec-string)])
  (:import [com.ashafa.clutch CouchDB]
           [java.lang IllegalStateException]
           [java.net ConnectException]
           (clojure.lang Keyword)
           (java.lang String)))

(defmacro create-javascript-view
  "It creates a view in a couchdb database"
  [couchdb design-name view-name javascript-function]
  `(do
     (save-view (.url ~couchdb) ~design-name
       (view-server-fns :javascript
         {~(keyword view-name)
          {:map ~javascript-function}}))))

(defprotocol CouchViewsOps
  "Defines side-effecting operations on a CouchDB database.
  It extends clutch with some extra methods."
  (create-view! [this design-name view-name javascript-function] "Creates View")
  (get-view [this design-name view-name] "Returns a lazy-seq on the couchdb view")
  (create-user-view! [this username javascript-function] "The design name it will in be the database name with '-' and username. The same for the view name. Creates a concrete view for the user")
  (get-user-view [this username] "The design name it will be the database name with '-' and username. The same for the view name. Returns a lazy-seq on the couchdb view."))

(defprotocol CouchExtOps
  "Create / Delete views.
  It extends clutch with some extra methods."
  (drop! [this] "DELETES a CouchDB database. It returns {:ok true} if it works")
  (up? [this] "Checks if the server is up")
  (exist? [this] "Checks if the database exists")
  (take-all [this] "Returns all the items of a database"))

(extend-type CouchDB
  CouchExtOps
  (drop! [this] (delete-database (.url this)))
  (up? [this] (try (do
                     (count this)
                     true)
                (catch ConnectException _ false)
                (catch IllegalStateException _ true)))
  (exist? [this] (try (do
                        (count this)
                        true)
                   (catch IllegalStateException _ false)))
  (take-all [this] (take (count this) this))
  CouchViewsOps
  (create-view! [^CouchDB this ^String design-name ^String view-name ^String javascript-function]
    (eval (@#' create-javascript-view nil nil this design-name view-name javascript-function)))
  (get-view [^CouchDB this ^String design-name ^String view-name]
    (com.ashafa.clutch/get-view (.url this) design-name (keyword view-name)))
  (create-user-view! [^CouchDB this ^String username ^String javascript-function]
    (let [view-name (str (.url this) "-" username)]
      (eval (@#' create-javascript-view nil nil this view-name view-name javascript-function))))
  (get-user-view [^CouchDB this ^String username]
    (let [view-name (str (.url this) "-" username)]
      (com.ashafa.clutch/get-view (.url this) view-name (keyword view-name)))))

(defn couch
  "Returns an instance of an implementation of CouchOpsExt"
  ([url] (CouchDB. url nil))
  ([url meta] (CouchDB. url meta)))

