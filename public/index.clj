(ns index
  (:require [pcp :as pcp]
            [styles.main :as s]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [tick.alpha.api :as t]
            [konserve-jdbc.core :refer [new-jdbc-store]]
            [konserve.core :as k]
            [clojure.core.async :as async :refer [<!!]]))

;; (pcp/reset) ;clear the application context

(defonce query (str "https://api.twitter.com/2/tweets/search/recent?query=clojure"
                "&expansions=author_id,attachments.media_keys"
                "&tweet.fields=author_id,created_at,possibly_sensitive,id,public_metrics,attachments,entities"
                "&user.fields=id,name,url,profile_image_url,description"
                "&media.fields=duration_ms,height,media_key,preview_image_url,public_metrics,type,url,width"
                "&max_results=100"))

(def hn (str "http://hn.algolia.com/api/v1/search_by_date?query=clojure"
                "&hitsPerPage=100"
                "&page=1"
                "&numericFilters=created_at_i>" (- (pcp/now) 604800)
                "&tags=story"))

(defonce pg {:dbtype "postgresql"
             :dbname (pcp/secret "POSTGRES_DB")
             :host (pcp/secret "POSTGRES_HOST")
             :user (pcp/secret "POSTGRES_USER")
             :password (pcp/secret "POSTGRES_PASSWORD")}) ; use defonce to persist between calls on this document root

(defonce store (<!! (new-jdbc-store pg :table "konserve")))
(defonce visits (atom (or (<!! (k/get-in store [:visits])) 0)))

(defn log-visit []
  (future 
    (let [v (or (<!! (k/get-in store [:visits])) 0)]
      (if (zero? v)
        (k/assoc-in store [:visits] v)
        (k/update-in store [:visits] inc))
      (swap! visits (fn [_] (inc v))))))

(log-visit)

