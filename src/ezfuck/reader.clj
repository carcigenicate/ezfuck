(ns ezfuck.reader
  (:require [ezfuck.state :as s]
            [helpers.general-helpers :as g]))

(defrecord Execution-state [program-state instruction-pointer commands])

; TODO: Where do [] and {} go? Need to know if they touch the instruction pointer or not.
; Always manipulate the exec-state? Wrap s/ commands in (update e-state :p-state command)?
(def symbol-map {})

(defn new-exec-state [commands]
  (->Execution-state (s/new-state)
                     0
                     commands))

; ----- Instruction Pointer

(defn effect-instruction-pointer [state f]
  (update state :instruction-pointer
          #(g/clamp (f %) 0 Long/MAX_VALUE)))

(defn inc-instruction-pointer [state]
  (effect-instruction-pointer state inc))

; ----- Looping [ ]

(defn- current-loop-anchor-index [state]
  (-> state
      (:loop-anchors)
      (last)))

(defn syntax-error [^String cause]
  (RuntimeException.
    (str "Syntax Error: " cause)))

(defn check-anchors-non-empty [state]
  (when (empty? (:loop-anchors state))
    (throw (syntax-error "Unmatched ]."))))

(defn start-loop [state]
  (update state :loop-anchors
          #(conj % (:instruction-pointer state))))

(defn- unchecked-loop-jump [state]
  (assoc state :instruction-pointer
               (current-loop-anchor-index state)))

(defn checked-loop-jump [state]
  (check-anchors-non-empty state)

  (unchecked-loop-jump state))

(defn- unchecked-loop-end [state]
  (update state :loop-anchors
          #(vec (drop-last %))))

(defn checked-loop-end [state]
  (check-anchors-non-empty state)

  (unchecked-loop-end state))

(defn close-loop [state]
  (let [{cp :cell-pointer cs :cells} state]
    (if (s/current-cell-zero? state)
      (checked-loop-end state)
      (checked-loop-jump state))))

; ----- Jumps { }(Moves the instruction pointer the indicated number of chunks)
(defn jump-left [state & [by?]]
  (let [by (s/default-mag by?)]
    (if (s/current-cell-zero? state)
      state
      (effect-instruction-pointer state #(- % by)))))

(defn jump-right [state & [by?]]
  (let [by (s/default-mag by?)]
    (if (s/current-cell-zero? state)
      state
      (effect-instruction-pointer state #(+ % by)))))

; -----