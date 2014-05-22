(ns hypergraphdb_tutorial.core
  (:import (org.hypergraphdb HGEnvironment HGHandle HGQuery$hg HGPlainLink)
           (java.util LinkedList))
  (:gen-class :main true)

(def database (atom nil))
(def handler (atom nil))
(def data-list (atom nil))


(defn create-database
  "
  Creates a database or opens existing one from the folder specified by argument
  "
  [path]
  (let [dbinstance (HGEnvironment/get path)]
    (reset! database dbinstance)))


(defn add-single-data
  [example]
  (reset! handler (.add @database example)))


(defn get-single-data
  []
  (.get @database @handler))


(defn add-linked-data
  []
  (let [item-handlers (map #(.add @database %) (list 1 2 3 4 5))]
    (reduce #(do (.add @database (new HGPlainLink (into-array HGHandle [%1 %2]))) %2) item-handlers)))


(defn get-linked-data
  []
  (HGQuery$hg/getAll @database (HGQuery$hg/type HGPlainLink)))


(defn print-linked-data
  [linked-data]
  (doall (map #(println (str (.get @database (.getTargetAt % 0)) " " (.get @database (.getTargetAt % 1)))) linked-data)))


(defn create-list
  []
  (do
    (reset! data-list (new LinkedList))
    (.add @data-list (new Integer 1))
    (.add @data-list (new Integer 2))
    (.add @data-list (new Integer 3))
    ))


(defn add-list-entry
  []
  (reset! handler (.add @database @data-list)))


(defn print-list
  []
  (println (clojure.string/join " " (.get @database @handler))))


(defn modify-list
  []
  (.add @data-list (new Integer 4)))


(defn update-list-entry
  []
  (.update @database @data-list))


(defn close-database
  []
  (.close @database))


(defn example-one
  []
  (do
    (create-database "hgdbtest")
    (add-single-data {:hello "world"})
    (println (get-single-data))
    (close-database)
    ))


(defn example-two
  []
  (do
    (create-database "hgdbtest")
    (add-linked-data)
    (print-linked-data (get-linked-data))
    (close-database)
    ))


(defn example-three
  []
  (do
    (create-database "hgdbtest")
    (create-list)
    (add-list-entry)
    (print-list)
    (modify-list)
    (update-list-entry)
    (print-list)
    (close-database)
    ))


(defn example-four
  []
  (do
    (create-database "hgdbtest")
    (.beginTransaction (.getTransactionManager @database))
    (try
      (.add @database (new Integer (/ 1 0)))
      (.commit (.getTransactionManager @database))
      (println "Transaction commited")
      (catch Exception e (do
                           (.abort (.getTransactionManager @database))
                           (println "Transaction aborted"))))
    (close-database)
    ))

(defn -main
  []
  (example-one))