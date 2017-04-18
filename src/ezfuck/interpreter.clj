(ns ezfuck.interpreter
  (:require [ezfuck.state :as st]
            [ezfuck.language-symbols :as sy]))

(defn filter-code [^String code]
  (filter #(or (sy/symbol-map %)
               (Character/isDigit ^Character %))
          code))

(defn- chunk-code [^String code])


(defn interpret [^String code]
  ())