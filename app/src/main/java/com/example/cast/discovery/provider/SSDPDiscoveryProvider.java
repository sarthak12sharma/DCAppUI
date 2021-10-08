package com.example.cast.discovery.provider;

import static com.facebook.ads.AdError.MEDIATION_ERROR_CODE;

import android.content.Context;
import android.util.Log;
import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryProvider;
import com.example.cast.discovery.DiscoveryProviderListener;
import com.example.cast.discovery.provider.ssdp.SSDPClient;
import com.example.cast.discovery.provider.ssdp.SSDPDevice;
import com.example.cast.discovery.provider.ssdp.SSDPPacket;
import com.example.cast.service.config.ServiceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSDPDiscoveryProvider implements DiscoveryProvider {
    Context context;
    ConcurrentHashMap<String, ServiceDescription> discoveredServices = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<>();
    boolean isRunning = false;
    private Runnable mRespNotifyHandler = new Runnable() {
        /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57154 */

        public void run() {
            while (SSDPDiscoveryProvider.this.ssdpClient != null) {
                try {
                    SSDPDiscoveryProvider.this.handleSSDPPacket(new SSDPPacket(SSDPDiscoveryProvider.this.ssdpClient.multicastReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                    return;
                }
            }
        }
    };
    private Runnable mResponseHandler = new Runnable() {
        /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57143 */

        public void run() {
            while (SSDPDiscoveryProvider.this.ssdpClient != null) {
                try {
                    SSDPDiscoveryProvider.this.handleSSDPPacket(new SSDPPacket(SSDPDiscoveryProvider.this.ssdpClient.responseReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                    return;
                }
            }
        }
    };
    boolean needToStartSearch = false;
    private Thread notifyThread;
    private Thread responseThread;
    private Timer scanTimer;
    List<DiscoveryFilter> serviceFilters;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    private SSDPClient ssdpClient;
    private Pattern uuidReg;

    public boolean containsServicesWithFilter(SSDPDevice sSDPDevice, String str) {
        return true;
    }

    public SSDPDiscoveryProvider(Context context2) {
        this.context = context2;
        this.uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");
        this.serviceListeners = new CopyOnWriteArrayList<>();
        this.serviceFilters = new CopyOnWriteArrayList();
    }

    private void openSocket() {
        SSDPClient sSDPClient = this.ssdpClient;
        if (sSDPClient == null || !sSDPClient.isConnected()) {
            try {
                InetAddress ipAddress = Util.getIpAddress(this.context);
                if (ipAddress != null) {
                    this.ssdpClient = createSocket(ipAddress);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: protected */
    public SSDPClient createSocket(InetAddress inetAddress) throws IOException {
        return new SSDPClient(inetAddress);
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            openSocket();
            List<DiscoveryFilter> list = this.serviceFilters;
            if (list != null && !list.isEmpty()) {
                this.executorService = Executors.newScheduledThreadPool(this.serviceFilters.size() * 3);
            }
            this.scanTimer = new Timer();
            this.scanTimer.schedule(new TimerTask() {
                /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.C57121 */

                public void run() {
                    SSDPDiscoveryProvider.this.sendSearch();
                }
            }, 100, 10000);
            this.responseThread = new Thread(this.mResponseHandler);
            this.notifyThread = new Thread(this.mRespNotifyHandler);
            this.responseThread.start();
            this.notifyThread.start();
        }
    }

    public void sendSearch() {
        ArrayList<String> arrayList = new ArrayList();
        long time = new Date().getTime() - 60000;
        for (String str : this.foundServices.keySet()) {
            ServiceDescription serviceDescription = this.foundServices.get(str);
            if (serviceDescription == null || serviceDescription.getLastDetection() < time) {
                arrayList.add(str);
            }
        }
        for (String str2 : arrayList) {
            ServiceDescription serviceDescription2 = this.foundServices.get(str2);
            if (serviceDescription2 != null) {
                notifyListenersOfLostService(serviceDescription2);
            }
            if (this.foundServices.containsKey(str2)) {
                this.foundServices.remove(str2);
            }
        }
        rescan();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void stop() {
        this.isRunning = false;
        Timer timer = this.scanTimer;
        if (timer != null) {
            timer.cancel();
            this.scanTimer = null;
        }
        Thread thread = this.responseThread;
        if (thread != null) {
            thread.interrupt();
            this.responseThread = null;
        }
        Thread thread2 = this.notifyThread;
        if (thread2 != null) {
            thread2.interrupt();
            this.notifyThread = null;
        }
        SSDPClient sSDPClient = this.ssdpClient;
        if (sSDPClient != null) {
            sSDPClient.close();
            this.ssdpClient = null;
        }
        ScheduledExecutorService scheduledExecutorService = this.executorService;
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            this.executorService.shutdown();
        }
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void restart() {
        stop();
        start();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void reset() {
        stop();
        this.foundServices.clear();
        this.discoveredServices.clear();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void rescan() {
        ScheduledExecutorService scheduledExecutorService = this.executorService;
        if (scheduledExecutorService == null || scheduledExecutorService.isShutdown()) {
            Log.w(Util.T, "There are no filters added");
            return;
        }
        for (DiscoveryFilter discoveryFilter : this.serviceFilters) {
            final String sSDPSearchMessage = SSDPClient.getSSDPSearchMessage(discoveryFilter.getServiceFilter());
            for (int i = 0; i < 3; i++) {
                try {
                    this.executorService.schedule(new Runnable() {
                        /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57132 */

                        public void run() {
                            try {
                                if (SSDPDiscoveryProvider.this.ssdpClient != null) {
                                    SSDPDiscoveryProvider.this.ssdpClient.send(sSDPSearchMessage);
                                }
                            } catch (IOException e) {
                                Log.e(Util.T, e.getMessage());
                            }
                        }
                    }, (long) i, TimeUnit.SECONDS);
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
        if (discoveryFilter.getServiceFilter() == null) {
            Log.e(Util.T, "This device filter does not have ssdp filter info");
        } else {
            this.serviceFilters.add(discoveryFilter);
        }
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
        this.serviceFilters.remove(discoveryFilter);
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void setFilters(List<DiscoveryFilter> list) {
        this.serviceFilters = list;
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public boolean isEmpty() {
        return this.serviceFilters.size() == 0;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSSDPPacket(SSDPPacket sSDPPacket) {
        String str;
        if (sSDPPacket != null && sSDPPacket.getData().size() != 0 && sSDPPacket.getType() != null) {
            String str2 = sSDPPacket.getData().get(sSDPPacket.getType().equals(SSDPClient.NOTIFY) ? "NT" : "ST");
            if (str2 != null && !SSDPClient.MSEARCH.equals(sSDPPacket.getType()) && isSearchingForFilter(str2) && (str = sSDPPacket.getData().get("USN")) != null && str.length() != 0) {
                Matcher matcher = this.uuidReg.matcher(str);
                if (matcher.find()) {
                    String group = matcher.group();
                    if (SSDPClient.BYEBYE.equals(sSDPPacket.getData().get("NTS"))) {
                        ServiceDescription serviceDescription = this.foundServices.get(group);
                        if (serviceDescription != null) {
                            this.foundServices.remove(group);
                            notifyListenersOfLostService(serviceDescription);
                            return;
                        }
                        return;
                    }
                    String str3 = sSDPPacket.getData().get("LOCATION");
                    if (str3 != null && str3.length() != 0) {
                        ServiceDescription serviceDescription2 = this.foundServices.get(group);
                        if (serviceDescription2 == null && this.discoveredServices.get(group) == null) {
                            serviceDescription2 = new ServiceDescription();
                            serviceDescription2.setUUID(group);
                            serviceDescription2.setServiceFilter(str2);
                            serviceDescription2.setIpAddress(sSDPPacket.getDatagramPacket().getAddress().getHostAddress());
                            serviceDescription2.setPort(MEDIATION_ERROR_CODE);
                            this.discoveredServices.put(group, serviceDescription2);
                            getLocationData(str3, group, str2);
                        }
                        if (serviceDescription2 != null) {
                            serviceDescription2.setLastDetection(new Date().getTime());
                        }
                    }
                }
            }
        }
    }

    public void getLocationData(String str, String str2, String str3) {
        try {
            getLocationData(new URL(str), str2, str3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLocationData(final URL url, final String str, final String str2) {
        Util.runInBackground(new Runnable() {
            /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57165 */

            public void run() {
                SSDPDevice sSDPDevice;
                ServiceDescription serviceDescription;
                try {
                    sSDPDevice = new SSDPDevice(url, str2);
                } catch (Exception e) {
                    e.printStackTrace();
                    sSDPDevice = null;
                }
                if (sSDPDevice != null) {
                    sSDPDevice.UUID = str;
                    if (SSDPDiscoveryProvider.this.containsServicesWithFilter(sSDPDevice, str2) && (serviceDescription = SSDPDiscoveryProvider.this.discoveredServices.get(str)) != null) {
                        serviceDescription.setServiceFilter(str2);
                        serviceDescription.setFriendlyName(sSDPDevice.friendlyName);
                        serviceDescription.setModelName(sSDPDevice.modelName);
                        serviceDescription.setModelNumber(sSDPDevice.modelNumber);
                        serviceDescription.setModelDescription(sSDPDevice.modelDescription);
                        serviceDescription.setManufacturer(sSDPDevice.manufacturer);
                        serviceDescription.setApplicationURL(sSDPDevice.applicationURL);
                        serviceDescription.setServiceList(sSDPDevice.serviceList);
                        serviceDescription.setResponseHeaders(sSDPDevice.headers);
                        serviceDescription.setLocationXML(sSDPDevice.locationXML);
                        serviceDescription.setServiceURI(sSDPDevice.serviceURI);
                        serviceDescription.setPort(sSDPDevice.port);
                        SSDPDiscoveryProvider.this.foundServices.put(str, serviceDescription);
                        SSDPDiscoveryProvider.this.notifyListenersOfNewService(serviceDescription);
                    }
                }
                SSDPDiscoveryProvider.this.discoveredServices.remove(str);
            }
        }, true);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyListenersOfNewService(ServiceDescription serviceDescription) {
        for (String str : serviceIdsForFilter(serviceDescription.getServiceFilter())) {
            final ServiceDescription clone = serviceDescription.clone();
            clone.setServiceID(str);
            Util.runOnUI(new Runnable() {
                /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57176 */

                public void run() {
                    Iterator it = SSDPDiscoveryProvider.this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        ((DiscoveryProviderListener) it.next()).onServiceAdded(SSDPDiscoveryProvider.this, clone);
                    }
                }
            });
        }
    }

    private void notifyListenersOfLostService(ServiceDescription serviceDescription) {
        for (String str : serviceIdsForFilter(serviceDescription.getServiceFilter())) {
            final ServiceDescription clone = serviceDescription.clone();
            clone.setServiceID(str);
            Util.runOnUI(new Runnable() {
                /* class com.inshot.cast.core.discovery.provider.SSDPDiscoveryProvider.RunnableC57187 */

                public void run() {
                    Iterator it = SSDPDiscoveryProvider.this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        ((DiscoveryProviderListener) it.next()).onServiceRemoved(SSDPDiscoveryProvider.this, clone);
                    }
                }
            });
        }
    }

    public List<String> serviceIdsForFilter(String str) {
        String serviceId;
        ArrayList arrayList = new ArrayList();
        for (DiscoveryFilter discoveryFilter : this.serviceFilters) {
            if (discoveryFilter.getServiceFilter().equals(str) && (serviceId = discoveryFilter.getServiceId()) != null) {
                arrayList.add(serviceId);
            }
        }
        return arrayList;
    }

    public boolean isSearchingForFilter(String str) {
        for (DiscoveryFilter discoveryFilter : this.serviceFilters) {
            if (discoveryFilter.getServiceFilter().equals(str)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }
}

/*
import android.content.Context;
import android.util.Log;

import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryProvider;
import com.example.cast.discovery.DiscoveryProviderListener;
import com.example.cast.discovery.provider.ssdp.SSDPClient;
import com.example.cast.discovery.provider.ssdp.SSDPDevice;
import com.example.cast.discovery.provider.ssdp.SSDPPacket;
import com.example.cast.service.config.ServiceDescription;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

public class SSDPDiscoveryProvider implements DiscoveryProvider {
    Context context;

    boolean needToStartSearch = false;

    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;

    ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<String, ServiceDescription>();
    ConcurrentHashMap<String, ServiceDescription> discoveredServices = new ConcurrentHashMap<String, ServiceDescription>();

    List<DiscoveryFilter> serviceFilters;

    private SSDPClient ssdpClient;

    private Timer scanTimer;

    private Pattern uuidReg;

    private Thread responseThread;
    private Thread notifyThread;

    boolean isRunning = false;

    public SSDPDiscoveryProvider(Context context) {
        this.context = context;

        uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");

        serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
        serviceFilters = new CopyOnWriteArrayList<DiscoveryFilter>();
    }

    private void openSocket() {
        if (ssdpClient != null && ssdpClient.isConnected())
            return;

        try {
            InetAddress source = Util.getIpAddress(context);
            if (source == null)
                return;

            ssdpClient = createSocket(source);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected SSDPClient createSocket(InetAddress source) throws IOException {
        return new SSDPClient(source);
    }

    @Override
    public void start() {
        if (isRunning)
            return;

        isRunning = true;

        openSocket();

        scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendSearch();
            }
        }, 100, RESCAN_INTERVAL);

        responseThread = new Thread(mResponseHandler);
        notifyThread = new Thread(mRespNotifyHandler);

        responseThread.start();
        notifyThread.start();
    }

    public void sendSearch() {
        List<String> killKeys = new ArrayList<String>();

        long killPoint = new Date().getTime() - TIMEOUT;

        for (String key : foundServices.keySet()) {
            ServiceDescription service = foundServices.get(key);
            if (service == null || service.getLastDetection() < killPoint) {
                killKeys.add(key);
            }
        }

        for (String key : killKeys) {
            final ServiceDescription service = foundServices.get(key);

            if (service != null) {
                notifyListenersOfLostService(service);
            }

            if (foundServices.containsKey(key))
                foundServices.remove(key);
        }

        rescan();
    }

    @Override
    public void stop() {
        isRunning = false;

        if (scanTimer != null) {
            scanTimer.cancel();
            scanTimer = null;
        }

        if (responseThread != null) {
            responseThread.interrupt();
            responseThread = null;
        }

        if (notifyThread != null) {
            notifyThread.interrupt();
            notifyThread = null;
        }

        if (ssdpClient != null) {
            ssdpClient.close();
            ssdpClient = null;
        }
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public void reset() {
        stop();
        foundServices.clear();
        discoveredServices.clear();
    }

    @Override
    public void rescan() {
        for (DiscoveryFilter searchTarget : serviceFilters) {
            final String message = SSDPClient.getSSDPSearchMessage(searchTarget.getServiceFilter());

            Timer timer = new Timer();
            */
/* Send 3 times like WindowsMedia *//*

            for (int i = 0; i < 3; i++) {
                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {
                        try {
                            if (ssdpClient != null)
                                ssdpClient.send(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

                timer.schedule(task, i * 1000);
            }
        }

    }

    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {
        if (filter.getServiceFilter() == null) {
            Log.e(Util.T, "This device filter does not have ssdp filter info");
        } else {
            serviceFilters.add(filter);
        }
    }

    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {
        serviceFilters.remove(filter);
    }

    @Override
    public void setFilters(List<DiscoveryFilter> filters) {
        serviceFilters = filters;
    }

    @Override
    public boolean isEmpty() {
        return serviceFilters.size() == 0;
    }

    private Runnable mResponseHandler = new Runnable() {
        @Override
        public void run() {
            while (ssdpClient != null) {
                try {
                    handleSSDPPacket(new SSDPPacket(ssdpClient.responseReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private Runnable mRespNotifyHandler = new Runnable() {
        @Override
        public void run() {
            while (ssdpClient != null) {
                try {
                    handleSSDPPacket(new SSDPPacket(ssdpClient.multicastReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private void handleSSDPPacket(SSDPPacket ssdpPacket) {
        // Debugging stuff
//        Util.runOnUI(new Runnable() {
//
//            @Override
//            public void run() {
//                Log.d("Connect SDK Socket", "Packet received | type = " + ssdpPacket.type);
//
//                for (String key : ssdpPacket.data.keySet()) {
//                    Log.d("Connect SDK Socket", "    " + key + " = " + ssdpPacket.data.get(key));
//                }
//                Log.d("Connect SDK Socket", "__________________________________________");
//            }
//        });
        // End Debugging stuff

        if (ssdpPacket == null || ssdpPacket.getData().size() == 0 || ssdpPacket.getType() == null)
            return;

        String serviceFilter = ssdpPacket.getData().get(ssdpPacket.getType().equals(SSDPClient.NOTIFY) ? "NT" : "ST");

        if (serviceFilter == null || SSDPClient.MSEARCH.equals(ssdpPacket.getType()) || !isSearchingForFilter(serviceFilter))
            return;

        String usnKey = ssdpPacket.getData().get("USN");

        if (usnKey == null || usnKey.length() == 0)
            return;

        Matcher m = uuidReg.matcher(usnKey);

        if (!m.find())
            return;

        String uuid = m.group();

        if (SSDPClient.BYEBYE.equals(ssdpPacket.getData().get("NTS"))) {
            final ServiceDescription service = foundServices.get(uuid);

            if (service != null) {
                foundServices.remove(uuid);

                notifyListenersOfLostService(service);
            }
        } else {
            String location = ssdpPacket.getData().get("LOCATION");

            if (location == null || location.length() == 0)
                return;

            ServiceDescription foundService = foundServices.get(uuid);
            ServiceDescription discoverdService = discoveredServices.get(uuid);

            boolean isNew = foundService == null && discoverdService == null;

            if (isNew) {
                foundService = new ServiceDescription();
                foundService.setUUID(uuid);
                foundService.setServiceFilter(serviceFilter);
                foundService.setIpAddress(ssdpPacket.getDatagramPacket().getAddress().getHostAddress());
                foundService.setPort(3001);

                discoveredServices.put(uuid, foundService);

                getLocationData(location, uuid, serviceFilter);
            }

            if (foundService != null)
                foundService.setLastDetection(new Date().getTime());
        }
    }

    public void getLocationData(final String location, final String uuid, final String serviceFilter) {
        try {
            getLocationData(new URL(location), uuid, serviceFilter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLocationData(final URL location, final String uuid, final String serviceFilter) {
        Util.runInBackground(new Runnable() {

            @Override
            public void run() {
                SSDPDevice device = null;
                try {
                    device = new SSDPDevice(location, serviceFilter);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }

                if (device != null) {
                    device.UUID = uuid;
                    boolean hasServices = containsServicesWithFilter(device, serviceFilter);

                    if (hasServices) {
                        final ServiceDescription service = discoveredServices.get(uuid);

                        if (service != null) {
                            service.setServiceFilter(serviceFilter);
                            service.setFriendlyName(device.friendlyName);
                            service.setModelName(device.modelName);
                            service.setModelNumber(device.modelNumber);
                            service.setModelDescription(device.modelDescription);
                            service.setManufacturer(device.manufacturer);
                            service.setApplicationURL(device.applicationURL);
                            service.setServiceList(device.serviceList);
                            service.setResponseHeaders(device.headers);
                            service.setLocationXML(device.locationXML);
                            service.setServiceURI(device.serviceURI);
                            service.setPort(device.port);

                            foundServices.put(uuid, service);

                            notifyListenersOfNewService(service);
                        }
                    }
                }

                discoveredServices.remove(uuid);
            }
        }, true);

    }

    private void notifyListenersOfNewService(ServiceDescription service) {
        List<String> serviceIds = serviceIdsForFilter(service.getServiceFilter());

        for (String serviceId : serviceIds) {
            ServiceDescription _newService = service.clone();
            _newService.setServiceID(serviceId);

            final ServiceDescription newService = _newService;

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {

                    for (DiscoveryProviderListener listener : serviceListeners) {
                        listener.onServiceAdded(SSDPDiscoveryProvider.this, newService);
                    }
                }
            });
        }
    }

    private void notifyListenersOfLostService(ServiceDescription service) {
        List<String> serviceIds = serviceIdsForFilter(service.getServiceFilter());

        for (String serviceId : serviceIds) {
            ServiceDescription _newService = service.clone();
            _newService.setServiceID(serviceId);

            final ServiceDescription newService = _newService;

            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    for (DiscoveryProviderListener listener : serviceListeners) {
                        listener.onServiceRemoved(SSDPDiscoveryProvider.this, newService);
                    }
                }
            });
        }
    }

    public List<String> serviceIdsForFilter(String filter) {
        ArrayList<String> serviceIds = new ArrayList<String>();

        for (DiscoveryFilter serviceFilter : serviceFilters) {
            String ssdpFilter = serviceFilter.getServiceFilter();

            if (ssdpFilter.equals(filter)) {
                String serviceId = serviceFilter.getServiceId();

                if (serviceId != null)
                    serviceIds.add(serviceId);
            }
        }

        return serviceIds;
    }

    public boolean isSearchingForFilter(String filter) {
        for (DiscoveryFilter serviceFilter : serviceFilters) {
            String ssdpFilter = serviceFilter.getServiceFilter();

            if (ssdpFilter.equals(filter))
                return true;
        }

        return false;
    }

    public boolean containsServicesWithFilter(SSDPDevice device, String filter) {
//        List<String> servicesRequired = new ArrayList<String>();
//
//        for (JSONObject serviceFilter : serviceFilters) {
//        }

        //  TODO  Implement this method.  Not sure why needs to happen since there are now required services.

        return true;
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
        serviceListeners.remove(listener);
    }
}*/
