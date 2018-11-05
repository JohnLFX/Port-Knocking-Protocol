package cnt4004.webserver;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class WebServerInstance {

    private static WebServer WEB_SERVER;

    private WebServerInstance() {
    }

    public static WebServer getWebServer() {

        if (WEB_SERVER == null) {

            WebServer server = new WebServer("localhost", 8080);
            try {
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Web server is running!");

        }

        return WEB_SERVER;

    }

}
