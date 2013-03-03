(ns concurrent.prod-con)

(defn iterator-fn->lazy-seq
  ;; TODO docs
  [f]
  {:pre  [(fn? f)]
   :post [(seq? %)]}
  (lazy-seq
    (let [next-item (f)]
      (if (nil? next-item)
        '()
        (cons next-item (iterator-fn->lazy-seq f))))))

(defn work-queue
  ;; TODO docs
  ([] (work-queue 0))
  ([size]
    (if (= 0 size)
      (java.util.concurrent.LinkedBlockingQueue.)
      (java.util.concurrent.ArrayBlockingQueue. size))))

(def work-complete-sentinel (Object.))

(defn work-queue->seq
  ;; TODO docs
  [queue]
  (iterator-fn->lazy-seq
    (fn []
      (let [next-item (.take queue)]
        (if (= work-complete-sentinel next-item)
          (do
            ;; TODO: doc
            (.put queue work-complete-sentinel)
            nil)
          next-item)))))

;(defn work-seq
;  ([work-fn]
;    (println "Attempting to get work seq for work-fn")
;    (work-seq work-fn (work-fn)))
;  ([work-fn next-work]
;    (println "Attempting to get work seq for work-fn with next-work:" next-work)
;    (if-not (nil? next-work)
;      (cons next-work (lazy-seq work-seq work-fn))
;      '())))
;
(defn producer
  ([work-fn num-threads]
    (producer work-fn num-threads 0))
  ([work-fn num-threads max-work]
    (let [queue    (work-queue max-work)
          workers (doall
                    (for [i (range num-threads)]
                      (do
                        (println "Creating producer future" i)
                        (future
                          (let [work-count (atom 0)]
                            (doseq [work (iterator-fn->lazy-seq work-fn)]
                              (.put queue work)
                              (swap! work-count inc))
                            (.put queue work-complete-sentinel)
                            @work-count)))))]
      (println "Done creating producer workers")
      {:queued-work (work-queue->seq queue)
       :workers     workers})))
;
;(defn consumer
;  [producer work-fn num-threads max-results]
;  (let [result-queue (work-queue max-results)
;        workers      (for [_ (range num-threads)]
;                        (future (doseq [work (:queued-work producer)]
;                                  (.put result-queue (work-fn work)))))]
;    (work-queue-seq result-queue)))
;
;(def next-work-id (atom 1))
;
;(defn create-work
;  []
;  (println "create-work called")
;  (Thread/sleep 200)
;  (println "create-work: back from sleep")
;  (let [_    (println "current work id" @next-work-id)
;        work (swap! next-work-id inc)
;        _    (println "got next  work id" work)]
;    (println "create-work about to print thread info")
;    (println "current thread:" (Thread/currentThread))
;    (println "current thread id:" (.getId (Thread/currentThread)))
;    (println "Produced work"  work " on thread " (.getId (Thread/currentThread)))
;    work))
;
;(defn do-work
;  [work]
;  (println "Doing work:" work "  on thread " (.getId (Thread/currentThread))))
;
;
;(defn tryit
;  []
;  (let [prod    (producer create-work 5 20)
;        _       (println "Created producer")
;;        results (consumer prod do-work 10 20)
;        _       (println "Created consumer")]
;    (println "Main driver about to start trying to retrieve results")
;;    (doseq [result results]
;;      (println "Main driver: got result: " result))))
;    (doseq [worker (:workers prod)]
;      (println "Waiting for worker to finish")
;      (println "Worker finished:" @worker))
;  )
;  (println "Done."))
