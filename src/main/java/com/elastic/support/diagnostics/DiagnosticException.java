package com.elastic.support.diagnostics;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticException extends RuntimeException {

    List params;

    public List getParams() {
        return params;
    }

    public DiagnosticException() {
    }

    public DiagnosticException(String message) {
        super(message);
    }

    public DiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiagnosticException(Throwable cause) {
        super(cause);
    }

    public DiagnosticException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DiagnosticException(String message, List params){
        super(message);
        this.params = params;
    }

}
