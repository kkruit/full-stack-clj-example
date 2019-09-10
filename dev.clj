(require
  '[figwheel.main.api]
  '[nrepl.server]
  '[cider.piggieback]
  '[full-stack-clj-docker.core :refer [dev-main]])

(defn cljs-repl []
      (figwheel.main.api/cljs-repl "dev"))

(defn nrepl-handler []
      (require 'cider.nrepl)
      (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(dev-main)

(nrepl.server/start-server
  :port 7888
  :bind "0.0.0.0"
  :handler (nrepl-handler))

(figwheel.main.api/start "dev")

