(defproject forumdb "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.hypergraphdb/hgdb "1.2"]
                 [org.hypergraphdb/hgbdbje "1.2"]
                 [com.sleepycat/je "5.0.73"]
                 [clj-time "0.7.0" :exclusions [org.clojure/clojure]]
                ]
  :main ^:skip-aot forumdb.core
  :target-path "target/%s"
  :jvm-opts ["-Dfile.encoding=utf-8"]
  :profiles {:uberjar {:aot :all}}
  :java-source-paths ["src/forumdb/java"]
)
