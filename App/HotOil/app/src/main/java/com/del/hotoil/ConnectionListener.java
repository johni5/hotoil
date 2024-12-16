package com.del.hotoil;

public interface ConnectionListener {

    void onConnect(String deviceName);

    void onDisconnect();
}
