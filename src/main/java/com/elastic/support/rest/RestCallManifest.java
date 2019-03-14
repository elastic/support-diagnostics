package com.elastic.support.rest;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RestCallManifest {

    private Map<String, CallHistory> callHistories = new LinkedHashMap();
    private int runs;

    public void setCallHistory(String name, int attempts, boolean succeeded){
        CallHistory ch = callHistories.get(name);
        if (ch == null){
            ch = new CallHistory();
        }
        ch.setAttempts(attempts);
        ch.setSucceeded(succeeded);
        callHistories.put(name, ch);
    }

    public CallHistory getCallHistory(String name){
        return callHistories.get(name);
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public int getTotalSuccesses(){
        int cnt = 0;
        Collection<CallHistory> calls = callHistories.values();
        for(CallHistory ch: calls){
            if(ch.isSucceeded()){
                cnt++;
            }
        }
        return cnt;
    }

    public int getTotalFailures(){
        int cnt = 0;
        Collection<CallHistory> calls = callHistories.values();
        for(CallHistory ch: calls){
            if(! ch.isSucceeded()){
                cnt++;
            }
        }
        return cnt;
    }

    public boolean getSuccess(String name){
        CallHistory ch = getCallHistory(name);
        if(ch != null){
            return ch.isSucceeded();
        }
        return false;
    }

    public int getAttempts(String name){
        CallHistory ch = getCallHistory(name);
        if(ch != null){
            return ch.getAttempts();
        }
        return -1;
    }

}
