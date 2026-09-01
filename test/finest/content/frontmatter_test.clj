(ns finest.content.frontmatter-test
  (:require [clojure.test :refer [deftest is testing]]
            [finest.content.frontmatter :as frontmatter]))

(deftest parse-valid-frontmatter
  (let [raw "---\ntitle: Foo\ntags: [a, b]\n---\nBody text\nwith --- a dash line\n"
        {:keys [meta body]} (frontmatter/parse raw)]
    (is (= "Foo" (:title meta)))
    (is (= ["a" "b"] (:tags meta)))
    (is (= "Body text\nwith --- a dash line\n" body))))

(deftest parse-missing-frontmatter
  (testing "throws on missing frontmatter delimiters"
    (is (thrown? clojure.lang.ExceptionInfo
                 (frontmatter/parse "no frontmatter here")))))

(deftest parse-malformed-frontmatter
  (testing "throws when opening delimiter present but no closing delimiter"
    (is (thrown? clojure.lang.ExceptionInfo
                 (frontmatter/parse "---\ntitle: Foo\nbody with no closing delimiter")))))
