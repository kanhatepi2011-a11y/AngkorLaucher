package com.movtery.angkorlauncher.launch;

public class LaunchPreparationException extends IllegalStateException {
    public LaunchPreparationException(String message) {
        super(message);
    }

    public LaunchPreparationException(String message, Throwable cause) {
        super(message, cause);
    }
}
