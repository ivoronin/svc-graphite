(ns svc-graphite.core
  (:require [clojure.xml :as xml]
            [clj-time.coerce :as timec]
            [clj-time.format :as timef]
            [clj-graphite.client :as graphitec]
            [clojure.tools.cli :as cli])
  (:gen-class))

(def program-name "svc-graphite")
(def program-version "0.1.0")

(def num-cores
  "Returns number of CPU cores"
  (.. Runtime getRuntime availableProcessors))

(defn parse-timestamp
  "Parse timestamp and timezone information, returns epoch time in seconds"
  [document-attrs]
  (/ (timec/to-long
    (timef/parse
      (timef/formatter "yyyy-MM-dd HH:mm:ss zZZ")
        (str (:timestamp document-attrs) " "
          (clojure.string/replace
            (:timezone document-attrs) #"(?<=[-|+])(?!\d{2}:)" "0")))) 1000))

(defn feed-file
  "Parses IBM SVC iostat dump and sends data to graphite"
  [file graphite]
  (let [document (xml/parse file) document-attrs (:attrs document)
        content (:content document) timestamp (parse-timestamp document-attrs)
        cluster-name (:cluster document-attrs) node-name (:id document-attrs)]
    (doseq [element content]
      (let [group-name (name (:tag element)) element-attrs (:attrs element)]
        (doseq [[statistic value-string] element-attrs]
          ; Skip unneeded attributes
          (if-not (some #{statistic} [:id :cluster :node_id :cluster_id
                                      :type :type_id :wwpn :fc_wwpn
                                      :fcoe_wwpn :sas_wwn :iqn
                                      :idw :dis :cv :lsy])
           (let [statistic-name (name statistic)
                 value (Float/parseFloat value-string)]
              (.feed graphite
                  (format "svc.%s.%s.%s.%s" cluster-name node-name group-name
                    ; id can be nil for some elements
                    (if-let [object-name (:id element-attrs)]
                      (format "%s.%s" object-name statistic-name)
                      statistic-name))
                value timestamp))))))))

(defn feed-batch
  "Runs feed-file for each file in batch and reports errors if any"
  [files graphite]
  (doseq [file files]
    (try (feed-file file graphite)
      (catch Exception ex
        (println (format "Failed to process %s: %s" file (.getMessage ex)))))))

(defn feed-files
  "Runs feed-batch for every group of files in parallel"
  [files graphite]
  (let [num-files (count files)]
    (doall (pmap #(feed-batch % graphite)
      (partition-all (/ num-files num-cores) files)))))

(defn print-usage
  "Print usage banner and exit"
  [banner]
  (println (format "Usage: %s [OPTION]... FILE...\n%s" program-name banner))
  (System/exit 0))

(defn print-version
  "Print version and exit"
  []
  (println (format "%s %s" program-name program-version))
  (System/exit 0))

(defn errx
  "Prints error message and exits"
  [status message]
  (binding [*out* *err*]
    (println (format "%s: %s" program-name message)))
  (System/exit status))

(defn -main
  [& args]
  (let [[options args banner]
    (try (cli/cli args
      ["-H" "--host" "Graphite server hostname or address" :default "127.0.0.1"]
      ["-P" "--port" "Graphite server port" :default 2003 :parse-fn #(Long. %)]
      ["-h" "--help" "Show help" :default false :flag true]
      ["-V" "--version" "Show version" :default false :flag true])
    (catch Exception ex (errx 1 (.getMessage ex))))]
    (when (:help options) (print-usage banner))
    (when (:version options) (print-version))
    (when (empty? args) (errx 1 "missing file operand"))
    (let [graphite (graphitec/client {:host (:host options)
                                      :port (:port options)})]
      (feed-files args graphite))
    (shutdown-agents)))
