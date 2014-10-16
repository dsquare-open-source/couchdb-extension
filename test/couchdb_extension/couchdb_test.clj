(ns couchdb-extension.couchdb-test
  (:refer-clojure :exclude [clojure.core/count])
  (:use clojure.test
        midje.sweet)
  (:require [couchdb-extension.couchdb :as couchdb]
            [couchdb-extension.clutch :as clutch-extended]
            [com.ashafa.clutch :as clutch])
  (:import [java.lang IllegalStateException]
           [java.util Date]
           [java.net ConnectException]))

(def currentNamespace (str *ns*))

(fact "if the server is down we return false"
  (couchdb/server-is-up? currentNamespace) => false
  (provided (clutch-extended/couch currentNamespace) => anything)
  (provided (couchdb/count-db anything) =throws=> (ConnectException.)))

(fact "if the server is up we return true"
  (couchdb/server-is-up? currentNamespace) => true
  (provided (couchdb/count-db anything) => 3))

(fact "if the server is up and the database is ready we just return correctly"
  (couchdb/first-time? currentNamespace) => false
  (provided (clutch-extended/couch currentNamespace) => anything)
  (provided (couchdb/count-db anything) => 3))

(fact "if the server is up but the database is not created we create it"
  (couchdb/first-time? currentNamespace) => true
  (provided (clutch-extended/couch currentNamespace) => anything)
  (provided (couchdb/count-db anything) =throws=> (IllegalStateException.))
  (provided (clutch/create! anything) => anything))

(fact "Store default configuration for the historian"
  (couchdb/store currentNamespace :configuration {"username" "Daniil" "password" "password" "server" "pi-connector-test.dsquare.intra"}) => anything
  (provided (clutch-extended/couch currentNamespace) => "mock")
  (provided
    (clutch/assoc! "mock" :configuration {"username" "Daniil" "password" "password" "server" "pi-connector-test.dsquare.intra"})
    => {:result {:ok true, :id ":configuration", :rev "2-16870bc1dc06755b895ba715da9041ce"}}))

(fact "Get values for the configuration"
  (couchdb/get-value currentNamespace :configuration ) => {:_id ":configuration"
                                                           :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                           :server "pi-connector-test.dsquare.intra"
                                                           :username "Daniil"
                                                           :password "password"}
  (provided (clutch-extended/couch currentNamespace) => {:configuration {:_id ":configuration"
                                                                         :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                                         :server "pi-connector-test.dsquare.intra"
                                                                         :username "Daniil"
                                                                         :password "password"}}))

(fact "update configuration"
  (couchdb/update-value currentNamespace :configuration {:username "edu" :password "edu-passowrd" :server "pi-connector-test.dsquare.intra"}) => anything

  (provided (clutch-extended/couch currentNamespace) => {:configuration {:_id ":configuration"
                                                                         :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                                         :server "pi-connector-test.dsquare.intra"
                                                                         :username "Daniil"
                                                                         :password "password"}})
  (provided (clutch/assoc!
              anything :configuration {:_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                       :server "pi-connector-test.dsquare.intra"
                                       :username "edu", :password "edu-passowrd"}) => anything))

(fact "update configuration when the key does not exist"
  (couchdb/update-value currentNamespace :configuration {:username "edu"}) => anything

  (provided (clutch-extended/couch currentNamespace) => {})
  (provided (clutch/assoc!
              anything :configuration {:username "edu"}) => anything))


(def testAtom (atom {:username "Daniil"
                     :password "password"
                     :server "pi-connector-test.dsquare.intra"}))

(def currentNamespace *ns*)
(def db (couchdb/database currentNamespace testAtom))


(fact "if the server is down we just return. Everything is going to be in memory"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "couchdb-extension-couchdb-test") => false))

(fact "If the server is up we check if it's the first time"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "couchdb-extension-couchdb-test") => true)
  (provided (couchdb/first-time? "couchdb-extension-couchdb-test") => false)
  (provided (couchdb/get-value "couchdb-extension-couchdb-test" :configuration )
    => {:_id ":configuration"
        :_rev "3-887ec7ea147165566a5ac5948eb7c383"
        :server "pi-connector-test.dsquare.intra"
        :username "Daniil"
        :password "password"})
  (provided (couchdb/override-reference {:server "pi-connector-test.dsquare.intra"
                                         :username "Daniil"
                                         :password "password"} testAtom) => anything)
  (provided (couchdb/add-configuration-watch currentNamespace testAtom) => anything))

(fact "If it's the first time we story the default values"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "couchdb-extension-couchdb-test") => true)
  (provided (couchdb/first-time? "couchdb-extension-couchdb-test") => true)
  (provided (couchdb/store "couchdb-extension-couchdb-test" :configuration {:username "Daniil" :password "password" :server "pi-connector-test.dsquare.intra"}) => anything)
  (provided (couchdb/get-value "couchdb-extension-couchdb-test" :configuration )
    => {:_id ":configuration", :_rev "3-887ec7ea147165566a5ac5948eb7c383", :server "pi-connector-test.dsquare.intra", :username "Daniil", :password "password"})
  (provided (couchdb/override-reference {:server "pi-connector-test.dsquare.intra"
                                         :username "Daniil"
                                         :password "password"} testAtom) => anything)
  (provided (couchdb/add-configuration-watch currentNamespace testAtom) => anything))

(fact "if the server is up remove the watch when destroying it"
  (couchdb/destroy db) => anything
  (provided (couchdb/server-is-up? "couchdb-extension-couchdb-test") => true)
  (provided (couchdb/remove-configuration-watch testAtom) => anything))
