package com.kirshboim.polygod;

import com.google.inject.Singleton;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

@Singleton
public class PolyjamHttpServer extends NanoHTTPD {

    public static final int BASE_PLAY_PORT = 5550;

    private final RequestHandler handler;
    private String serverIp;

    public PolyjamHttpServer(int port, RequestHandler handler) {
        super(port);
        this.handler = handler;
    }

    public void start(String serverIp) throws IOException {
        super.start();
        this.serverIp = serverIp;
    }

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();
        String remoteIp = session.getHeaders().get("remote-addr");

        if (uri.equals("/hi")) {
            String name = session.getParms().get("name").trim();

            if (handler.register(remoteIp, name)) {
                return new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, "hi '" +
                        name + "' at '" + remoteIp + "'");
            } else {
                return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "no name, no game");
            }

        } else if (uri.equals("/status")) {
            int port = handler.status(remoteIp);

            if (port == -2) {
                return new NanoHTTPD.Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "don't know you");

            } else if (port == -1) {
                return new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, "wait");

            } else {
                String response = String.valueOf(BASE_PLAY_PORT + port) + ' ' + serverIp;
                return new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, response);

            }
        }

        return new NanoHTTPD.Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, ":(");
    }
}
