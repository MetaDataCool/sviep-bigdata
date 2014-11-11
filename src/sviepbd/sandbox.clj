(ns sviepbd.sandbox "Namespace with all libraries to experiement"
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            )
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Element Document]
           [org.jsoup.select Elements]
           )
  (:use clojure.repl clojure.pprint)
  )

(comment 
  (def woh-id "299390749413") ;; a fb page id, find it from  
  (def my-token "734266833319223|yiHW63aGEayDOoJqus2EVD8kaaw")
  (->
    @(http/get 
       (str "https://graph.facebook.com/" woh-id "/feed")
       {:oauth-token my-token})
    :body
    (json/decode true)
    :data)
  
  ;; sample FB posts
  ({:description "The International Tap House",
    :caption "internationaltaphouse.com",
    :status_type "shared_story",
    :name "International Tap House - Chesterfield",
    :privacy {:value ""},
    :created_time "2014-11-07T17:40:53+0000",
    :type "link",
    :updated_time "2014-11-07T17:40:53+0000",
    :icon
    "https://fbstatic-a.akamaihd.net/rsrc.php/v2/yD/r/aS8ecmYRys0.gif",
    :likes
    {:data
     [{:id "10202603310756790", :name "Kelly Warner Loos"}
      {:id "882556565089017", :name "Danielle Flauaus"}
      {:id "806424936089772", :name "Sokhary Kong Pavese"}],
     :paging
     {:cursors
      {:after "ODA2NDI0OTM2MDg5Nzcy",
       :before "MTAyMDI2MDMzMTA3NTY3OTA="}}},
    :from
    {:category "Non-profit organization",
     :category_list [{:id "2603", :name "Non-Profit Organization"}],
     :name "Wings of Hope",
     :id "299390749413"},
     :link
     "http://internationaltaphouse.com/chesterfield/entertainment.php",
     :id "299390749413_10152844924669414",
     :picture
     "https://fbexternal-a.akamaihd.net/safe_image.php?d=AQCeFv2o5Q9k
JrO1&w=158&h=158&url=http%3A%2F%2Finternationaltaphouse.com%2Fchesterfield%2Fimages%2Fhoursofoperationheader.gif",
     :message
     "On November 20th, International Tap House in Chesterfield is pouring in support of the Medical Relief and Air Transport (MAT) Program through their \"Keg for a Cause\". ITAP is donating all of the money earned from the selling of the featured beer from 5:00-8:00 pm to the MAT Program. It's also \"Keep the Glass Night\", where you can take your beer glass home. Please join us for this great cause! See you there! \nhttp://internationaltaphouse.com/chesterfield/entertainment.php"}
    {:object_id "10152841755324414",
     :status_type "mobile_status_update",
     :privacy {:value ""},
     :created_time "2014-11-05T23:06:31+0000",
     :type "photo",
     :updated_time "2014-11-05T23:06:31+0000",
     :icon
     "https://fbstatic-a.akamaihd.net/rsrc.php/v2/yz/r/StEh3RhPvjk.gif",
     :likes
     {:data
      [{:id "151695398202436", :name "Lightspeed Aviation Foundation"}
       {:id "10152593155778500", :name "Ricardo G
onzalez M."}
       {:id "10152871121809185", :name "Wm Daniel File"}
       {:id "882556565089017", :name "Danielle Flauaus"}
       {:id "10205724751344835", :name "Melanie Halley"}
       {:id "10152863220490681", :name "Rebecca Rabbitt Pogorzelski"}
       {:id "796603860396576", :name "Jan Longshore"}
       {:id "10203152479357376", :name "Tina Noland"}
       {:id "10201834067653012", :name "Laura Martin"}
       {:id "10202818039915899", :name "Amanda Marie Karas"}
       {:id "795589387143731", :name "Cecilia Waggoner"}
       {:id "10204404259922373", :name "Amy Brockhurst"}
       {:id "10204101796858768", :name "Larry Kamberg"}
       {:id "10204521924960172", :name "Heather Prokopf"}
       {:id "10100399388549203", :name "Jessica Rodgers"}
       {:id "1513778468908778", :name "Carol Brown Enright"}
       {:id "10202896612552544", :name "Cheryl S Button"}
       {:id "10204350975606904", :name "Sally Wood Phillips"}
       {:id "10204377757395429", :name "Timothy Carman"}
       {:id "10203259528951547", :name "Jennifer Shelley"}
       {:id "1020391
7665796274", :name "Godoy Carlina"}
       {:id "736130353128901", :name "Rick Patton"}
       {:id "10100580974878961", :name "Emily Zimmermann"}
       {:id "10203253487760056", :name "Barrett Williams"}
       {:id "10152787673857920", :name "Becky King Niemiec"}],
      :paging
      {:cursors
       {:after "MTAxNTI3ODc2NzM4NTc5MjA=",
        :before "MTUxNjk1Mzk4MjAyNDM2"},
       :next
       "https://graph.facebook.com/v2.2/299390749413_10152841757849414/likes?limit=25&after=MTAxNTI3ODc2NzM4NTc5MjA%3D"}},
     :from
     {:category "Non-profit organization",
      :category_list [{:id "2603", :name "Non-Profit Organization"}],
      :name "Wings of Hope",
      :id "299390749413"},
     :link
     "https://www.facebook.com/wingsofhopeinc/photos/pcb.10152841757849414/10152841755324414/?type=1&relevant_count=3",
     :id "299390749413_10152841757849414",
     :picture
     "https://fbcdn-sphotos-e-a.akamaihd.net/hphotos-ak-xpf1/v/t1.0-9/s130x130/10253764_10152841755324414_7071461477275786055_n.jpg?oh=a46df0cedd6bb71fde487e30e18f07a6&oe=54EC7A50&__gda__=142497082
2_c0cad02b8021d156abd3f86f6a866c29",
     :message
     "On Saturday October 11th, the hangar was hopping at the \"On the Road to Wings of Hope\", a celebration honoring our many supporters and donors of Wings of Hope. The event was held at the world headquarters in Chesterfield, and guests dined on global culinary treats, danced the night away to the music of Gateway City Big Band, and enjoyed traditional performances by Nepalese dancers. If you missed it this year, we hope you'll join us next year!\n\nAll Photos by Blacktie-Missouri"}
    {:object_id "10152841121629414",
     :status_type "added_photos",
     :privacy {:value ""},
     :comments
     {:data
      [{:id "10152841121629414_10152841164409414",
        :from {:id "10203025761988588", :name "Chuck Midyett"},
        :message
        "Please do help the M.A.T. operation.  I am a volunteer pilot and the service provided to our \"clients\" is life saving.",
        :can_remove false,
        :created_time "2014-11-05T16:48:38+0000",
        :like_count 1,
        :user_likes false}],
      :paging

      {:cursors
       {:after
        "WTI5dGJXVnVkRjlqZFhKemIzSTZNVEF4TlRJNE5ERXhOalEwTURrME1UUTZNVFF4TlRJd05qRXhPRG94",
        :before
        "WTI5dGJXVnVkRjlqZFhKemIzSTZNVEF4TlRJNE5ERXhOalEwTURrME1UUTZNVFF4TlRJd05qRXhPRG94"}}},
     :created_time "2014-11-05T16:20:49+0000",
     :type "photo",
     :updated_time "2014-11-05T16:48:38+0000",
     :icon
     "https://fbstatic-a.akamaihd.net/rsrc.php/v2/yz/r/StEh3RhPvjk.gif",
     :likes
     {:data
      [{:id "10152871121809185", :name "Wm Daniel File"}
       {:id "10202992353440788", :name "Tom Hutson"}
       {:id "10202896612552544", :name "Cheryl S Button"}
       {:id "983540168339618", :name "Clint Hanley"}
       {:id "10205062064615089", :name "Karen Goering"}
       {:id "806424936089772", :name "Sokhary Kong Pavese"}
       {:id "816926681679673", :name "Rubens Alvarez Benalcazar"}
       {:id "10203025761988588", :name "Chuck Midyett"}
       {:id "1513778468908778", :name "Carol Brown Enright"}
       {:id "882556565089017", :name "Danielle Flauaus"}
       {:id "756646864372863", :name "Dan Hicks"}],
      :paging

      {:cursors
       {:after "NzU2NjQ2ODY0MzcyODYz",
        :before "MTAxNTI4NzExMjE4MDkxODU="}}},
     :from
     {:category "Non-profit organization",
      :category_list [{:id "2603", :name "Non-Profit Organization"}],
      :name "Wings of Hope",
      :id "299390749413"},
     :link
     "https://www.facebook.com/wingsofhopeinc/photos/a.447134074413.232775.299390749413/10152841121629414/?type=1&relevant_count=1",
     :id "299390749413_10152841122604414",
     :picture
     "https://scontent-a.xx.fbcdn.net/hphotos-xpa1/v/t1.0-9/s130x130/10268576_10152841121629414_4176173694800455371_n.png?oh=020ee8a0b94085f4771cc44706989350&oe=54DEC057",
     :message
     "This week we would like to put our CFK Sponsor Spotlight on Porlier Outdoor Advertising. http://www.porlier.biz. \n\nChampions for Kids help fund the Wings of Hope St. Louis regional Medical Relief and Air Transport (MAT) Program, which connects seriously ill and disabled individuals (85% of them children) - with no access to advanced medical care - to the state-of-the-art treatments they n
eed. All of our MAT Program services are free of charge and provided as long as they are needed.\n\nIf you have an interest in your business being one of our Champions for Kids, please contact us!"})
  
  )