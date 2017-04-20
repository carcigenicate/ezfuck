(defproject ezfuck "1"
  :description "A implementation of the exfuck language."

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ; Available at my GitHub
                 [helpers "1"]]

  :main ezfuck.main

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
