package com.example.cast.discovery.provider;

import android.content.Context;
import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryProvider;
import com.example.cast.discovery.DiscoveryProviderListener;
import com.example.cast.service.FireTVService;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.config.ServiceDescription;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FireTVDiscoveryProvider implements DiscoveryProvider {
    private DiscoveryController discoveryController;
    DiscoveryController.IDiscoveryListener fireTVListener;
    ConcurrentHashMap<String, ServiceDescription> foundServices;
    private boolean isRunning;
    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void setFilters(List<DiscoveryFilter> list) {
    }

    public FireTVDiscoveryProvider(Context context) {
        this(new DiscoveryController(context));
    }

    public FireTVDiscoveryProvider(DiscoveryController discoveryController2) {
        this.foundServices = new ConcurrentHashMap<>();
        this.serviceListeners = new CopyOnWriteArrayList<>();
        this.discoveryController = discoveryController2;
        this.fireTVListener = new FireTVDiscoveryListener();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void start() {
        if (!this.isRunning) {
            this.discoveryController.start(this.fireTVListener);
            this.isRunning = true;
        }
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void stop() {
        if (this.isRunning) {
            this.discoveryController.stop();
            this.isRunning = false;
        }
        for (ServiceDescription serviceDescription : this.foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        this.foundServices.clear();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void restart() {
        stop();
        start();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void rescan() {
        restart();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void reset() {
        this.foundServices.clear();
        stop();
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }

    @Override // com.inshot.cast.core.discovery.DiscoveryProvider
    public boolean isEmpty() {
        return this.foundServices.isEmpty();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            /* class com.inshot.cast.core.discovery.provider.FireTVDiscoveryProvider.RunnableC57091 */

            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceAdded(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            /* class com.inshot.cast.core.discovery.provider.FireTVDiscoveryProvider.RunnableC57102 */

            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceRemoved(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError serviceCommandError) {
        Util.runOnUI(new Runnable() {
            /* class com.inshot.cast.core.discovery.provider.FireTVDiscoveryProvider.RunnableC57113 */

            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceDiscoveryFailed(FireTVDiscoveryProvider.this, serviceCommandError);
                }
            }
        });
    }

    class FireTVDiscoveryListener implements DiscoveryController.IDiscoveryListener {
        FireTVDiscoveryListener() {
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void playerDiscovered(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer != null) {
                String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
                ServiceDescription serviceDescription = FireTVDiscoveryProvider.this.foundServices.get(uniqueIdentifier);
                if (serviceDescription == null) {
                    ServiceDescription serviceDescription2 = new ServiceDescription();
                    updateServiceDescription(serviceDescription2, remoteMediaPlayer);
                    FireTVDiscoveryProvider.this.foundServices.put(uniqueIdentifier, serviceDescription2);
                    FireTVDiscoveryProvider.this.notifyListenersThatServiceAdded(serviceDescription2);
                    return;
                }
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
            }
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void playerLost(RemoteMediaPlayer remoteMediaPlayer) {
            ServiceDescription serviceDescription;
            if (remoteMediaPlayer != null && (serviceDescription = FireTVDiscoveryProvider.this.foundServices.get(remoteMediaPlayer.getUniqueIdentifier())) != null) {
                FireTVDiscoveryProvider.this.notifyListenersThatServiceLost(serviceDescription);
                FireTVDiscoveryProvider.this.foundServices.remove(remoteMediaPlayer.getUniqueIdentifier());
            }
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void discoveryFailure() {
            FireTVDiscoveryProvider.this.notifyListenersThatDiscoveryFailed(new ServiceCommandError("FireTV discovery failure"));
        }

        private void updateServiceDescription(ServiceDescription serviceDescription, RemoteMediaPlayer remoteMediaPlayer) {
            String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
            serviceDescription.setDevice(remoteMediaPlayer);
            serviceDescription.setFriendlyName(remoteMediaPlayer.getName());
            serviceDescription.setIpAddress(uniqueIdentifier);
            serviceDescription.setServiceID(FireTVService.ID);
            serviceDescription.setUUID(uniqueIdentifier);
        }
    }
}

/*
import android.content.Context;
import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryProvider;
import com.example.cast.discovery.DiscoveryProviderListener;
import com.example.cast.service.FireTVService;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.config.ServiceDescription;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FireTVDiscoveryProvider implements DiscoveryProvider {

    private DiscoveryController discoveryController;

    private boolean isRunning;

    DiscoveryController.IDiscoveryListener fireTVListener;

    ConcurrentHashMap<String, ServiceDescription> foundServices
            = new ConcurrentHashMap<>();

    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners
            = new CopyOnWriteArrayList<>();

    public FireTVDiscoveryProvider(Context context) {
        this(new DiscoveryController(context));
    }

    public FireTVDiscoveryProvider(DiscoveryController discoveryController) {
        this.discoveryController = discoveryController;
        this.fireTVListener = new FireTVDiscoveryListener();
    }

    */
/**
     * Safely start discovery. Ignore if it's already started.
     *//*

    @Override
    public void start() {
        if (!isRunning) {
            discoveryController.start(fireTVListener);
            isRunning = true;
        }
    }

    */
/**
     * Safely stop discovery and remove all found FireTV services because they don't work when
     * discovery is stopped. Ignore if it's already stopped.
     *//*

    @Override
    public void stop() {
        if (isRunning) {
            discoveryController.stop();
            isRunning = false;
        }
        for (ServiceDescription serviceDescription : foundServices.values()) {
            notifyListenersThatServiceLost(serviceDescription);
        }
        foundServices.clear();
    }

    */
/**
     * Safely restart discovery
     *//*

    @Override
    public void restart() {
        stop();
        start();
    }

    */
/**
     * Invokes restart method since FlingSDK doesn't have analog of rescan
     *//*

    @Override
    public void rescan() {
        // discovery controller doesn't have rescan capability
        restart();
    }

    */
/**
     * Stop discovery and removes all cached services
     *//*

    @Override
    public void reset() {
        foundServices.clear();
        stop();
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
        serviceListeners.remove(listener);
    }

    */
/**
     * DiscoveryFilter is not used in current implementation
     *//*

    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    */
/**
     * DiscoveryFilter is not used in current implementation
     *//*

    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {
        // intentionally left blank
    }

    */
/**
     * DiscoveryFilter is not used in current implementation
     *//*

    @Override
    public void setFilters(List<DiscoveryFilter> filters) {
        // intentionally left blank
    }

    @Override
    public boolean isEmpty() {
        return foundServices.isEmpty();
    }

    private void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                for (DiscoveryProviderListener listener : serviceListeners) {
                    listener.onServiceAdded(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    private void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                for (DiscoveryProviderListener listener : serviceListeners) {
                    listener.onServiceRemoved(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    private void notifyListenersThatDiscoveryFailed(final ServiceCommandError error) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                for (DiscoveryProviderListener listener : serviceListeners) {
                    listener.onServiceDiscoveryFailed(FireTVDiscoveryProvider.this, error);
                }
            }
        });
    }


    class FireTVDiscoveryListener implements DiscoveryController.IDiscoveryListener {

        @Override
        public void playerDiscovered(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer == null) {
                return;
            }
            String uid = remoteMediaPlayer.getUniqueIdentifier();
            ServiceDescription serviceDescription = foundServices.get(uid);

            if (serviceDescription == null) {
                serviceDescription = new ServiceDescription();
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
                foundServices.put(uid, serviceDescription);
                notifyListenersThatServiceAdded(serviceDescription);
            } else {
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
            }
        }

        @Override
        public void playerLost(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer == null) {
                return;
            }
            ServiceDescription serviceDescription
                    = foundServices.get(remoteMediaPlayer.getUniqueIdentifier());
            if (serviceDescription != null) {
                notifyListenersThatServiceLost(serviceDescription);
                foundServices.remove(remoteMediaPlayer.getUniqueIdentifier());
            }
        }

        @Override
        public void discoveryFailure() {
            final ServiceCommandError error = new ServiceCommandError("FireTV discovery failure");
            notifyListenersThatDiscoveryFailed(error);
        }

        private void updateServiceDescription(ServiceDescription serviceDescription,
                                              RemoteMediaPlayer remoteMediaPlayer) {
            String uid = remoteMediaPlayer.getUniqueIdentifier();
            serviceDescription.setDevice(remoteMediaPlayer);
            serviceDescription.setFriendlyName(remoteMediaPlayer.getName());
            serviceDescription.setIpAddress(uid);
            serviceDescription.setServiceID(FireTVService.ID);
            serviceDescription.setUUID(uid);
        }

    }

}*/
