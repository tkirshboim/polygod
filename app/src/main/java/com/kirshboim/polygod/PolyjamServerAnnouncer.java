package com.kirshboim.polygod;

import com.google.inject.Singleton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class PolyjamServerAnnouncer {

    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private DatagramSocket socket;
    private boolean stop = false;

    public void start(final InetAddress broadcastAddress, final Logger logger) throws SocketException {
        socket = new DatagramSocket(4443);
        socket.setBroadcast(true);

        stop = false;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                String data = "hi";
                DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                        broadcastAddress, 4444);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    logger.log("failed to send broadcast - " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!stop) {
                    executor.submit(this);
                }
            }
        });

    }

    public void stop() {
        stop = true;
        if (socket != null) {
            socket.close();
        }
    }
}
