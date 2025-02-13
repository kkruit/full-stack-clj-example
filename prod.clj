(require
  '[clojure.java.io :as io]
  '[clojure.string :as str]
  '[cljs.build.api :as api]
  '[leiningen.core.project :as p :refer [defproject]]
  '[leiningen.uberjar :refer [uberjar]])

(defn read-project-clj []
  (p/ensure-dynamic-classloader)
  (-> "project.clj" load-file var-get))

(defn read-deps-edn [aliases-to-include]
  (let [{:keys [paths deps aliases]} (-> "deps.edn" slurp clojure.edn/read-string)
        deps (->> (select-keys aliases aliases-to-include)
                  vals
                  (mapcat :extra-deps)
                  (into deps)
                  (map (fn parse-coord [coord]
                         (let [[artifact info] coord
                               s (str artifact)]
                           (if-let [i (str/index-of s "$")]
                             [(symbol (subs s 0 i))
                              (assoc info :classifier (subs s (inc i)))]
                             coord))))
                  (reduce
                    (fn [deps [artifact info]]
                      (if-let [version (:mvn/version info)]
                        (conj deps
                          (transduce cat conj [artifact version]
                            (select-keys info [:exclusions :classifier])))
                        deps))
                    []))
        paths (->> (select-keys aliases aliases-to-include)
                   vals
                   (mapcat :extra-paths)
                   (into paths))]
    {:dependencies deps
     :source-paths []
     :resource-paths paths}))

(defn delete-children-recursively! [f]
  (when (.isDirectory f)
    (doseq [f2 (.listFiles f)]
      (delete-children-recursively! f2)))
  (when (.exists f) (io/delete-file f)))

(def project (-> (read-project-clj)
                 (merge (read-deps-edn []))
                 p/init-project))

(def out-file "resources/public/main.js")
(def out-dir "resources/public/main.out")

(println "Building" out-file)
(delete-children-recursively! (io/file out-dir))
(api/build "src" {:main          'full-stack-clj-docker.core
                  :optimizations :advanced
                  :output-to     out-file
                  :output-dir    out-dir})
(delete-children-recursively! (io/file out-dir))

(println "Building uberjar")
(uberjar project)
(System/exit 0)
