package cnt4004.service.services;

import cnt4004.service.Service;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

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
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, List<String>> parms = session.getParameters();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }

}
