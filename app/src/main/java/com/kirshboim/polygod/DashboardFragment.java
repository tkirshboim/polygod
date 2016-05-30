package com.kirshboim.polygod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class DashboardFragment extends RoboFragment {

    @InjectView(R.id.server_switch)
    private Switch serverSwitch;
    @InjectView(R.id.server_status)
    private TextView statusText;
    @InjectView(R.id.server_log)
    private TextView log;
    @InjectView(R.id.server_ip)
    private TextView serverIp;
    @InjectView(R.id.server_ip_button)
    private Button setServerIpButton;
    @InjectView(R.id.panic_button)
    private Button panicButton;
    @InjectView(R.id.text_about)
    private TextView about;

    private boolean autoScroll = true;
    private BroadcastReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dashboard_fragment, container, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        serverSwitch.setOnClickListener(createServerSwitchClickListener());
        log.setOnClickListener(createLogClickListener());
        panicButton.setOnClickListener(createPanicListener());
        setServerIpButton.setOnClickListener(createSetIpListener());
        about.setOnClickListener(createAboutListener());

        log.setMovementMethod(new ScrollingMovementMethod());
        receiver = createBroadcastReceiver();
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ServerController.Events.SERVER_STATE.equals(action)) {
                    Server.State state =
                            (Server.State) intent.getSerializableExtra(ServerController.Events.EXTRA_KEY_STATE_ENUM);
                    String ip = intent.getStringExtra(ServerController.Events.EXTRA_KEY_IP);
                    updateServerState(state, ip);
                } else if (ServerController.Events.SERVER_LOG_MESSAGE.equals(action)) {
                    String logMessage = intent.getStringExtra(ServerController.Events.EXTRA_KEY_LOG_MESSAGE);
                    log.append(logMessage);

                    if (autoScroll) {
                        int scrollAmount = log.getLayout().getLineTop(log.getLineCount()) - log.getHeight();
                        if (scrollAmount > 0)
                            log.scrollTo(0, scrollAmount);
                        else
                            log.scrollTo(0, 0);
                    }

                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        FragmentActivity activity = getActivity();
        activity.registerReceiver(receiver, new IntentFilter(ServerController.Events.SERVER_STATE));
        activity.registerReceiver(receiver, new IntentFilter(ServerController.Events.SERVER_LOG_MESSAGE));
        activity.sendBroadcast(new Intent(ServerController.Events.SERVER_GET_STATE));

        log.setText("");
        activity.sendBroadcast(new Intent(ServerController.Events.SERVER_GET_LOG));

        String ip = loadServerIp();
        broadcastServerIp(ip);
        serverIp.setText(ip);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(receiver);
    }

    private String loadServerIp() {
        return PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("serverIp", "127.0.0.1");
    }

    private void saveServerIp(String ip) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("serverIp", ip).apply();
    }

    private void broadcastServerIp(String ip) {
        Intent intent = new Intent(ServerController.Events.SERVER_IP);
        intent.putExtra(ServerController.Events.EXTRA_KEY_IP, ip);
        getActivity().sendBroadcast(intent);
    }

    private void updateServerState(Server.State state, String ip) {
        if (state == Server.State.FAILED) {
            serverSwitch.setEnabled(true);
            return;
        } else {
            serverSwitch.setChecked(state == Server.State.STARTING || state == Server.State.STARTED);
            serverSwitch.setEnabled(state == Server.State.STARTED || state == Server.State.STOPPED);
        }

        statusText.setText(state.toString() + (ip == null ? "" : " on " + ip));
    }

    public View.OnClickListener createServerSwitchClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = serverSwitch.isChecked();
                if (serverSwitch.isChecked()) {
                    getActivity().sendBroadcast(new Intent(ServerController.Events.SERVER_START));
                } else {
                    getActivity().sendBroadcast(new Intent(ServerController.Events.SERVER_STOP));
                }

                serverSwitch.setChecked(!checked);
            }
        };
    }

    public View.OnClickListener createLogClickListener() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                autoScroll = !autoScroll;
            }
        };
    }

    private View.OnClickListener createPanicListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().sendBroadcast(new Intent(ServerController.Events.PANIC));
            }
        };
    }

    private View.OnClickListener createSetIpListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = serverIp.getText().toString();

                if (Util.isValidId(ip) == false) {
                    Toast.makeText(getActivity(), "Invalid Ip - not set", Toast.LENGTH_SHORT).show();

                } else {
                    saveServerIp(ip);
                    broadcastServerIp(ip);
                }
            }
        };
    }

    private View.OnClickListener createAboutListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardFragment.this.getActivity(), AboutActivity.class));
            }
        };
    }

}
