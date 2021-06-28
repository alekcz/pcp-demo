(ns styles.main
  (:require [garden.core :refer [css]]))

(def styles 
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
                    :max-width "20vw"
                    :background-color "#F8F8F8" 
                    :box-shadow "0px 0px 10px rgb(0 0 0 / 15%)" 
                    :display "flex"
                    :flex-direction "column"
                    :padding "10px 20px"
                    :position "fixed"
                    :top 0
                    :bottom 0
                    :left 0}])
    (css [:main   { :flex-grow 8
                    :padding "30px"
                    :max-width "60vw"
                    :margin "0 20vw"}])                          
    (css [:aside  { :min-width "20vw"
                    :max-width "20vw"
                    :background-color "#F8F8F8" 
                    :box-shadow "0px 0px 10px rgb(0 0 0 / 15%)" 
                    :display "flex"
                    :padding "20px"
                    :flex-direction "column"
                    :justify-content "space-between"
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
                      :padding "20px"
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
    (css [:a.accent {:color "#00A89D" :text-decoration "none"}])
    (css [:div.grow {:flex-grow "99"}])
    (css [:.close-shave {:margin "0px"}])
    (css [:.mild-shave {:margin "6px 0"}])
    (css [:ul {:padding-inline-start "20px"}])
    (css [:a.go {:background-color "rgb(27, 149, 224)" :color "#FFF" :position "absolute" :bottom "0px" :right "0px" :text-decoration "none" :border-top-left-radius "15px" :border-bottom-right-radius "15px"}])
    (css [:a.hn {:background-color "rgb(255, 102, 0)" :color "#FFF" :position "absolute" :bottom "0px" :right "0px" :text-decoration "none" :border-top-left-radius "15px" :border-bottom-right-radius "15px"}])
    (css [:a.username {:color "rgb(83, 100, 113)" :text-decoration "none"}])
    (css [:a.user {:color "rgb(83, 100, 113)" :text-decoration "none" :font-size "14px"}])
    (css [:span.time {:color "rgb(83, 100, 113)" :font-size "14px"}])
    (css [:div.view {:display "flex" :justify-content "center" :align-item "center" :padding "8px" }])
    (css [:img.view {:height "20px" :width "20px" :margin-left "10px"}])
    (css [:.credits {:text-align "center" :display "flex" :flex-direction "column" :align-items "center" :justify-content "center"}])])