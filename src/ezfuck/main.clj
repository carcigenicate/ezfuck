(ns ezfuck.main
  (:require [ezfuck.interpreter :as i]))

(defn -main [& args]
  (i/repl))
