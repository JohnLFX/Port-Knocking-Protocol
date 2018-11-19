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

        InetSocketAddress bindAddress = new InetSocketAddress(config.getProperty("service-bind"), Integer.parseInt(config.getProperty("service-port")));

        if (Boolean.valueOf(config.getProperty("use-embedded-webserver", "true"))) {

            this.serviceType = ServiceType.EMBEDDED_WEBSERVER;
            this.service = new EmbeddedWebService(bindAddress);

        } else {

            this.serviceType = ServiceType.TCP_PROXY;
            this.service = new TCPProxyService(bindAddress, config.getProperty("proxy-address"), Integer.parseInt(config.getProperty("proxy-port")));

        }

    }

    public void initializeService() {

        if (service != null)
            throw new IllegalStateException("Already initialized");

        LOGGER.info("Setting up " + serviceType.toString().toLowerCase() + " service");
        service.initialize();

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
