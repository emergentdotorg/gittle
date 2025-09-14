package org.emergent.maven.gitver.core;

public class GitverException extends RuntimeException {

    public GitverException(String message) {
        super(message);
    }

    public GitverException(Throwable cause) {
        super(cause);
    }

    public GitverException(String message, Throwable cause) {
        super(message, cause);
    }
}
