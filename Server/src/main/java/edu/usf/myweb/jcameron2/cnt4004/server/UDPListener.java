package edu.usf.myweb.jcameron2.cnt4004.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPListener implements Runnable {

    private final DatagramSocket datagramSocket;
    private final UDPKnockCallback callback;

    public UDPListener(DatagramSocket datagramSocket, UDPKnockCallback callback) {
        this.datagramSocket = datagramSocket;
        this.callback = callback;
    }

    @Override
    public void run() {

        byte[] data = new byte[64];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);

        while (datagramSocket.isBound()) {

            try {

                datagramSocket.receive(receivePacket);
                callback.onKnock();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
