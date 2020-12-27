package com.example.avd.screen.server;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.screen.server
 *  @文件名:   SocketLive
 *  @创建者:   bloodsoul
 *  @创建时间:  2020/12/27 8:26
 *  @描述：    TODO
 */

import android.media.projection.MediaProjection;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerSocketLive {

    private static final String TAG = "SocketLive";

    private final ScreenWebSocketServer mServer;
    private CodecLiveTask mCodecLiveTask;

    public ServerSocketLive(int serverPort) {
        mServer = new ScreenWebSocketServer(new InetSocketAddress(serverPort));
    }

    public boolean isOpen() {
        return mServer.isOpen();
    }

    public void start(MediaProjection mediaProjection) {
        mServer.start();
        mCodecLiveTask = new CodecLiveTask(this);
        mCodecLiveTask.startLive(mediaProjection);
    }

    public void stop() {
        if (mCodecLiveTask != null) {
            mCodecLiveTask.stopLive();
            mCodecLiveTask = null;
        }
        try {
            mServer.close();
            mServer.stop();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendData(byte[] bytes) {
        mServer.sendData(bytes);
    }

    private static class ScreenWebSocketServer extends WebSocketServer {

        private boolean mIsOpen;
        private WebSocket mWebSocket;

        public ScreenWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        public boolean isOpen() {
            return mIsOpen;
        }

        public void sendData(byte[] bytes) {
            if (mWebSocket != null && mWebSocket.isOpen()) {
                mWebSocket.send(bytes);
            }
        }

        public void close() {
            if (mWebSocket != null) {
                mWebSocket.close();
            }
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Log.i(TAG, "onOpen: thread " + Thread.currentThread().getName());
            mIsOpen = true;
            mWebSocket = conn;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.i(TAG, "onClose: thread " + Thread.currentThread().getName());
            mIsOpen = false;
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.i(TAG, "onMessage: ");
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            mIsOpen = false;
            Log.i(TAG, "onError: thread " + Thread.currentThread().getName());
        }

        @Override
        public void onStart() {
            mIsOpen = true;
            Log.i(TAG, "onStart: thread " + Thread.currentThread().getName());
        }
    }

}
