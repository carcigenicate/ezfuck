(ns ezfuck.interpreter
  (:require [ezfuck.state :as st]
            [ezfuck.language-symbols :as sy]
            [helpers.general-helpers :as g]))

(def standard-state (st/new-state))

(defn digit? [^Character chr]
  (Character/isDigit chr))

(defn valid-symbol? [^Character sym]
  (or (sy/symbol-map sym)
      (digit? sym)))

(defn filter-code [^String code]
  (filter valid-symbol? code))

(defn resolve-sym
  "Expects either a sequence of digits representing a number, or a character representing a command."
  [sym]
  (if (sy/symbol-map sym)
    (sy/symbol-map sym)
    (Integer/parseInt (apply str sym))))


(defn- chunk-symbols [filtered-code-symbols]
  (loop [[f-sym :as syms] filtered-code-symbols
         acc []]
    (if f-sym
      (let [dig? (digit? f-sym)
            sym (if dig?
                  (take-while digit? syms)
                  (first syms))

            resolved-sym (resolve-sym sym)

            rest-syms (if dig?
                        (drop-while digit? syms)
                        (drop 1 syms))]

        (recur rest-syms (conj acc resolved-sym)))

      acc)))

(defn group-data-commands [code-chunks]
  (let [dropped-starting-number (if (number? (first code-chunks))
                                    (drop 1 code-chunks)
                                    code-chunks)]
    (loop [[comm & rest-chunks] dropped-starting-number
           acc []]

      (if comm
        (let [[args rest-chunks'] (split-with number? rest-chunks)]
          (recur rest-chunks' (conj acc [comm args])))
        acc))))

(defn process-code [^String code]
  (-> code
      (filter-code)
      (chunk-symbols)
      (group-data-commands)))

(defn interpret [^String code]
  (let [chunks (process-code code)]
    (st/apply-chunks standard-state chunks)))

(defn repl []
  (loop [state standard-state]

    (print ":>")
    (flush)

    (when-let [input (read-line)]
      (case input
        "stop" nil

        "clear" (recur (st/new-state))

        (let [comm-chunks (process-code input)
              next-state (st/apply-chunks state comm-chunks)]

          (println (str next-state))

          (recur
            (assoc next-state :instruction-pointer 0)))))))