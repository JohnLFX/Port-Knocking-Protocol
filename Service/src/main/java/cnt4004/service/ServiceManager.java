package cnt4004.service;

import cnt4004.service.services.EmbeddedWebService;
import cnt4004.service.services.TCPProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * The service manager is a singleton instance used for managing a single service
 */
public class ServiceManager {

    private static ServiceManager INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);
    private final ServiceType serviceType;
    private Service service;

    private ServiceManager() {

        // Load stuff from the configuration

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

        InetSocketAddress bindAddress = new InetSocketAddress(config.getProperty("service-bind"), Integer.parseInt(config.getProperty("service-port")));

        // Determine what type of service to run
        if (Boolean.valueOf(config.getProperty("use-embedded-webserver", "true"))) {

            this.serviceType = ServiceType.EMBEDDED_WEBSERVER;
            this.service = new EmbeddedWebService(bindAddress);

        } else {

            this.serviceType = ServiceType.TCP_PROXY;
            this.service = new TCPProxyService(bindAddress, config.getProperty("proxy-address"), Integer.parseInt(config.getProperty("proxy-port")));

        }

    }

    /**
     * Initializes the service. See {@link Service#initialize()}
     */
    public void initializeService() {
        LOGGER.info("Setting up " + serviceType.toString().toLowerCase() + " service");
        service.initialize();
    }

    /**
     * Shuts down the service. See {@link Service#shutdown()}
     */
    public void shutdownService() {
        if (service != null) {
            LOGGER.info("Shutting down " + serviceType.toString().toLowerCase() + " service");
            service.shutdown();
        }
    }

    /**
     * Opens the service. See {@link Service#open()}
     */
    public void openService() {
        if (service != null)
            service.open();
    }

    /**
     * Closes the service. See {@link Service#close()}
     */
    public void closeService() {
        if (service != null)
            service.close();
    }

    /**
     * Returns a singleton instance of the service manager
     *
     * @return The instance
     */
    public static ServiceManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ServiceManager();

        return INSTANCE;
    }

}
