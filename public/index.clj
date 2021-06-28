(ns index
  (:require [pcp :as pcp]
            [styles.main :as s]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [tick.alpha.api :as t]))

(def query (str "https://api.twitter.com/2/tweets/search/recent?query=clojure"
                "&expansions=author_id,attachments.media_keys"
                "&tweet.fields=author_id,created_at,possibly_sensitive,id,public_metrics,attachments"
                "&user.fields=id,name,url,profile_image_url,description"
                "&media.fields=duration_ms,height,media_key,preview_image_url,public_metrics,type,url,width"
                "&max_results=100"))

(def hn (str "http://hn.algolia.com/api/v1/search_by_date?query=clojure"
                "&hitsPerPage=100"
                "&page=1"
                "&numericFilters=created_at_i>" (- (pcp/now) 604800)
                "&tags=story"))

(defn quality? [tweet]
  (and 
    (< (count (re-seq #"#" (:text tweet))) 4) 
    (not (= "1405776728157425677" (:author_id tweet))) ; remove the book pirate
    (not (= "1406092047933526019" (:author_id tweet))) ; remove another book pirate
    (not (:possibly_sensitive tweet))
    (not (str/starts-with? (:text tweet) "RT @"))))

(defn hightlight [text]
  (-> (str " " text)
    (str/replace #"\n" "<br/>")
    (str/replace #"#(.*?)($|\s)" "<a class='mentions' href='https://twitter.com/hashtag/$1' target='_blank'>#$1</a> ")
    (str/replace #"@(.*?)($|\s)" "<a class='mentions' href='https://twitter.com/$1' target='_blank'>@$1</a> ")
    (str/replace #"(https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*))($|\s)" "<a class='mentions' href='$1' target='_blank'>$1</a> ")))

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
          [:title "PCP Demo website"]
          [:link {:rel "shortcut icon" :type "image/svg" :href "logo-alt.svg"}]
          [:meta {:charset "utf-8"}]
          s/styles]
        [:body 
          [:header 
            [:h1 "Clojure Pulse"]
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
                        [:a.username {:href (str "https://twitter.com/" (:username u))} (str "@" (:username u))]
                        "&nbsp;&nbsp;&middot;&nbsp;&nbsp;"
                        (let [time (t/instant (:created_at t))]
                          [:span.time (str (t/day-of-month time) " " (str/capitalize (t/month time)) " " (t/year time) " - " (t/time time))])]
                      [:p.text 
                        (hightlight (:text t))]
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
