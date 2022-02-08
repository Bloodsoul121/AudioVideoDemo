package com.example.avd.screen.client;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.screen.client
 *  @文件名:   ClientSocketLive
 *  @创建者:   bloodsoul
 *  @创建时间:  2020/12/27 9:43
 *  @描述：    TODO
 */

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class ClientSocketLive {

    private static final String TAG = "ClientSocketLive";

    private final SocketCallback mSocketCallback;
    private final int mServerPort;
    private ScreenWebSocketClient mSocketClient;

    public ClientSocketLive(SocketCallback callback, int serverPort) {
        mSocketCallback = callback;
        mServerPort = serverPort;
    }

    public void start() {
        try {
//            URI uri = new URI("ws://192.168.1.104:" + mServerPort);
            URI uri = new URI("rtp://239.11.1.1:5004");
            mSocketClient = new ScreenWebSocketClient(mSocketCallback, uri);
            mSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mSocketClient.close();
    }

    public interface SocketCallback {
        void callBack(byte[] data);
    }

    private static class ScreenWebSocketClient extends WebSocketClient {

        private final SocketCallback mSocketCallback;

        public ScreenWebSocketClient(SocketCallback socketCallback, URI serverUri) {
            super(serverUri);
            mSocketCallback = socketCallback;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.i(TAG, "onOpen: thread " + Thread.currentThread().getName());
        }

        @Override
        public void onMessage(String message) {
            Log.i(TAG, "onMessage: message thread " + Thread.currentThread().getName());
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.i(TAG, "onMessage: ByteBuffer thread " + Thread.currentThread().getName());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            mSocketCallback.callBack(buf);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i(TAG, "onClose: " + code + " " + reason + " " + remote);
        }

        @Override
        public void onError(Exception ex) {
            Log.i(TAG, "onError: ");
            ex.printStackTrace();
        }
    }

}
