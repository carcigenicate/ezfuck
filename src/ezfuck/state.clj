(ns ezfuck.state)

(ns brain-fuck.ezfuck.state
  (:require [clojure.string :as s]
            [helpers.general-helpers :as g]))

; TODO:
; - Have an operator that evaluates to the current cell value.
; - Have a Reader class that handles the instruction-pointer?

(def default-effect-magnitude 1)

(declare pprint-state)
(defrecord State [last-command instruction-pointer cell-pointer loop-anchors cells]
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
        prettied-command (update state :last-command #(if (nil? %) \_ %))]
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

(defn effect-instruction-pointer [state f]
  (update state :instruction-pointer
          #(g/clamp (f %) 0 Long/MAX_VALUE)))

(defn inc-instruction-pointer [state]
  (effect-instruction-pointer state inc))

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

; ----- Current Cell Effectors / Readers + - / *

(defn- effect-current-cell [state f]
  (update-in state [:cells (:cell-pointer state)] f))

(defn- current-cell-value [state]
  (get-in state [:cells (:cell-pointer state)]))

(defn- current-cell-zero? [state]
    (zero? (current-cell-value state)))

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

; ----- IO , .

(defn output-cell-at-pointer
  "Assumes the current cell value is a valid character code."
  [state]
  (let [{cells :cells cp :cell-pointer} state
        raw-output (cells cp)
        output-char (char raw-output)]

    (print output-char)
    (flush)

    state))

(defn buffered-input-to-cell-at-pointer
  "Reads in a string, and sets the current cell value to the ASCII code of the first letter.
  Must enter a newline after the input."
  [state]
  (effect-current-cell state
                       (constantly
                         (-> (read-line)
                             (first)
                             (int)))))

; ----- Looping [ ]

(defn- current-loop-anchor-index [state]
  (-> state
      (:loop-anchors)
      (last)))

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
    (if (current-cell-zero? state)
      (checked-loop-end state)
      (checked-loop-jump state))))

; ----- Jumps { }(Moves the instruction pointer the indicated number of chunks)
(defn jump-left [state & [by?]]
  (let [by (default-mag by?)]
    (if (current-cell-zero? state)
      state
      (effect-instruction-pointer state #(- % by)))))

(defn jump-right [state & [by?]]
  (let [by (default-mag by?)]
    (if (current-cell-zero? state)
      state
      (effect-instruction-pointer state #(+ % by)))))

; ----- Insertion / Extraction Operator ^(Evaluates to the value of the current cell)
; ------ Not yet implemented

(defn insert [state]
  state)

(defn extract [state]
  state)

; ----- Run

(defn- valid-chunk? [chunk]
  (or (fn? chunk) (number? chunk)))

(defn command? [chunk]
  (fn? chunk))

(defn value? [chunk]
  (number? chunk))

(defn- verify-chunk [chunk]
  (when-not (valid-chunk? chunk)
    (throw (RuntimeException. (str "Invalid Chunk: " chunk)))))

(defn- apply-last-command [state current-chunk]
  (let [{last-comm :last-command} state]
    (if last-comm
      (-> state
          (last-comm (when (value? current-chunk)))
          (assoc :last-command nil))
      state)))

; TODO: There's no lookahead to see what the next chunk is.
; Currently delays execution of each chunk until the type of the next is known
(defn apply-chunk-to-state [state chunk]
  (verify-chunk chunk)

  (let [{last-comm? :last-command} state
        last-comm-ran-state (apply-last-command state chunk)]

    (if (command? chunk)
      (assoc last-comm-ran-state :last-command chunk)

      last-comm-ran-state)))

(defn apply-to-state [state chunk] ; Chunk name?
  (-> state
      (apply-chunk-to-state chunk)
      (inc-instruction-pointer)))
