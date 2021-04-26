package com.elastic.support.diagnostics;

public class DiagnosticException extends Exception {
    public DiagnosticException() {
        super();
    }

    public DiagnosticException(String message) {
        super(message);
    }

    public DiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }
}