(defn quality? [tweet]
  (and 
    (< (count (re-seq #"#" (:text tweet))) 4) 
    (not (= "1405776728157425677" (:author_id tweet))) ; remove the book pirate
    (not (= "1406092047933526019" (:author_id tweet))) ; remove another book pirate
    (not (:possibly_sensitive tweet))
    (not (str/starts-with? (:text tweet) "RT @"))))

(defn replace-several [content replacements]
      (let [replacement-list (partition 2 replacements)]
        (reduce #(apply str/replace %1 %2) content replacement-list)))

(defn make-link [url text]
  (str "<a class='mentions' href='" url "' target='_blank'>" text "</a> "))

(defn hightlight [t]
  (-> (str " " (:text t))
      (str/replace #"#(\S*?)($| |\n)" "<a class='mentions' href='https://twitter.com/hashtag/$1' target='_blank'>#$1</a> ")
      (str/replace #"@(\S*?)($| |\n)" "<a class='mentions' href='https://twitter.com/$1' target='_blank'>@$1</a> ")
      (str/replace #"\n" "<br/>")
      (replace-several (->> t :entities :urls (map (fn [ob] [(str (:url ob)) (make-link (:url ob) (:expanded_url ob))])) flatten vec))))

(defn resp []
  (let [tweets' (-> @(http/get query {:headers {"Authorization" (str "Bearer " (pcp/secret "TWITTER_BEARER"))}}) :body (json/decode true))
        tweets (filter quality? (map #(assoc % :kind "tweet") (:data tweets')))
        users (->> tweets' :includes :users)
        media (->> tweets' :includes :media)
        news' (-> @(http/get hn) :body (json/decode true))
        news (map #(assoc % :kind "story") (:hits news'))
        all (concat (vec news) (vec tweets))]
    (pcp/render-html-unescaped 
      [:html {:style "font-family: 'Source Sans Pro', Arial, Helvetica, sans-serif;text-align: center;"
              :lang "en"}
        [:head 
          [:title "Clojure Pulse - PCP demo site"]
          [:link {:rel "shortcut icon" :type "image/svg" :href "logo-alt.svg"}]
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
          [:meta {:name "description" :content "The latest in Clojure across Twitter and Hacker News. Built with PCP."}]
          [:meta {:name "keywords" :content "clojure, pcp, php, web development, lisp"}]
          [:meta {:name "author" :content "alekcz"}]
          [:meta {:name "twitter:site" :content "https://clojure-pulse.musketeers.io"}]
          [:meta {:name "twitter:card" :content "summary"}]
          [:meta {:name "twitter:title" :content "Clojure Pulse - PCP demo site"}]
          [:meta {:name "twitter:description" :content "The latest in Clojure across Twitter and Hacker News. Built with PCP."}]
          [:meta {:name "twitter:creator" :content "@alekcz"}]
          [:meta {:name "twitter:image" :content "https://clojure-pulse.musketeers.io/twittercard.png"}]
          [:meta {:property "og:title" :content "Clojure Pulse - PCP demo site"}]
          [:meta {:property "og:type" :content "website"}]
          [:meta {:property "og:url" :content "https://clojure-pulse.musketeers.io"}]
          [:meta {:property "og:image" :content "https://clojure-pulse.musketeers.io/social.png"}]
          [:meta {:property "og:description" :content "The latest in Clojure across Twitter and Hacker News. Built with PCP."}]
          [:meta {:property "og:site_name" :content "Clojure Pulse - PCP demo site"}]
          s/styles]
        [:body 
          [:header 
            [:h1 "Clojure Pulse"]
            [:small.overlap (str "Viewed " @visits " times")]
            [:p "The latest in Clojure across Twitter and Hacker News."]
            [:p.close-shave "Other places you can find Clojure on the web:"
              [:ul.close-shave 
                [:li [:a.accent {:target "_blank" :href "https://clojurians.slack.com/"} "Clojurian Slack"]]
                [:li [:a.accent {:target "_blank" :href "https://www.reddit.com/r/Clojure/"} "r/Clojure"]]
                [:li [:a.accent {:target "_blank" :href "https://clojure.org/news/news"} "Clojure Deref"]]
                [:li [:a.accent {:target "_blank" :href "https://clojureverse.org/"} "Clojureverse"]]
                [:li [:a.accent {:target "_blank" :href "https://www.libhunt.com/l/clojure"} "Clojure LibHunt"]]]]
            [:br]]
          [:main
            [:h3 "News from Clojure"]
            (for [t (reverse (sort-by :created_at all))]
              (if (= (:kind t) "story")
                [:div.hacker-news 
                  [:div.grow
                    [:p.text 
                      [:strong (str (:title t) "   ")]
                      [:br]
                      [:span.time "points " (:points t)]
                      "&nbsp;&nbsp;&middot;&nbsp;&nbsp;"
                      [:a.username {:href (str "https://news.ycombinator.com/user?id=" (:author t))} (:author t)]
                      "&nbsp;&nbsp;&middot;&nbsp;&nbsp;"
                      (let [time (t/instant (t/instant (t/new-duration (:created_at_i t) :seconds)))]
                        [:span.time (str (t/day-of-month time) " " (str/capitalize (t/month time)) " " (t/year time) " - " (t/time time))])]
                    [:p.text 
                      (:story_text t)]]
                  (when (:url t)
                    [:a.hn {:href (-> t :url str) :target "_blank"} 
                      [:div.view "Go to HN \u2192"]])]
                (let [u (->> (filter #(= (:id %) (:author_id t)) users) first) 
                      m (->> (filter #(= (:media_key %) (-> t :attachments :media_keys first)) media) first)]
                  [:div.tweet 
                    [:img.profile {:src (-> u :profile_image_url (str/replace "_normal" "_bigger")) :width "80px" :height "80px"}]
                    [:div.grow
                      [:p.text 
                        [:strong (str (:name u) "   ")]
                        [:a.username {:target "_blank" :href (str "https://twitter.com/" (:username u))} (str "@" (:username u))]
                        "&nbsp;&nbsp;&middot;&nbsp;&nbsp;"
                        (let [time (t/instant (:created_at t))]
                          [:span.time (str (t/day-of-month time) " " (str/capitalize (t/month time)) " " (t/year time) " - " (t/time time))])]
                      [:p.text 
                        (hightlight t)]
                      (when (= (:type m) "photo") 
                        [:br]
                        [:br]
                        [:img {:src (:url m) :width "80%"}])]
                    [:a.go {:href (str "https://twitter.com/" (:username u) "/status/" (:id t)) :target "_blank"} 
                      [:div.view "Go to tweet \u2192"]]])))
            [:br][:br][:br]]
          [:aside
             [:h3 "About PCP"]
             [:p "Too long have we hustled to deploy clojure websites. Too long have we spun up one instance per site. Too long have we reminisced about PHP."]
             [:p "Today we enjoy the benefits of both. Welcome to PCP."]
             [:p "Learn more about PCP:" [:br] [:a.accent {:href "https://github.com/alekcz/pcp"} "https://github.com/alekcz/pcp"]]
             [:p "or view the source for this site:" [:br] [:a.accent {:href "https://github.com/alekcz/pcp-demo"} "https://github.com/alekcz/pcp-demo"]]
             [:div.grow]
             [:div.credits
              [:small.mild-shave "This site is built with:"]
              [:img {:src "/logo-alt.svg" :width "40px"}]
              [:small.mild-shave "PCP: Clojure Processor"]]]]])))

(pcp/response 200 (resp) "text/html")            
