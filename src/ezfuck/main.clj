(ns ezfuck.main
  (:require [ezfuck.interpreter :as i]
            [clojure.string :as string])

  (:import [java.io FileNotFoundException])

  (:gen-class))


(defn -main [& [^String mode? ^String path?]]
  (let [mode-lower (string/lower-case (or mode? ""))]

    (case (first mode-lower)
      \i (if path?
           (try
             (i/interpret (slurp path?))

             (catch FileNotFoundException e
               (println "Invalid path.")))

           (println "Enter a path to read from."))

      \r (i/repl)

      (do
        (println "To interpret a file, type: i <path>")
        (println "To start the REPL, type: r")))))
