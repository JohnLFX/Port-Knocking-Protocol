package cnt4004.service;

import cnt4004.service.services.EmbeddedWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class ServiceManager {

    private static ServiceManager INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);
    private final ServiceType serviceType;
    private Service service;

    private ServiceManager() {

        Properties config = new Properties();

        try {

            Path configFile = Paths.get("service.properties");

            if (Files.notExists(configFile)) {

                try (InputStream in = ServiceManager.class.getResourceAsStream("/service.properties")) {
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

            this.serviceType = ServiceType.EMBEDDED_WEBSERVER;

        } else {

            this.serviceType = ServiceType.PROXY;

        }

    }

    public void initializeService() {

        switch (serviceType) {

            case EMBEDDED_WEBSERVER:
                service = new EmbeddedWebService();
                break;
            default:
                service = null;
                break;
        }

        if (service != null) {

            LOGGER.info("Setting up " + serviceType.toString().toLowerCase() + " service");
            service.initialize();

        } else {

            LOGGER.warn("No service implementation found for  service type " + serviceType);

        }

    }

    public void shutdownService() {

        if (service != null) {

            LOGGER.info("Shutting down " + serviceType.toString().toLowerCase() + " service");
            service.shutdown();

        }

    }

    public void openService() {
        if (service != null)
            service.open();
    }

    public void closeService() {
        if (service != null)
            service.close();
    }

    public static ServiceManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ServiceManager();

        return INSTANCE;
    }

}
