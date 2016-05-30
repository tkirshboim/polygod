package com.kirshboim.polygod;

public interface RequestHandler {

    boolean register(String remoteIp, String name);

    int status(String remoteIp);
}
