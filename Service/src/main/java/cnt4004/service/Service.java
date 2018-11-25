package cnt4004.service;

/**
 * Interface representing a service
 */
public interface Service {

    /**
     * Initializes the service
     */
    void initialize();

    /**
     * Shuts down the service.
     * A shut down indicates that the service should deallocate resources.
     */
    void shutdown();

    /**
     * Opens the service
     */
    void open();

    /**
     * Closes the service, but can still be opened without re-initializing
     */
    void close();

}
