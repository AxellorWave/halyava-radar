package org.halyavaradar;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.config.ApplicationFactory;

@Slf4j
public class Main {
    public static void main(String[] args) {
        int exitCode;
        try (ApplicationFactory factory = new ApplicationFactory()) {
            exitCode = factory.createCommandLine().execute(args);
        } catch (RuntimeException e) {
            log.error("Не удалось запустить или завершить приложение", e);
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
