package cnt4004.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class Service {

    private static Service INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    private Service() {

        Properties config = new Properties();

        try {

            Path configFile = Paths.get("service.properties");

            if (Files.notExists(configFile)) {

                try (InputStream in = Service.class.getResourceAsStream("/service.properties")) {
                    LOGGER.info("Creating new configuration file: " + configFile);
                    Files.copy(in, configFile);
                }

            }

            try (InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ)) {
                LOGGER.info("Loading service settings from " + configFile);
                config.load(in);
            }

        } catch (IOException e) {

            LOGGER.error("Failed to read service configuration", e);

        }

        if (Boolean.valueOf(config.getProperty("use-embedded-webserver", "true"))) {

            LOGGER.debug("Using the embedded web server");

        } else {


        }

    }

    public void initializeService() {

    }

    public void shutdownService() {

    }

    public void openService() {

    }

    public void closeService() {

    }

    public static Service getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Service();

        return INSTANCE;
    }

}
