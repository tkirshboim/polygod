package com.kirshboim.polygod;

import com.google.inject.Singleton;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.kirshboim.polygod.model.Player;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class Server implements RequestHandler {

    private static final int PLAYER_TIMEOUT_SEC = 300;

    public enum State {STOPPED, STARTING, STARTED, STOPPING, FAILED;}

    private static final int LOG_MAX_LINES = 0xff;

    private static final DateFormat logFormat = new SimpleDateFormat("hh:mm:ss ");

    public static final Server instance = new Server();

    private ExecutorService cleanupExecutor;

    private List<String> log = new LinkedList<>();
    private PolyjamHttpServer httpServer = new PolyjamHttpServer(8080, this);

    private PolyjamServerAnnouncer announcer = new PolyjamServerAnnouncer();
    private Logger logger;

    private ServerStateUpdateCallback callback;
    private String serverIp = null;

    private State state = State.STOPPED;
    private LinkedHashMap<String, Player> queue = new LinkedHashMap<>();

    public void setLogger(Logger logger, ServerStateUpdateCallback callback) {
        this.callback = callback;
        this.logger = logger;
    }

    public void setServerIp(String serverIp) {
        if (serverIp != null && serverIp.equals(this.serverIp) == false) {
            this.serverIp = serverIp;
            log("server ip set to " + serverIp);
        }
    }

    public void start(InetAddress broadcastAddress) {
        if (broadcastAddress == null) {
            log("broadcast address null");
            setState(State.FAILED);
            return;
        }

        if (serverIp == null) {
            log("server ip null");
            setState(State.FAILED);
            return;
        }

        setState(State.STARTING);

        try {
            httpServer.start(serverIp);
            announcer.start(broadcastAddress, logger);
            runCleanupTask();

        } catch (IOException e) {
            httpServer.stop();
            announcer.stop();
            e.printStackTrace();
            log(e.toString());
        }

        setState(State.STARTED);
    }

    public void stop() {
        setState(State.STOPPING);
        announcer.stop();
        httpServer.stop();
        setState(State.STOPPED);
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }

    private void runCleanupTask() {
        cleanupExecutor = Executors.newFixedThreadPool(1);
        cleanupExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<String> toRemove = new LinkedList<>();
                for (String ip : queue.keySet()) {
                    if (now() - queue.get(ip).seenAt > PLAYER_TIMEOUT_SEC * 1000) {
                        toRemove.add(ip);
                    }
                }

                synchronized (queue) {
                    for (String ip : toRemove) {
                        queue.remove(ip);
                    }
                }

                if (toRemove.isEmpty() == false) {
                    callback.updateQueue();
                }

                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!cleanupExecutor.isShutdown()) {
                    cleanupExecutor.submit(this);
                }
            }
        });
    }

    private void setState(State state) {
        this.state = state;
        callback.updateState();
        log(state.toString());
    }

    public void assign(String ip, int slot) {
        for (Player player : queue.values()) {
            if (player.ip.equals(ip)) {
                player.slot = slot;

            } else if (slot != -1 && player.slot == slot) {
                player.slot = -1;
            }
        }

        callback.updateQueue();
    }

    private void log(String message) {
        message = logFormat.format(new Date()) + message + '\n';

        logger.log(message);
        log.add(message);
        if (log.size() == LOG_MAX_LINES) {
            log.remove(0);
        }
    }

    public String getLog() {
        StringBuilder sb = new StringBuilder();
        for (String line : log) {
            sb.append(line);
        }
        return sb.toString();
    }

    public State getState() {
        return state;
    }

    public List<Player> getQueue() {
        return new LinkedList<>(queue.values());
    }

    @Override
    public boolean register(String remoteIp, String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        Player player = queue.get(remoteIp);
        if (player == null) {
            player = new Player(remoteIp, name);

            synchronized (queue) {
                queue.put(remoteIp, player);
            }

            callback.updateQueue();

        } else if (player.name != name) {
            // new registration for the same ip - put to end of queue
            player.setName(name);

            synchronized (queue) {
                queue.remove(remoteIp);
                queue.put(remoteIp, player);
            }

            callback.updateQueue();
        }

        player.seen();

        return true;
    }

    @Override
    public int status(String remoteIp) {
        Player player = queue.get(remoteIp);

        if (player == null) {
            return -2;
        }

        player.seen();
        return player.slot;
    }

    public void sendNoteOffToAllPorts() {
        log("sending note off to all ports");
        allFingersUp();

    }

    public void allFingersUp() {
        if (serverIp == null) {
            return;
        }

        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(serverIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < 4; i++) {
            int port = PolyjamHttpServer.BASE_PLAY_PORT + i;
            try {
                OSCPortOut portOut = new OSCPortOut(serverAddress, port);
                for (int j = 0; j < 4; j++) {
                    pointer(portOut, j, -1f, -1f, -1f, -1f, -1f, -1f);
                }
                portOut.close();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public void pointer(OSCPortOut portOut, int pointerId, float x, float y, float pressure,
                        float azimuth, float pitch, float roll) {

        List<Object> args = new ArrayList<Object>(7);
        args.add(pointerId);
        args.add(x);
        args.add(y);
        args.add(pressure);
        args.add(azimuth);
        args.add(pitch);
        args.add(roll);

        try {
            portOut.send(new OSCMessage("/finger", args));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
