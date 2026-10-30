(ns finest.views.components-test
  (:require [clojure.test :refer [deftest is]]
            [finest.views.components :as c]))

(deftest review-of-text-names-each-line-once
  (is (= "Crisis on Infinite Earths: Part One, Part Two; Superman: The Man of Steel; Watchmen"
         (c/review-of-text
          {:review-of [{:slug "coie-1" :title "Part One" :line "events" :line-title "Crisis on Infinite Earths"}
                       {:slug "mos" :title "The Man of Steel" :line "superman" :line-title "Superman"}
                       {:slug "coie-2" :title "Part Two" :line "events" :line-title "Crisis on Infinite Earths"}
                       {:slug "watchmen" :title "Watchmen"}]}))))

(deftest post-card-title-link-is-not-draggable
  (let [title-link (->> (c/post-card {:slug "x" :title "X" :type :news :tags #{}})
                        (tree-seq sequential? seq)
                        (some #(when (and (vector? %) (= :h2 (first %))) (second %))))]
    (is (= {:href "/posts/x" :draggable "false"} (second title-link)))))
