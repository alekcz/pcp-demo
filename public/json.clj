(require '[pcp :as pcp]
         '[cheshire.core :as json])

(pcp/response 200 (json/encode {:message "pew pew"}) "text/plain")   