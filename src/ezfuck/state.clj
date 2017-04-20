(ns ezfuck.state
  (:require [clojure.string :as s]
            [helpers.general-helpers :as g])

  (:refer-clojure :exclude [chunk]))

; TODO:
; - Have an operator that evaluates to the current cell value.
; - Have a Reader class that handles the instruction-pointer?

(def default-effect-magnitude 1)

(def insertion-marker ::insert)

(def standard-args-n 1)

(declare pprint-state)
(defrecord State [instruction-pointer cell-pointer loop-anchors cells]
  Object
  (toString [self] (pprint-state self)))

(defn new-state []
  (->State
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
  (let [limited-cells (update state :cells rev-drop-zeros)]
    (str "<" (s/join " " (vals limited-cells)) ">")))

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

; ----- Instruction Pointer

(defn effect-instruction-pointer [state f]
  (update state :instruction-pointer
          #(g/clamp (f %) -1 Long/MAX_VALUE)))

(defn inc-instruction-pointer [state]
  (effect-instruction-pointer state inc))

; ----- Pointer Left/Right < >

(defn clamp-non-neg [ptr]
  (if (neg? ptr) 0 ptr))

(defn move-pointer-left [state by]
  (let [{ptr :cell-pointer} state
        new-ptr (clamp-non-neg (- ptr by))]

    (-> state
        (shrink-cells-if-nec new-ptr)
        (assoc :cell-pointer new-ptr))))

(defn move-pointer-right [state by]
  (let [{ptr :cell-pointer} state
        new-ptr (clamp-non-neg (+ ptr by))]

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
  "Effects the current cell by applying f to the current cell value, and args (apply f current args)
  args values default to default-effect-magnitude when nil."
  [state f & args]
  (effect-current-cell state #(apply f % args)))

(defn add [state n]
  (defaulting-effect-current-cell state + n))

(defn mult [state n]
  (defaulting-effect-current-cell state * n))

(defn sub [state n]
  (defaulting-effect-current-cell state - n))

(defn div [state n]
  (defaulting-effect-current-cell state
                                  #(long (/ % %2))
                                  n))

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

(defn start-loop [state _]
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

(defn close-loop [state _]
  (if (current-cell-zero? state)
    (checked-loop-end state)
    (checked-loop-jump state)))

; ----- Jumps { }(Moves the instruction pointer the indicated number of chunks)
(defn jump-left [state by]
  (if (current-cell-zero? state)
    state
    (effect-instruction-pointer state #(- % by))))

(defn jump-right [state by]
  (if (current-cell-zero? state)
    state
    (effect-instruction-pointer state #(+ % by))))

; ----- Insertion / Extraction Operator ^(Evaluates to the value of the current cell)

(defn insertion-operator? [arg]
  (= arg ::insert))

(defn insert [state]
  (throw (UnsupportedOperationException.
           "Insert hasn't been implememted yet.")))

(defn extract [state & [n?]]
  (if n?
    (effect-current-cell state
                         (constantly n?))
    state))

; ----- Argument Processing

(defn- substitute-if-insertion [sub-value arg]
  (if (insertion-operator? arg)
    sub-value
    arg))

(defn substitute-args [args sub-value]
  (map #(substitute-if-insertion sub-value %) args))

(defn standardize-args [args n-args]
  (let [defaulted-args (mapv #(or % default-effect-magnitude) args)
        n-needed (- n-args (count args))]
    (cond
      (pos? n-needed)
      (into (vec args)
            (repeat n-needed default-effect-magnitude))

      (neg? n-needed)
      (subvec defaulted-args 0 n-args)

      :else
      args)))

(defn wrap-command [command n-args]
  (fn [state & args]
    (let [subd-args (substitute-args args (current-cell-value state))
          standardized-args (standardize-args subd-args n-args)]

      (apply command state standardized-args))))

; ----- Run

(defn- valid-chunk? [chunk]
  (let [[comm args?] chunk]
    (and (fn? comm)
         (or (nil? args?) (seq? args?)))))

(defn- verify-chunk [chunk]
  (when-not (valid-chunk? chunk)
    (throw (RuntimeException. (str "Invalid Chunk: " chunk)))))

(defn- apply-chunk-to-state [state chunk]
  (verify-chunk chunk)

  (let [[comm args] chunk
        wrapped-command (wrap-command comm standard-args-n)]

    (apply wrapped-command state args)))

(defn apply-chunk [state chunk] ; Chunk name?
  (-> state
      (apply-chunk-to-state chunk)
      (inc-instruction-pointer)))

(defn apply-chunks
  "Applies the chunks to the state.
  At the end of the chunks, it applies the remaining command; if the last chunk was a command."
  [state chunks]
  (let [chunks-v (vec chunks)]
    (loop [state' state]
      (let [ptr (:instruction-pointer state')
            chunk (get chunks-v ptr nil)]

        (if chunk
          (recur (apply-chunk state' chunk))
          state')))))
