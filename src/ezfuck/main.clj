(ns ezfuck.main
  (:require [ezfuck.interpreter :as i])

  (:gen-class))

(defn -main [& args]
  (i/repl))
