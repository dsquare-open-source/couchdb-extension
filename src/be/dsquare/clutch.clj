(ns be.dsquare.clutch
  (:gen-class)
  (:use [com.ashafa.clutch :only (delete-database save-view view-server-fns)]
        [com.ashafa.clutch.view-server :only (view-server-exec-string)])
  (:import [com.ashafa.clutch CouchDB]
           [java.lang IllegalStateException]
           [java.net ConnectException]
           (clojure.lang Keyword)
           (java.lang String)))

;(defprotocol CouchViewsOps
;  "Defines side-effecting operations on a CouchDB database.
;  It extends clutch with some extra methods."
;  (create-view! [this design-name view-name javascript-function] "Creates View"))


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
  (take-all [this] (take (count this) this)))

(defmacro create-view!
  "It creates a view in a couchdb database"
  [couchdb design-name view-name javascript-function]
  `(do
    (save-view (.url ~couchdb) ~design-name
      (view-server-fns :javascript
        {~(keyword view-name)
         {:map ~javascript-function}})))
  )

(defn couch
  "Returns an instance of an implementation of CouchOpsExt"
  ([url] (CouchDB. url nil))
  ([url meta] (CouchDB. url meta)))



(in-ns 'be.dsquare.clutch)
(def db (couch "edu-test"))
;
;
(clojure.pprint/pprint (take-all db))
;(create-db "edu-test")
;
;(original-clutch/assoc! db "8" {:name "carlos" :type "A"})
;
;(clojure.pprint/pprint (meta *1))
;
;(original-clutch/configure-view-server "edu-test" (com.ashafa.clutch.view-server/view-server-exec-string))
;
;(original-clutch/save-view "edu-test" "edu-view-test-2"
;  (original-clutch/view-server-fns :clojure
;    {:edu-test-view-1 {:map (fn [doc] [[(:name doc) doc]])}}))
;
;(save-view "edu-test" "edu-view-javascript-1"
;  (view-server-fns :javascript
;    {:edu-javascript-1 {:map "function(doc) {if (doc.name && doc.name === 'edu') {emit(doc._id, doc);}}"}}))
;
;(create-view! db "edu-view-javascript-1" :edu-javascript-2 "function(doc) {if (doc.name && doc.name === 'carlos') {emit(doc._id, doc);}}")
(defn passing-parameters [couchdb design-name view-name javascript-function]
  (eval (@#' create-view! nil nil couchdb design-name view-name javascript-function))
  )

(passing-parameters db "edu-view-javascript-1" :edu-javascript-2 "function(doc) {if (doc.name && doc.name === 'carlos') {emit(doc._id, doc);}}")
(passing-parameters db "edu-view-javascript-2" :edu-javascript-3 "function(doc) {if (doc.name && doc.name === 'edu') {emit(doc._id, doc);}}")

 (->>
   (range 5 10)
   (map (fn [x] (passing-parameters db  (str "edu-view-javascript-" x) (str "edu-javascript-" x)
                  (str "function(doc) {if (doc.name && doc.name === 'edu" x "') {emit(doc._id, doc);}}"))))
   )


(clojure.pprint/pprint
  (macroexpand-all
    '(let [couchdb db design-name "edu-view-javascript-1" view-name :edu-javascript-2
           javascript-function "function(doc) {if (doc.name && doc.name === 'carlos') {emit(doc._id, doc);}}"]
    (create-view! couchdb  design-name view-name
       javascript-function)))
  )
;
;(let [view (original-clutch/get-view "edu-test" "edu-view-javascript-2" :edu-javascript-2)]
;  (->
;    (map #(:value %) (take (count view) view))
;    clojure.pprint/pprint))