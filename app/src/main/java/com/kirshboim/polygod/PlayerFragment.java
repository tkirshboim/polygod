package com.kirshboim.polygod;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.kirshboim.polygod.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.fragment.RoboListFragment;

public class PlayerFragment extends RoboListFragment {

    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiver = createBroadcastReceiver();
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ServerController.Events.SERVER_QUEUE.equals(action)) {
                    List<Player> queue = (List<Player>) intent.getSerializableExtra(ServerController.Events.EXTRA_KEY_QUEUE);

                    int index = getListView().getFirstVisiblePosition();
                    View v = getListView().getChildAt(0);
                    int top = (v == null) ? 0 : v.getTop();

                    updateQueue(queue);

                    if (index != -1) {
                        getListView().setSelectionFromTop(index, top);
                    }
                }
            }
        };
    }


    @Override
    public void onStart() {
        super.onStart();
        FragmentActivity activity = getActivity();
        activity.registerReceiver(receiver, new IntentFilter(ServerController.Events.SERVER_QUEUE));
        activity.sendBroadcast(new Intent(ServerController.Events.SERVER_GET_QUEUE));
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(receiver);
    }

    private void updateQueue(List<Player> queue) {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Player player : queue) {
            list.add(putData(player.ip, player.name, player.slot == 0, player.slot == 1, player.slot == 2, player.slot == 3));
        }

        String[] from = {"ip", "name", "port1", "port2", "port3", "port4"};
        int[] to = {R.id.ip, R.id.name, R.id.port1, R.id.port2, R.id.port3, R.id.port4};
        MyListAdapter listAdapter = new MyListAdapter(getActivity(), list, R.layout.queue_row_2, from, to);
        setListAdapter(listAdapter);
    }

    private HashMap<String, Object> putData(String ip, String name, boolean port1, boolean port2, boolean port3, boolean port4) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put("ip", ip);
        item.put("name", name);
        item.put("port1", port1);
        item.put("port2", port2);
        item.put("port3", port3);
        item.put("port4", port4);
        return item;
    }

    private class MyListAdapter extends SimpleAdapter {

        public MyListAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            String name = ((TextView) view.findViewById(R.id.name)).getText().toString();
            String ip = ((TextView) view.findViewById(R.id.ip)).getText().toString();

            addListener(view, R.id.port1, name, ip);
            addListener(view, R.id.port2, name, ip);
            addListener(view, R.id.port3, name, ip);
            addListener(view, R.id.port4, name, ip);

            return view;
        }

        private void addListener(View container, final int resId, final String name, final String ip) {
            View checkbox = container.findViewById(resId);
            if (checkbox instanceof CheckBox) {
                ((CheckBox) checkbox).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        buttonView.setChecked(!isChecked);

                        final int slot = !isChecked ? -1 : (resId == R.id.port1) ? 0 : (resId == R.id.port2) ? 1 :
                                (resId == R.id.port3) ? 2 : (resId == R.id.port4) ? 3 : -1;

                        String message = (slot == -1) ? "Are you sure you want " + name + " to stop playing?"
                                : "Are you sure you want to give " + name + " port " + (slot + 1) + " ?";

                        new AlertDialog.Builder(getActivity())
                                .setMessage(message).setPositiveButton("Do It!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(ServerController.Events.ASSIGN_SLOT);
                                intent.putExtra(ServerController.Events.EXTRA_KEY_IP, ip);
                                intent.putExtra(ServerController.Events.EXTRA_KEY_SLOT, slot);
                                getActivity().sendBroadcast(intent);
                            }
                        }).setNegativeButton("Cancel", null).show();
                    }
                });
            }
        }
    }
}
