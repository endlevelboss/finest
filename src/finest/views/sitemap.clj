(ns finest.views.sitemap
  "The sitemap at /sitemap.xml: every public page's address, so search
   engines find pages few links point to. No <lastmod>: file times on
   the server say when content was deployed, not when it changed, and a
   wrong lastmod is worse than none."
  (:require [hiccup2.core :as h]))

(defn sitemap
  "`paths` are root-relative (\"/posts/x\"); `base-url` makes them absolute."
  [{:keys [base-url paths]}]
  (str
    (h/html {:mode :xml}
      (h/raw "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
      [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
       (for [path paths]
         [:url [:loc (str base-url path)]])])))
