package com.example.avd.livechat.base;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.livechat.base
 *  @文件名:   SocketLive
 *  @创建者:   bloodsoul
 *  @创建时间:  2020/12/27 17:16
 *  @描述：    TODO
 */

public interface SocketLive {

    void start();

    void close();

    void sendData(byte[] bytes);

}
