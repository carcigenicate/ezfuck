(ns ezfuck.state)

(ns brain-fuck.ezfuck.state
  (:require [clojure.string :as s]
            [helpers.general-helpers :as g]))

; TODO:
; - Have an operator that evaluates to the current cell value.
; ! Fix the wrapping behavior of move-left
;   - If move-left amount > than current size, the cell pointer becomes invalid
;   -

(def default-effect-magnitude 1)

(declare pprint-state)
(defrecord State [current-command instruction-pointer cell-pointer loop-anchors cells]
  Object
  (toString [self] (pprint-state self)))

(defn new-state []
  (->State
    nil
    0
    0
    []
    [0]))

; ----- Misc

(defn rev-drop-zeros [cells]
  (->> cells
       (reverse)
       (drop-while zero?)
       (reverse)
       (vec)))

(defn pprint-state [state]
  (let [limited-cells (update state :cells rev-drop-zeros)
        prettied-command (update state :current-command #(if (nil? %) \_ %))]
    (str "<" (s/join " " (vals prettied-command)) ">")))

(defn syntax-error [^String cause]
  (RuntimeException.
    (str "Syntax Error: " cause)))

(defn check-anchors-non-empty [state]
  (when (empty? (:loop-anchors state))
    (throw (syntax-error "Unmatched ]."))))

(defn pointer-inbounds? [cells ptr]
  (<= 0 ptr (dec (count cells))))

(defn can-shrink-cells? [cells new-ptr]
  (and (> (dec (count cells)) new-ptr)
       (every? zero?
               (subvec cells (inc new-ptr)))))

(defn- grow-cells-to [cells new-size]
  (into cells
        (repeat (- new-size (count cells)) 0)))

(defn shrink-cells-to [cells new-size]
  (vec (drop-last (- (count cells) new-size)
                  cells)))

(defn resize-cells-to [cells new-size]
  (let [size-diff (- new-size (count cells))]
    (cond
      (zero? size-diff) cells
      (pos? size-diff) (grow-cells-to cells new-size)
      :else (shrink-cells-to cells new-size))))

(defn grow-cells-if-nec [state new-ptr]
  (update state :cells
          #(if (pointer-inbounds? % new-ptr)
             %
             (grow-cells-to % (inc new-ptr)))))

(defn shrink-cells-if-nec [state new-ptr]
  (update state :cells
          #(if (can-shrink-cells? % new-ptr)
             (shrink-cells-to % (inc new-ptr))
             %)))

(defn default-mag [mag?]
  (or mag? default-effect-magnitude))

; ----- Instruction Pointer

(defn inc-instruction-pointer [state]
  (update state :instruction-pointer inc))

; ----- Pointer Left/Right < >

(defn move-pointer-left [state & [by?]]
  (let [by (default-mag by?)
        {cells :cells ptr :cell-pointer} state
        new-ptr (g/clamp (- ptr by) 0 (count cells))]
    (-> state
        (shrink-cells-if-nec new-ptr)
        (assoc :cell-pointer new-ptr))))

(defn move-pointer-right [state & [by?]]
  (let [by (default-mag by?)
        {cells :cells ptr :cell-pointer} state
        new-ptr (+ ptr by)]
    (-> state
        (grow-cells-if-nec new-ptr)
        (assoc :cell-pointer new-ptr))))

; ----- Current Cell Effectors + - / *

(defn- effect-current-cell [state f]
  (update-in state [:cells (:cell-pointer state)] f))

(defn- defaulting-effect-current-cell
  "Effects the current cell by applying f to the current cell value, and n? (apply f current vs?)
  vs? values default to default-effect-magnitude when nil."
  [state f & vs?]
  (let [vs (map default-mag vs?)]
    (effect-current-cell state #(apply f % vs))))

(defn add [state & [n?]]
  (defaulting-effect-current-cell state + n?))

(defn mult [state & [n?]]
  (defaulting-effect-current-cell state * n?))

(defn sub [state & [n?]]
  (defaulting-effect-current-cell state - n?))

(defn div [state & [n?]]
  (defaulting-effect-current-cell state / n?))

#_
(defn sqrt [state]
  (defaulting-effect-current-cell state #(int (Math/sqrt %))))

; ----- Looping [ ] { }(Moves the instruction pointer the indicated number of chunks)

; ----- Meta? ^(Evaluates to the value of the current cell)

; ----- IO , .


