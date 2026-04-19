//---------------
// CACHING LOGS
//---------------

//when a log is cached or invalidated, print debug info if the setting is enabled
+cached_log(Phase, K, CroppedLog) : setting(debug_log(true)) <- .print("[CACHE INFO] CACHED log locally for phase ", Phase, " with K=", K).
-cached_log(Phase, K, CroppedLog) : setting(debug_log(true)) <- .print("[CACHE INFO] REMOVED local cached log for phase ", Phase, " with K=", K).

//cache hit
+!fetch_log_block(Phase, K, CroppedLog , cache_if_possible) 
    :   cached_log(Phase, K, CroppedLog)
    <-  if (setting(debug_log(true))) {
            .print("[CACHE INFO] USING local cached log for phase ", Phase, " with K=", K);
        }.

//cache miss, pending request
+!fetch_log_block(Phase, K, CroppedLog, cache_if_possible) 
    :   not cached_log(Phase, K, _) 
    &   pending_log_request(Phase, K)
    <-  if (setting(debug_log(true))) {
            .print("[CACHE INFO] WAITING for pending log fetch for phase ", Phase);
        }
        //wait until the requests is fulfilled
        .wait(cached_log(Phase, K, _)); 
        //actually obtain the cached log
        ?cached_log(Phase, K, CroppedLog). 

//cahce miss, no pending request
+!fetch_log_block(Phase, K, CroppedLog, cache_if_possible) 
    :   not cached_log(Phase, K, _) 
    &   not pending_log_request(Phase, K)
    <-  //lock the request to avoid multiple requests
        +pending_log_request(Phase, K);

        //call the overloaded version (simple retrieval, no caching)
        !fetch_log_block(Phase, K, CroppedLog);

        //cache the  result for future use
        +cached_log(Phase, K, CroppedLog);

        //releas the lock
        -pending_log_request(Phase, K).

//fallback: narrator failed to respond, timed out, or threw an error
-!fetch_log_block(Phase, K, [] , cache_if_possible)
    <-  if (setting(debug_log(true))) {
            .print("[ERROR] Narrator failed to respond to log query (caching mode).");
        }
        //release the log nonethelss
        -pending_log_request(Phase, K);
        //remove the cache, for safety
        -cached_log(Phase, K, _).


//---------------
// NON-CACHING LOG RETRIEVAL
//---------------

//simply check in the narrator's belief base
+!fetch_log_block(Phase, K, CroppedLog) 
    :   .my_name(Me)
    <-  .send(narrator, askOne, get_log_chunk(Phase, [Me], K, _), Reply); //askOne to get the belief
        Reply = get_log_chunk(_, _, _, CroppedLog)[source(_)].


//fallback: narrator failed to respond, timed out, or threw an error
-!fetch_log_block(Phase, K, []) 
    <-  if (setting(debug_log(true))) {
            .print("[ERROR] Narrator failed to respond to log query (non-caching mode).");
        }.