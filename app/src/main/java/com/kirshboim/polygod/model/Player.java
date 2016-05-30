package com.kirshboim.polygod.model;

import java.io.Serializable;

public class Player implements Serializable {

    public final String ip;
    public String name;
    public int slot = -1;
    public long seenAt;

    public Player(String ip, String name) {
        this.ip = ip;
        this.name = name;
        seen();
    }

    public void seen() {
        seenAt = System.currentTimeMillis();
    }

    public void setName(String name) {
        this.name = name;
    }
}
