package cnt4004.service.services;

import cnt4004.service.Service;
import com.github.terma.javaniotcpproxy.TcpProxy;
import com.github.terma.javaniotcpproxy.TcpProxyConfig;

import java.net.InetSocketAddress;

public class TCPProxyService implements Service {

    private TcpProxy tcpProxy;
    private TcpProxyConfig config;

    public TCPProxyService(InetSocketAddress bindAddress, String remoteHost, int remotePort) {
        int localPort = bindAddress.getPort();
        config = new TcpProxyConfig() {

            private int workers = 2;

            @Override
            public int getLocalPort() {
                return localPort;
            }

            @Override
            public int getRemotePort() {
                return remotePort;
            }

            @Override
            public String getRemoteHost() {
                return remoteHost;
            }

            @Override
            public int getWorkerCount() {
                return workers;
            }

            @Override
            public void setWorkerCount(int workerCount) {
                workers = workerCount;
            }
        };
    }

    @Override
    public void initialize() {
        if (tcpProxy != null)
            throw new IllegalStateException("Already initialized");

        tcpProxy = new TcpProxy(config);
    }

    @Override
    public void shutdown() {
        tcpProxy.shutdown();
        tcpProxy = null;
    }

    @Override
    public void open() {
        tcpProxy.start();
    }

    @Override
    public void close() {
        tcpProxy.shutdown();
    }

}
