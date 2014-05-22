(ns forumdb.core
  (:import [org.hypergraphdb HGEnvironment]
           (java.util Locale TimeZone)
           (java.text SimpleDateFormat)
           (java.io File))
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [clj-time.format :as f]
            [clj-time.core :as t]
  )
  (:gen-class :main true)
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
      (doseq [rule (filter (fn [x] (= (name (:tag x)) data-tag)) (:content task-result))]
        (cond
          (= (:name (:attrs rule)) thread-title-name)
            (do
              (println "Title:")
              (println (re-find (re-pattern "(?<=Temat: ).*") (first (:content rule))))
            )
          (= (:name (:attrs rule)) user-data-name)
            (do
              (println "User data:")
              (println "    Rank:")
              (println (re-find (re-pattern "^.*(?=\nDołączył.a.: )") (string/trim (first (:content rule)))))
              (println "    Join date:")
              ;(println (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule))))
              (def locale (Locale. "pl" "PL"))
              (def dateFormat (SimpleDateFormat. "dd MMM yyyy" locale))
              (def date (. dateFormat parse (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule)))))
              (def digitDateFormat (SimpleDateFormat. "dd MM yyyy" locale))
              (def dateString (. digitDateFormat format date))
              ;(def parser (f/with-locale (f/formatter (t/time-zone-for-offset 2) "dd MMM YYYY" "dd MMM YYYY") (Locale. "pl" "PL")))
              (def parser (f/with-locale (f/formatter "dd MM YYYY" (t/default-time-zone)) locale))
              (println (f/parse parser dateString))
              ;(println (f/parse (f/with-locale (f/formatter "dd MM YYYY" (t/default-time-zone)) locale) (. (SimpleDateFormat. "dd MM yyyy" locale) format (. (SimpleDateFormat. "dd MMM yyyy" locale) parse (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule)))))))
              ;(println (re-find (re-pattern "(?<=Dołączył.a.: ).*") (first (:content rule))))
              (println "    Posts count:")
              (println (re-find (re-pattern "(?<=Wpisy: ).*") (first (:content rule))))
              (println "    City:")
              (println (re-find (re-pattern "(?<=Skąd: ).*") (first (:content rule))))
            )
          (= (:name (:attrs rule)) user-login-name)
            (do
              (println "User login:")
              (println (first (:content rule)))
            )
          (= (:name (:attrs rule)) post-content-name)
            (do
              (println "Post content:")
              (println (first (:content rule)))
            )
          (= (:name (:attrs rule)) post-details-name)
            (do
              (println "Post details:")
              (println "    Date:")
              (def parser (f/formatter "dd-MM-YYYY HH:mm" (t/default-time-zone)))
              (def date (f/parse parser (re-find (re-pattern "(?<=Wysłany: ).*") (first (:content rule)))))

              (println (. @database get (. @database add date)))
              ;(println (f/unparse multi-parser (f/parse multi-parser (re-find (re-pattern "(?<=Wysłany: ).*") (first (:content rule))))))
              (println "    Title:")
              (println (re-find (re-pattern "(?<=Temat wpisu: ).*") (first (:content rule))))
            )
        )
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
