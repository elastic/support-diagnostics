package com.elastic.support.diagnostics;

import com.elastic.support.rest.RestExec;

import java.util.Map;

public class GlobalState {
    private static RestExec restExec = new RestExec();
    private static Inputs inputs;
    private static Map config;

    public static RestExec getRestExec() {
        return restExec;
    }

    public static void setRestExec(RestExec restExec) {
        GlobalState.restExec = restExec;
    }

    public static Inputs getInputs() {
        return inputs;
    }

    public static void setInputs(Inputs inputs) {
        GlobalState.inputs = inputs;
    }

    public static Map getConfig() {
        return config;
    }

    public static void setConfig(Map config) {
        GlobalState.config = config;
    }
}
