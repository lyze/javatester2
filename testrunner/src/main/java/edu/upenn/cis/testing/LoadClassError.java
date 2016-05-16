package edu.upenn.cis.testing;

/**
 * @author davix
 */
public class LoadClassError extends Error {
    public LoadClassError() {
    }

    public LoadClassError(String message) {
        super(message);
    }

    public LoadClassError(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadClassError(Throwable cause) {
        super(cause);
    }

    public LoadClassError(String message, Throwable cause, boolean
            enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
