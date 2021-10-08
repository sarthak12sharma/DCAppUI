package com.example.cast;

import java.util.HashMap;

public class DefaultPlatform {

    public DefaultPlatform() {
    }

    public static HashMap<String, String> getDeviceServiceMap() { 
        HashMap<String, String> devicesList = new HashMap<String, String>();
        devicesList.put("com.example.cast.service.WebOSTVService", "com.example.cast.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.example.cast.service.DLNAService", "com.example.cast.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.example.cast.service.RokuService", "com.example.cast.discovery.provider.SSDPDiscoveryProvider");
        devicesList.put("com.example.cast.service.CastService", "com.example.cast.discovery.provider.CastDiscoveryProvider");
//        devicesList.put("com.example.cast.service.AirPlayService", "com.example.cast.discovery.provider.ZeroconfDiscoveryProvider");
        devicesList.put("com.example.cast.service.FireTVService", "com.example.cast.discovery.provider.FireTVDiscoveryProvider");
        return devicesList;
    }

}
