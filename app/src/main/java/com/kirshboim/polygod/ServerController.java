package com.kirshboim.polygod;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.kirshboim.polygod.model.Player;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerController extends Service {

    public interface Events {
        String EXTRA_KEY_STATE_ENUM = "STATE_ENUM";
        String EXTRA_KEY_LOG_MESSAGE = "MESSAGE";
        String EXTRA_KEY_QUEUE = "QUEUE";
        String EXTRA_KEY_IP = "IP";
        String EXTRA_KEY_SLOT = "SLOT";

        String SERVER_START = ServerController.class.getName() + "#START_SERVER";
        String SERVER_STOP = ServerController.class.getName() + "#STOP_SERVER";
        String SERVER_GET_STATE = ServerController.class.getName() + "#SERVER_GET_STATE";
        String SERVER_GET_LOG = ServerController.class.getName() + "#SERVER_GET_LOG";
        String SERVER_STATE = ServerController.class.getName() + "#SERVER_STATE";
        String SERVER_LOG_MESSAGE = ServerController.class.getName() + "#SERVER_LOG_MESSAGE";
        String SERVER_QUEUE = ServerController.class.getName() + "#SERVER_QUEUE";
        String SERVER_GET_QUEUE = ServerController.class.getName() + "#SERVER_GET_QUEUE";
        String ASSIGN_SLOT = ServerController.class.getName() + "#ASSIGN_SLOT";
        String PANIC = ServerController.class.getName() + "#PANIC";
        String SERVER_IP = ServerController.class.getName() + "#SERVER_IP";
    }

    private ExecutorService executor;

    private Server server;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newFixedThreadPool(2);
        receiver = createBroadcastReceiver();
        registerReceiver(receiver, new IntentFilter(Events.SERVER_START));
        registerReceiver(receiver, new IntentFilter(Events.SERVER_STOP));
        registerReceiver(receiver, new IntentFilter(Events.SERVER_GET_STATE));
        registerReceiver(receiver, new IntentFilter(Events.SERVER_GET_LOG));
        registerReceiver(receiver, new IntentFilter(Events.SERVER_GET_QUEUE));
        registerReceiver(receiver, new IntentFilter(Events.ASSIGN_SLOT));
        registerReceiver(receiver, new IntentFilter(Events.PANIC));
        registerReceiver(receiver, new IntentFilter(Events.SERVER_IP));

        server = Server.instance;
        server.setLogger(createLogger(), createServerCallback());
    }

    private ServerStateUpdateCallback createServerCallback() {
        return new ServerStateUpdateCallback() {
            @Override
            public void updateState() {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        Server.State state = server.getState();
                        broadcastServerState(state);
                    }
                });
            }

            @Override
            public void updateQueue() {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        broadcastServerQueue(server.getQueue());
                    }
                });
            }
        };
    }

    private Logger createLogger() {
        return new Logger() {
            @Override
            public void log(final String message) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        broadcastLogMessage(message);
                    }
                });
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
        executor.shutdown();
        try {
            if (executor.awaitTermination(10, TimeUnit.SECONDS) == false) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        unregisterReceiver(receiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (Events.SERVER_START.equals(action)) {
                    startServer();

                } else if (Events.SERVER_STOP.equals(action)) {
                    stopServer();

                } else if (Events.SERVER_IP.equals(action)) {
                    server.setServerIp(intent.getStringExtra(Events.EXTRA_KEY_IP));

                } else if (Events.SERVER_GET_STATE.equals(action)) {
                    broadcastServerState(server.getState());

                } else if (Events.SERVER_GET_LOG.equals(action)) {
                    broadcastLogMessage(server.getLog());

                } else if (Events.SERVER_GET_QUEUE.equals(action)) {
                    broadcastServerQueue(server.getQueue());

                } else if (Events.ASSIGN_SLOT.equals(action)) {
                    String ip = intent.getStringExtra(Events.EXTRA_KEY_IP);
                    int slot = intent.getIntExtra(Events.EXTRA_KEY_SLOT, -1);
                    assignSlot(ip, slot);

                } else if (Events.PANIC.equals(action)) {
                    killAllNotes();
                }
            }

        };
    }

    private void startServer() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                server.start(Util.getBroadcastAddress(ServerController.this));
            }
        });
    }

    private void stopServer() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }

    private void assignSlot(final String ip, final int slot) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                server.assign(ip, slot);
            }
        });
    }

    private void killAllNotes() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                server.sendNoteOffToAllPorts();

            }
        });
    }

    private void broadcastServerQueue(List<Player> queue) {
        Intent intent = new Intent(Events.SERVER_QUEUE);
        intent.putExtra(Events.EXTRA_KEY_QUEUE, (Serializable) queue);
        sendBroadcast(intent);
    }

    private void broadcastServerState(Server.State state) {
        String ip = (state == Server.State.STARTED) ? Util.getFormattedIpAddress(ServerController.this) : null;

        Intent intent = new Intent(Events.SERVER_STATE);
        intent.putExtra(Events.EXTRA_KEY_STATE_ENUM, state);
        intent.putExtra(Events.EXTRA_KEY_IP, ip);
        sendBroadcast(intent);
    }

    private void broadcastLogMessage(String message) {
        Intent intent = new Intent(Events.SERVER_LOG_MESSAGE);
        intent.putExtra(Events.EXTRA_KEY_LOG_MESSAGE, message);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
