package cnt4004.service.services;

import cnt4004.service.Service;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

public class EmbeddedWebService extends NanoHTTPD implements Service {

    public EmbeddedWebService(InetSocketAddress bindAddress) {
        super(bindAddress.getHostName(), bindAddress.getPort());
    }

    @Override
    public void initialize() {

    }

    @Override
    public void shutdown() {
        close();
    }

    @Override
    public void open() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public Response serve(IHTTPSession session) {

        switch (session.getUri()) {

            case "/test1.html":
                return newFixedLengthResponse(Response.Status.OK, MIME_HTML,
                        "<html><body><h1>This is Test ONE</h1></body></html>");
            case "/test2.html":
                return newFixedLengthResponse(Response.Status.OK, MIME_HTML,
                        "<html><body><h1>This is Test TWO</h1></body></html>");
            case "/test3.html":
                return newFixedLengthResponse(Response.Status.OK, MIME_HTML,
                        "<html><body><h1>This is Test THREE</h1></body></html>");
            case "/testData.dat":
                return newChunkedResponse(Response.Status.OK, "application/octet-stream",
                        new InputStream() {
                            /* Infinite random bytes generator */
                            @Override
                            public int read() {
                                return ThreadLocalRandom.current().nextInt(256);
                            }
                        });
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML,
                        "<html><body><h1>404 Not Found</h1></body></html>");

        }

    }

}
