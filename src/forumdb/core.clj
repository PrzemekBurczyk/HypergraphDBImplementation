(ns forumdb.core
  (:import [org.hypergraphdb HGEnvironment HGHandle HGPlainLink HGQuery$hg HGQuery]
           (java.util Locale TimeZone)
           (java.text SimpleDateFormat)
           (java.io File)
           (model User ForumThread Post)
           (org.joda.time DateTime)
           (org.hypergraphdb.atom HGRel HGRelType)
           (org.hypergraphdb.query HGQueryCondition AtomPartRegExPredicate)
           (java.util.regex Pattern))
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
        (def database-exists 1)
        (println)
      )
      (do
        (println "Reading xml file...")
        (def parsed (xml/parse (string/join "/" [data-files-directory xml-filename])))
        (def database-exists 0)
        (println "Xml file loaded")
        (println)
      )
    )

    (println "Opening database...")
    (create-database db-name)
    (println "Database opened")
    (println)

    (def topType (. (. @database getTypeSystem) getTop))
    (def userThreadRelType (HGQuery$hg/assertAtom @database (HGRelType. "user-thread" (into-array HGHandle [topType topType]))))
    (def userPostRelType (HGQuery$hg/assertAtom @database (HGRelType. "user-post" (into-array HGHandle [topType topType]))))
    (def threadPostRelType (HGQuery$hg/assertAtom @database (HGRelType. "thread-post" (into-array HGHandle [topType topType]))))
    (def threadFirstPostRelType (HGQuery$hg/assertAtom @database (HGRelType. "thread-first-post" (into-array HGHandle [topType topType]))))

    (if (= database-exists 0)
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

              ;(. @database add (HGPlainLink. (into-array HGHandle [threadHandle userHandle postHandle])))
              (HGQuery$hg/addUnique @database (HGRel. (into-array HGHandle [userHandle threadHandle])) userThreadRelType (HGQuery$hg/link (into-array HGHandle [userHandle threadHandle])))
              (. @database add (HGRel. (into-array HGHandle [userHandle postHandle])) userPostRelType)
              (. @database add (HGRel. (into-array HGHandle [threadHandle postHandle])) threadPostRelType)

              (def saveTime (+ saveTime (- (System/currentTimeMillis) saveStart)))
              )

            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [threadHandle userHandle]))) size))
            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [postHandle userHandle]))) size))
            ;(println (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [postHandle threadHandle]))) size))
            ;(println)
            )
          )

        (println "Preprocessing...")
        (let [saveStart (System/currentTimeMillis)]
          (do
            (def threadHandles (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))
            (println 1)
            (def threadsPostsLinks (doall (map (fn [threadHandle] (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident threadHandle) (HGQuery$hg/type threadPostRelType)])))) threadHandles)))
            (println 2)
            (def threadsPosts (doall (map (fn [threadPostsLinks] (map (fn [threadPostLink] (HGQuery$hg/findOne @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/target threadPostLink) (HGQuery$hg/type Post)])))) threadPostsLinks)) threadsPostsLinks)))
            (println 3)
            (def threadsFirstPost (doall (map (fn [threadPosts] (reduce (fn [currentFirstPost threadPost] (if (t/before? (. (. @database get currentFirstPost) getCreateTime) (. (. @database get threadPost) getCreateTime)) currentFirstPost threadPost)) threadPosts)) threadsPosts)))
            (println 4)
            ;(map (fn [threadHandle threadFirstPost] (. @database add (HGRel. (into-array HGHandle [threadHandle threadFirstPost])) threadFirstPostRelType)) threadHandles threadsFirstPost)
            (doseq [threadPostPair (map vector threadHandles threadsFirstPost)]
              (. @database add (HGRel. (into-array HGHandle [(first threadPostPair) (last threadPostPair)])) threadFirstPostRelType)
              )

            (def saveTime (+ saveTime (- (System/currentTimeMillis) saveStart)))
            (def preprocessTime (- (System/currentTimeMillis) saveStart))
            )
          )

        (def stop (System/currentTimeMillis))
        (println (string/join " " ["Data parsed and saved in" (String/valueOf (/ (- stop start) 1000.0)) "seconds"]))
        (println (string/join " " ["Saving took" (String/valueOf (/ saveTime 1000.0)) "seconds"]))
        (println (string/join " " ["Preprocessing took" (String/valueOf (/ preprocessTime 1000.0)) "seconds"]))
        (println)
      )
      (do
        (println "Database already loaded, no need to parse")
        (println)

      )
    )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "liczba tematów utworzonych w 2013 roku")
        (def threadFirstPostLinks (HGQuery$hg/findAll @database (HGQuery$hg/type threadFirstPostRelType)))
        (println (count threadFirstPostLinks))
        (def threadFirstPosts (map (fn [threadFirstPostLink] (HGQuery$hg/getOne @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Post) (HGQuery$hg/target threadFirstPostLink)])))) threadFirstPostLinks))
        (println (count threadFirstPosts))
        (println (. (filter (fn [threadFirstPost] (t/within? (t/interval (t/date-time 2013) (t/date-time 2014)) (. threadFirstPost getCreateTime))) threadFirstPosts) size))

        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "najbardziej popularny temat w maju 2013")

        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "średnia długość tekstu posta")
        (def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        (println (quot (reduce (fn [count post] (+ (. (. post getContent) length) count)) 0 posts) (count posts)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "użytkownik wypowiadający się w największej liczbie tematów")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/type User)))
        (def uniqueThreadsCount (map (fn [userHandle] (HGQuery$hg/count @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type userThreadRelType)])))) userHandles))
        ;(def uniqueThreadsCount (map (fn [user] (reduce (fn [count thread] (if (> (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [user thread]))) size) 0) (+ count 1) count)) 0 (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))) userHandles))
        (def mostActiveUser (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector userHandles uniqueThreadsCount)))
        (println (. (. @database get (first mostActiveUser)) getLogin) (last mostActiveUser))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "użytkownik komentujący największą liczbę innych użytkowników")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/type User)))
        (def postsCount (map (fn [userHandle] (HGQuery$hg/count @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type userPostRelType)])))) userHandles))
        (def mostActiveUser (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector userHandles postsCount)))
        (println (. (. @database get (first mostActiveUser)) getLogin) (last mostActiveUser))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "liczba postów zawierających słowo 'Frodo'")
        (println (HGQuery$hg/count @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Post) (AtomPartRegExPredicate. (into-array String ["content"]) (Pattern/compile ".*Frodo.*" Pattern/DOTALL))]))))
        ;(def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        ;(println (count (filter (fn [post] (if (re-find #"Frodo" (. post getContent)) true false)) posts)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "liczba postów wysłanych przez użytkowników z miasta na literę 'K'")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type User) (AtomPartRegExPredicate. (into-array String ["city"]) (Pattern/compile "^K.*" Pattern/MULTILINE))]))))
        ;(def userHandles (filter (fn [userHandle] (. (. (. @database get userHandle) getCity) startsWith "K")) (HGQuery$hg/findAll @database (HGQuery$hg/type User))))
        (println (apply + (map (fn [userHandle] (. (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type userPostRelType)]))) size)) userHandles)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
      )
    (println)

    (println "35te najczęściej użyte słowo w treści posta")
    (let [operationStart (System/currentTimeMillis)]
      (do
        (def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        (println (first (nth (sort-by val > (reduce (fn [wordMap post] (merge-with + wordMap (reduce (fn [postWordMap word] (assoc postWordMap word (+ 1 (get postWordMap word 0)))) {} (string/split (string/triml (string/replace (. post getContent) (Pattern/compile "\\W" Pattern/UNICODE_CHARACTER_CLASS) " ")) #"\s+")))) {} posts)) 34)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        )
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
