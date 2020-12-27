package com.example.avd.livechat.server;

import android.util.Log;

import com.example.avd.livechat.base.SocketCallback;
import com.example.avd.livechat.base.SocketLive;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

//音视频通话服务端
public class ServerSocketLive implements SocketLive {

    private static final String TAG = "David";

    private WebSocket webSocket;
    private final SocketCallback socketCallback;

    public ServerSocketLive(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;
    }

    @Override
    public void start() {
        webSocketServer.start();
    }

    @Override
    public void close() {
        try {
            if (webSocket != null) {
                webSocket.close();
            }
            webSocketServer.stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

    private final WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(20000)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            ServerSocketLive.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            Log.i(TAG, "onClose: 关闭 socket ");
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {

        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer bytes) {
            Log.i(TAG, "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            Log.i(TAG, "onError:  " + e.toString());
        }

        @Override
        public void onStart() {

        }
    };

}
