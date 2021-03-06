(ns forumdb.core
  (:import [org.hypergraphdb HGEnvironment HGHandle HGPlainLink HGQuery$hg HGQuery HGValueLink HGConfiguration]
           (java.util Locale TimeZone)
           (java.text SimpleDateFormat)
           (java.io File)
           (model User ForumThread Post)
           (org.joda.time DateTime)
           (org.hypergraphdb.atom HGRel HGRelType)
           (org.hypergraphdb.query HGQueryCondition AtomPartRegExPredicate)
           (java.util.regex Pattern)
           (org.hypergraphdb.indexing ByPartIndexer)
           (org.hypergraphdb.handle SequentialUUIDHandleFactory))
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
  (def config (HGConfiguration.))
  (def handleFactory (SequentialUUIDHandleFactory. (System/currentTimeMillis) 0))
  (. config setHandleFactory handleFactory)
  (. config setTransactional false)
  (def storeConfig (. (. config getStoreImplementation) getConfiguration))
  (. (. storeConfig getEnvironmentConfig) setCacheSize (* (* 1024 1024) 500))
  (let [dbinstance (HGEnvironment/get path config)]
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
    ;(def userPostRelType (HGQuery$hg/assertAtom @database (HGRelType. "user-post" (into-array HGHandle [topType topType]))))

    ;(def postTypeHandle (. (. @database getTypeSystem) getTypeHandle Post))
    ;(. (. @database getIndexManager) register (ByPartIndexer. postTypeHandle, "content"))
    ;(. @database runMaintenance)

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

              (HGQuery$hg/addUnique @database (HGRel. (into-array HGHandle [userHandle threadHandle])) userThreadRelType (HGQuery$hg/link (into-array HGHandle [userHandle threadHandle])))
              (. @database add (HGValueLink. false (into-array HGHandle [userHandle postHandle])))
              (. @database add (HGValueLink. (. (. post getCreateTime) getMillis) (into-array HGHandle [threadHandle postHandle])))

              (def saveTime (+ saveTime (- (System/currentTimeMillis) saveStart)))
              )

            )
          )

        (let [saveStart (System/currentTimeMillis)]
          (def threadHandles (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))
          (def threadFirstPostLinks (map (fn [threadHandle] (reduce (fn [currentFirstThreadPostLink threadPostLink] (if (< (. (. @database get currentFirstThreadPostLink) getValue) (. (. @database get threadPostLink) getValue)) currentFirstThreadPostLink threadPostLink)) (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Long) (HGQuery$hg/incident threadHandle)])))) ) threadHandles))
          (def threadFirstPosts (map (fn [threadFirstPostLink] (HGQuery$hg/findOne @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Post) (HGQuery$hg/target threadFirstPostLink)])))) threadFirstPostLinks))
          (def postUserLinks (map (fn [threadFirstPost] (HGQuery$hg/getOne @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Boolean) (HGQuery$hg/incident threadFirstPost)])))) threadFirstPosts))
          (doseq [postUserLink postUserLinks]
            (. postUserLink setValue true)
            (. @database update postUserLink)
          )
          (def saveTime (+ saveTime (- (System/currentTimeMillis) saveStart)))
        )

        (def stop (System/currentTimeMillis))
        (println (string/join " " ["Data parsed and saved in" (String/valueOf (/ (- stop start) 1000.0)) "seconds"]))
        (println (string/join " " ["Saving took" (String/valueOf (/ saveTime 1000.0)) "seconds"]))
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
        (def threadHandles (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))
        (def threadFirstPosts (map (fn [threadHandle] (reduce (fn [currentFirstThreadPostLink threadPostLink] (if (< (. currentFirstThreadPostLink getValue) (. threadPostLink getValue)) currentFirstThreadPostLink threadPostLink)) (HGQuery$hg/getAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Long) (HGQuery$hg/incident threadHandle)])))) ) threadHandles))
        (def yearBegin (. (t/date-time 2013) getMillis))
        (def yearEnd (. (t/date-time 2014) getMillis))
        (println (count (filter (fn [threadFirstPost] (and (>= (. threadFirstPost getValue) yearBegin) (< (. threadFirstPost getValue) yearEnd))) threadFirstPosts)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "najbardziej popularny temat w maju 2013")
        (def dateBegin (. (t/date-time 2013 5) getMillis))
        (def dateEnd (. (t/date-time 2013 6) getMillis))
        (def threadHandles (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))
        (def threadPostsCounts (map (fn [threadHandle] (count (filter (fn [date] (and (>= date dateBegin) (< date dateEnd))) (map (fn [threadPostLink] (. threadPostLink getValue)) (HGQuery$hg/getAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Long) (HGQuery$hg/incident threadHandle)]))))))) threadHandles))
        (def mostPopularThread (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector threadHandles threadPostsCounts)))
        (println (. (. @database get (first mostPopularThread)) getTitle) (last mostPopularThread))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "średnia długość tekstu posta")
        (def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        (println (quot (reduce (fn [count post] (+ (. (. post getContent) length) count)) 0 posts) (count posts)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "użytkownik wypowiadający się w największej liczbie tematów")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/type User)))
        (def uniqueThreadsCount (map (fn [userHandle] (HGQuery$hg/count @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type userThreadRelType)])))) userHandles))
        ;(def uniqueThreadsCount (map (fn [user] (reduce (fn [count thread] (if (> (. (HGQuery$hg/findAll @database (HGQuery$hg/link (into-array HGHandle [user thread]))) size) 0) (+ count 1) count)) 0 (HGQuery$hg/findAll @database (HGQuery$hg/type ForumThread)))) userHandles))
        (def mostActiveUser (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector userHandles uniqueThreadsCount)))
        (println (. (. @database get (first mostActiveUser)) getLogin) (last mostActiveUser))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "użytkownik komentujący największą liczbę innych użytkowników")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/type User)))
        (def postsCount (map (fn [userHandle] (count (filter (fn [userPostLink] (= false (. userPostLink getValue))) (HGQuery$hg/getAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type Boolean)])))))) userHandles))
        (def mostActiveUser (reduce (fn [x y] (if (> (last x) (last y)) x y)) (map vector userHandles postsCount)))
        (println (. (. @database get (first mostActiveUser)) getLogin) (last mostActiveUser))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "liczba postów zawierających słowo 'Frodo'")
        (println (HGQuery$hg/count @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type Post) (AtomPartRegExPredicate. (into-array String ["content"]) (Pattern/compile ".*Frodo.*" Pattern/DOTALL))]))))
        ;(def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        ;(println (count (filter (fn [post] (if (re-find #"Frodo" (. post getContent)) true false)) posts)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "liczba postów wysłanych przez użytkowników z miasta na literę 'K'")
        (def userHandles (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/type User) (AtomPartRegExPredicate. (into-array String ["city"]) (Pattern/compile "^K.*" Pattern/MULTILINE))]))))
        ;(def userHandles (filter (fn [userHandle] (. (. (. @database get userHandle) getCity) startsWith "K")) (HGQuery$hg/findAll @database (HGQuery$hg/type User))))
        (println (apply + (map (fn [userHandle] (. (HGQuery$hg/findAll @database (HGQuery$hg/and (into-array HGQueryCondition [(HGQuery$hg/incident userHandle) (HGQuery$hg/type Boolean)]))) size)) userHandles)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

    (let [operationStart (System/currentTimeMillis)]
      (do
        (println "35te najczęściej użyte słowo w treści posta")
        (def posts (HGQuery$hg/getAll @database (HGQuery$hg/type Post)))
        (println (first (nth (sort-by val > (reduce (fn [wordMap post] (merge-with + wordMap (reduce (fn [postWordMap word] (assoc postWordMap word (+ 1 (get postWordMap word 0)))) {} (string/split (string/triml (string/replace (. post getContent) (Pattern/compile "\\W" Pattern/UNICODE_CHARACTER_CLASS) " ")) #"\s+")))) {} posts)) 34)))
        (println (string/join " " ["Operation took" (String/valueOf (/ (- (System/currentTimeMillis) operationStart) 1000.0)) "seconds"]))
        (println)
        )
      )

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
