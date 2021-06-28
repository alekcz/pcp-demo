(ns index
  (:require [pcp :as pcp]
            [garden.core :refer [css]]
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
    (pcp/html 
      [:html {:style "font-family: 'Source Sans Pro', Arial, Helvetica, sans-serif;text-align: center;"
              :lang "en"}
        [:head 
          [:title "PCP Demo website"]
          [:link {:rel "shortcut icon" :type "image/svg" :href "logo-alt.svg"}]
          [:meta {:charset "utf-8"}]
          [:style 
            (css [:* {:box-sizing "border-box"}])
            (css [:html :body {:margin "0px" :padding "0px" :text-align "left"}])
            (css [:p.info { :background-color "#EEE" 
                            :font-size "13px"
                            :margin-top "40px"
                            :padding "10px" 
                            :width "300px"}])
            (css [:body   { :display "flex"
                            :flex-direction "row"
                            :align-item "stretch"
                            :min-height "98vh"}])
            (css [:code   { :font-size "12px" 
                            :font-weight "normal"}])
            (css [:header { :min-width "20vw"
                            :background-color "#F8F8F8" 
                            :box-shadow "0px 0px 10px rgb(0 0 0 / 15%)" 
                            :display "flex"
                            :flex-direction "column"
                            :padding "10px"
                            :position "fixed"
                            :top 0
                            :bottom 0
                            :left 0}])
            (css [:main   { :flex-grow 8
                            :padding "30px"
                            :max-width "60vw"
                            :margin "0 20vw"}])                          
            (css [:aside  { :min-width "20vw"
                            :background-color "#F8F8F8" 
                            :box-shadow "0px 0px 10px rgb(0 0 0 / 15%)" 
                            :display "flex"
                            :flex-direction "column"
                            :position "fixed"
                            :top 0
                            :bottom 0
                            :right 0}])
            (css [:div.tweet {:display "flex" 
                              :position "relative"
                              :flex-direction "row"
                              :justify-content "flex-start" 
                              :align-items "stretch" 
                              :margin "20px" 
                              :box-shadow "0px 0px 12px rgb(0 0 0 / 20%)" 
                              :padding "20px 20px 40px 20px"
                              :border-radius "15px"}])
            (css [:div.hacker-news 
                             {:display "flex" 
                              :position "relative"
                              :flex-direction "row"
                              :justify-content "flex-start" 
                              :align-items "stretch" 
                              :margin "20px" 
                              :box-shadow "0px 0px 12px rgb(0 0 0 / 20%)" 
                              :padding "20px"
                              :border-radius "15px"}]) 
            (css [:img.profile {:margin-right "20px" :border-radius "100%"}])
            (css [:p.text {:text-align "left" :margin "0 0 10px 0"}])
            (css [:a.mentions {:color "rgb(27, 149, 224)" :text-decoration "none"}])
            (css [:div.grow {:flex-grow "99"}])
            (css [:a.go {:background-color "rgb(27, 149, 224)" :color "#FFF" :position "absolute" :bottom "0px" :right "0px" :text-decoration "none" :border-top-left-radius "15px" :border-bottom-right-radius "15px"}])
            (css [:a.hn {:background-color "rgb(255, 102, 0)" :color "#FFF" :position "absolute" :bottom "0px" :right "0px" :text-decoration "none" :border-top-left-radius "15px" :border-bottom-right-radius "15px"}])
            (css [:a.username {:color "rgb(83, 100, 113)" :text-decoration "none"}])
            (css [:a.user {:color "rgb(83, 100, 113)" :text-decoration "none" :font-size "14px"}])
            (css [:span.time {:color "rgb(83, 100, 113)" :font-size "14px"}])
            (css [:div.view {:display "flex" :justify-content "center" :align-item "center" :padding "8px" }])
            (css [:img.view {:height "20px" :width "20px" :margin-left "10px"}])]]
        [:body 
          [:header [:img {:src "/logo-alt.svg" :width "40px"}]]
          [:main
            (for [t (reverse (sort-by :created_at all))]
              (if (= (:kind t) "story")
                (let []
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
                        [:div.view "Go to HN \u2192"]])])
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
                        [:img {:src (:url m) :width "100%"}])]
                    [:a.go {:href (str "https://twitter.com/" (:username u) "/status/" (:id t)) :target "_blank"} 
                      [:div.view "Go to tweet \u2192"]]])))
            [:br][:br][:br]]
          [:aside]]])))

(pcp/response 200 (resp) "text/html")            
