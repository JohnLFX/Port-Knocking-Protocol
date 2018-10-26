package cnt4004.webserver;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

public class WebServer extends NanoHTTPD {

    public WebServer(String hostname, int port) {
        super(hostname, port);
    }

    public static void main(String[] args) throws IOException {

        WebServer server = new WebServer("localhost", 8080);
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        System.out.println("Web server is running!");

    }

    public void openTask() throws IOException {
        this.start();
    }

    public void closeTask() {
        this.stop();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }

}
