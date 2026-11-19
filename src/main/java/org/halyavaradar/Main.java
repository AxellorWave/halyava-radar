package org.halyavaradar;

import org.halyavaradar.config.ApplicationFactory;

public class Main {
    public static void main(String[] args) {
        int exitCode;
        try (ApplicationFactory factory = new ApplicationFactory()) {
            exitCode = factory.createCommandLine().execute(args);
        }
        System.exit(exitCode);
    }
}