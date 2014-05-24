(ns forumdb.core
  (:import [org.hypergraphdb HGEnvironment HGHandle HGPlainLink HGQuery$hg]
           (java.util Locale TimeZone)
           (java.text SimpleDateFormat)
           (java.io File)
           (model User ForumThread Post)
           (org.joda.time DateTime))
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [clj-time.format :as f]
            [clj-time.core :as t]
  )
  (:gen-class :main true)
  (:import (model ForumThread))
  )

(def database (atom nil))
(def db-name "db")
(def data-files-directory "data")
(def xml-filename "tolkien.xml")

(def data-tag "rule")

(def thread-title-name "thread-title")
(def user-data-name "user-data")
(def user-login-name "user-login")
(def post-content-name "post-content")
(def post-details-name "post-details")

(defn create-database
  "
  Creates a database or opens existing one from the folder specified by argument
  "
  [path]
  (let [dbinstance (HGEnvironment/get path)]
    (reset! database dbinstance)))

(defn close-database
  "
  Closes the existing database
  "
  []
  (. @database close)
)

(defn delete-database
  "
  Deletes the database from the filesystem
  "
  []
  (def db-folder (File. db-name))
  (doseq [file (. db-folder listFiles)] (. file delete))
  (. db-folder delete)
)

(defn -main
  [& args]
  (do

    (if (. (File. db-name) exists)
      (do
        (println "Database already exists")
        (def database-exists)
        (println)
      )
      (do
        (println "Reading xml file...")
        (def parsed (xml/parse (string/join "/" [data-files-directory xml-filename])))
        (println "Xml file loaded")
        (println)
      )
    )

    (println "Opening database...")
    (create-database db-name)
    (println "Database opened")
    (println)

    (if (not database-exists)
      (do
        (def saveTime (Long. 0))

        (def locale (Locale. "pl" "PL"))
        (println "Parsing and saving data...")
        (def start (System/currentTimeMillis))

        (doseq [task-result (:content parsed)]
          (let [user (User.) thread (ForumThread.) post (Post.)]
            (doseq [rule (filter (fn [x] (= (name (:tag x)) data-tag)) (:content task-result))]
              (cond
                (= (:name (:attrs rule)) thread-title-name)
                (do
                  (. thread setTitle (string/trim (re-find (re-pattern "(?<=Temat: ).*") (first (:content rule)))))
                  )
                (= (:name (:attrs rule)) user-data-name)
                (do
                  (try
                    (. user setRank (string/trim (re-find (re-pattern "^.*(?=\nDołączył.a.: )") (string/trim (first (:content rule))))))
                    (catch Exception e (. user setRank "")))
                  (try
                    (. user setJoinTime (DateTime. (. (SimpleDateFormat. "dd MMM yyyy" locale) parse (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule))))))
                    (catch Exception e (. user setJoinTime nil)))
                  (try
                    (. user setPostCount (Integer/parseInt (re-find (re-pattern "(?<=Wpisy: ).*") (first (:content rule)))))
                    (catch Exception e (. user setPostCount 0)))
                  (try
                    (. user setCity (string/trim (re-find (re-pattern "(?<=Skąd: ).*") (first (:content rule)))))
                    (catch Exception e (. user setCity "")))
                  )
                (= (:name (:attrs rule)) user-login-name)
                (do
                  (. user setLogin (string/trim (first (:content rule))))
                  )
                (= (:name (:attrs rule)) post-content-name)
                (do
                  (try
                    (. post setContent (string/trim (first (:content rule))))
                    (catch Exception e (. post setContent "")))
                  )
                (= (:name (:attrs rule)) post-details-name)
                (do
                  (. post setCreateTime (f/parse (f/formatter "dd-MM-YYYY HH:mm" (t/default-time-zone)) (re-find (re-pattern "(?<=Wysłany: ).*") (first (:content rule)))))
                  (try
                    (. post setTitle (string/trim (re-find (re-pattern "(?<=Temat wpisu: ).*") (first (:content rule)))))
                    (catch Exception e (. post setTitle "")))
                  )
                )
              )

            (let [saveStart (System/currentTimeMillis)]
              (def userHandle (HGQuery$hg/assertAtom @database user))
              (def threadHandle (HGQuery$hg/assertAtom @database thread))
              (def postHandle (. @database add post))

              (. @database add (HGPlainLink. (into-array HGHandle [threadHandle userHandle postHandle])))
              ;(. @database add (HGPlainLink. (into-array HGHandle [userHandle postHandle])))
              ;(. @database add (HGPlainLink. (into-array HGHandle [threadHandle postHandle])))

              (def saveTime (+ saveTime (- (System/currentTimeMillis) saveStart)))
              )

            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [threadHandle userHandle]))) size))
            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [postHandle userHandle]))) size))
            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [postHandle threadHandle]))) size))
            ;(println)

            ;(println "Wątek")
            ;(println (. thread getTitle))
            ;(println)
            ;(println "Użytkownik")
            ;(println (. user getLogin))
            ;(println (. user getCity))
            ;(println (. user getRank))
            ;(println (. user getJoinTime))
            ;(println (. user getPostCount))
            ;(println)
            ;(println "Post")
            ;(println (. post getContent))
            ;(println (. post getCreateTime))
            ;(println (. post getTitle))
            ;(println)
            )
          )

        (def stop (System/currentTimeMillis))
        (println (string/join " " ["Data parsed and saved in" (String/valueOf (/ (- stop start) 1000.0)) "seconds"]))
        (println (string/join " " ["Saving took" (String/valueOf (/ saveTime 1000.0)) "seconds"]))
      )
      (do
        (println "Database already loaded, no need to parse")
        (println)
      )
    )

    (println "liczba tematów utworzonych w 2013 roku")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "najbardziej popularny temat w maju 2013")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "średnia długość tekstu posta")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "użytkownik wypowiadający się w największej liczbie tematów")
    (let [operationStart (System/currentTimeMillis)]
      (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/type User)))
      (def uniqueThreadsCount (map (fn [user] (reduce (fn [count thread] (if (> (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [user thread]))) size) 0) (+ count 1) count)) 0 (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))) userHandles))
      (def mostActiveUser (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector userHandles uniqueThreadsCount)))
      (println (. (. @database get (first mostActiveUser)) getLogin) (last mostActiveUser))
      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "użytkownik komentujący największą liczbę innych użytkowników")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "liczba postów zawierających słowo 'Frodo'")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "liczba postów wysłanych przez użytkowników z miasta na literę 'K'")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println "35te najczęściej użyte słowo w treści posta")
    (let [operationStart (System/currentTimeMillis)]

      (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
      )
    (println)

    (println)
    (println "Closing database...")
    (close-database)
    (println "Database closed")
    (println)

    ;(println "Deleting database...")
    ;(delete-database)
    ;(println "Database deleted")
    ;(println)

  )
)
