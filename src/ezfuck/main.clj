(ns ezfuck.main
  (:require [ezfuck.interpreter :as i]
            [clojure.string :as string])

  (:gen-class))

(defn -main [^String mode & [^String path?]]
  (let [mode-lower (string/lower-case mode)]

    (case (first mode-lower)
      \i (let [code (if path? (slurp path?) nil)]
           (if path?
             (i/interpret code)
             (println "Enter a path to read from.")))

      \r (i/repl)

      (do
        (println "To interpret a file, type: i <path>")
        (println "To start the REPL, type: r")))))
