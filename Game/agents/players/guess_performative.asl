@guess_performative
+!guess_performative(speech(Speaker, Msg, Performative))
    :   setting(max_llm_retry(MaxRetries))
    <-  if (ui(_)) {
            .abolish(last_correct_performative(_));
            +last_correct_performative(Performative);
        };
        
        bert.guess_performative(Msg);
        .wait(performative_result(Response));
        -performative_result(_);

        //no need to check its correctness

        !submit_performative_guess(speech(Speaker, Msg, Response)).
