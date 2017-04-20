(ns ezfuck.language-symbols
  (:require [ezfuck.state :as s]))

(def symbol-map {\< s/move-pointer-left
                 \> s/move-pointer-right

                 \{ s/jump-left
                 \} s/jump-right

                 \[ s/start-loop
                 \] s/close-loop

                 \+ s/add
                 \- s/sub
                 \* s/mult
                 \/ s/div

                 \. s/output-cell-at-pointer
                 \, s/buffered-input-to-cell-at-pointer

                 \^ s/extract
                 \V s/insertion-marker})