(ns forumdb.core
  (:import [org.hypergraphdb HGEnvironment]
           (java.util Locale TimeZone)
           (java.text SimpleDateFormat)
           (java.io File)
           (model User ForumThread Post)
  )
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
  (doseq [file (. (File. db-name) listFiles)] (. file delete))
)

(defn -main
  [& args]
  (do

    (println "Reading xml file...")
    (def parsed (xml/parse (string/join "/" [data-files-directory "testfile.txt"])))
    (println "Xml file loaded")
    (println)

    (println "Creating database...")
    (create-database db-name)
    (println "Database created")
    (println)

    (doseq [task-result (:content parsed)]
      (let [user (User.) thread (ForumThread.) post (Post.)]
        (doseq [rule (filter (fn [x] (= (name (:tag x)) data-tag)) (:content task-result))]
          (cond
            (= (:name (:attrs rule)) thread-title-name)
            (do
              (. thread setTitle (re-find (re-pattern "(?<=Temat: ).*") (first (:content rule))))
            )
            (= (:name (:attrs rule)) user-data-name)
            (do
              (def locale (Locale. "pl" "PL"))
              (. user setRank (string/trim (re-find (re-pattern "^.*(?=\nDołączył.a.: )") (string/trim (first (:content rule))))))
              (. user setJoinTime (f/parse (f/with-locale (f/formatter "dd MM YYYY" (t/default-time-zone)) locale) (. (SimpleDateFormat. "dd MM yyyy" locale) format (. (SimpleDateFormat. "dd MMM yyyy" locale) parse (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule)))))))
              (. user setPostCount (Integer/parseInt (re-find (re-pattern "(?<=Wpisy: ).*") (first (:content rule)))))
              (. user setCity (string/trim (re-find (re-pattern "(?<=Skąd: ).*") (first (:content rule)))))
            )
            (= (:name (:attrs rule)) user-login-name)
            (do
              (. user setLogin (string/trim (first (:content rule))))
            )
            (= (:name (:attrs rule)) post-content-name)
            (do
              (. post setContent (string/trim (first (:content rule))))
            )
            (= (:name (:attrs rule)) post-details-name)
            (do
              (. post setCreateTime (f/parse (f/formatter "dd-MM-YYYY HH:mm" (t/default-time-zone)) (re-find (re-pattern "(?<=Wysłany: ).*") (first (:content rule)))))
              (. post setTitle (re-find (re-pattern "(?<=Temat wpisu: ).*") (first (:content rule))))
            )
          )
        )
        (println "Wątek")
        (println (. thread getTitle))
        (println)
        (println "Użytkownik")
        (println (. user getLogin))
        (println (. user getCity))
        (println (. user getRank))
        (println (. user getJoinTime))
        (println (. user getPostCount))
        (println)
        (println "Post")
        (println (. post getContent))
        (println (. post getCreateTime))
        (println (. post getTitle))
        (println)
      )
    )

    (def handler (. @database add "Przemek"))
    (println (. @database get handler))

    (println)

    (println "Closing database...")
    (close-database)
    (println "Database closed")
    (println)

    (println "Deleting database...")
    (delete-database)
    (println "Database deleted")
    (println)

  )
)
