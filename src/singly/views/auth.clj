(ns singly.views.auth
  (:require [singly.views.common :as common]
            [noir.content.getting-started]
            [noir.session :as session]
            [oauthentic.core :as oa]
            [clj-http.client :as http]
            [noir.request :as req])
  (:use [noir.core :only [defpage]])
)

(defn server-base-url []
  (let [r (req/ring-request)]
                (str (name (:scheme r)) "://" (:server-name r) ":" (:server-port r) )
  )
 )

(def client-id
  (get (System/getenv) "SINGLY_CLIENT_ID")  
)

(def client-secret
  (get (System/getenv) "SINGLY_CLIENT_SECRET")
)

(defn redirect-uri []
  (str (server-base-url) "/oauth/endpoint")
)

(defn authentication-url [service]
  "Construct a Singly Authentication url for a given service"
  (oa/build-authorization-url { :authorization-url "https://api.singly.com/oauth/authenticate"
                                :client-id client-id
                              }
                              { :redirect-uri (redirect-uri)
                                :service service
                              }
  )
 )


(defn singly-access-request [code]
  "Request for singly authentication"
   (http/post "https://api.singly.com/oauth/access_token" 
             {:as :json
              :throw-entire-message? true
              :form-params {:code code
                             :client_id client-id
                             :client_secret client-secret
                             :profile "all"
                            }
             }
             
   ) 
          
)

(defn store-singly-access! [code]
  "Create a HTTP request to singly with the code"
  (if
    (= nil (session/get :access-token))
     (let [response (singly-access-request code)]
       (session/put! :access-token (:access_token (:body response)))
       (session/put! :profile      (:profile      (:body response)))    
     "updated session")
    "using session cached values"
  )
 )


(defpage [:get "/oauth/endpoint"] {:keys [code error]}
  (if (= error nil)
    (let [ session-status (store-singly-access! code)        
         access-token (session/get :access-token)
         profile      (session/get :profile)
       ]
         (common/layout
           [:p "Welcome to Oauth Endpoint"]
           [:code code]
           [:p "Made request to Singly"]
           [:p (str "Your access token is " access-token)]
           [:p "Authenticated to Singly"]     
           [:h1 "Profile :"]
           [:pre (str (session/get :profile))]
           [:h2 "Request :"]
           [:pre   (let [r (req/ring-request)]
                (str r ))]               
           [:a {:href "/auth"} "AUTH HOME"]
          )
     ) 
     (common/layout
           [:p "Oauth Endpoint encountered error"]
           [:pre error]
     )
  )                                   
 )

(defpage "/auth" []
      (common/layout
           [:p "Authenticate with Singly"]
           [:ul        
             [:li [:a {:href (authentication-url "github") }   "Connect with github"]]           
             [:li [:a {:href (authentication-url "facebook") } "Connect with facebook"]]
             [:li [:a {:href (authentication-url "twitter") }  "Connect with twitter"]]
           ]
      )                
 )