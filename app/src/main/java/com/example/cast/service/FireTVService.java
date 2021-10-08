package com.example.cast.service;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerInfo;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.example.cast.core.ImageInfo;
import com.example.cast.core.MediaInfo;
import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.service.capability.CapabilityMethods;
import com.example.cast.service.capability.MediaControl;
import com.example.cast.service.capability.MediaPlayer;
import com.example.cast.service.capability.listeners.ResponseListener;
import com.example.cast.service.command.FireTVServiceError;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.command.ServiceSubscription;
import com.example.cast.service.config.ServiceConfig;
import com.example.cast.service.config.ServiceDescription;
import com.example.cast.service.sessions.LaunchSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FireTVService extends DeviceService implements MediaControl, MediaPlayer {

    /* renamed from: ID */
    public static final String ID = "FireTV";
    private static final String META_DESCRIPTION = "description";
    private static final String META_ICON_IMAGE = "poster";
    private static final String META_KIND = "kind";
    private static final String META_LABEL = "label";
    private static final String META_MIME_TYPE = "type";
    private static final String META_NOREPLAY = "noreplay";
    private static final String META_SRC = "src";
    private static final String META_SRCLANG = "srclang";
    private static final String META_TITLE = "title";
    private static final String META_TRACKS = "tracks";
    private PlayStateSubscription playStateSubscription;
    private final RemoteMediaPlayer remoteMediaPlayer;

    /* access modifiers changed from: private */
    public interface ConvertResult<Response, Result> {
        Response convert(Result result) throws JSONException;
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public MediaControl getMediaControl() {
        return this;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    public FireTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        if (serviceDescription == null || !(serviceDescription.getDevice() instanceof RemoteMediaPlayer)) {
            this.remoteMediaPlayer = null;
        } else {
            this.remoteMediaPlayer = (RemoteMediaPlayer) serviceDescription.getDevice();
        }
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, ID);
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public void connect() {
        super.connect();
        if (this.remoteMediaPlayer != null) {
            this.connected = true;
            reportConnected(this.connected);
        }
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public boolean isConnected() {
        return this.connected;
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public void disconnect() {
        super.disconnect();
        PlayStateSubscription playStateSubscription2 = this.playStateSubscription;
        if (playStateSubscription2 != null) {
            playStateSubscription2.unsubscribe();
            this.playStateSubscription = null;
        }
        this.connected = false;
    }

    /* access modifiers changed from: protected */
    @Override // com.inshot.cast.core.service.DeviceService
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(MediaPlayer.MediaInfo_Get);
        arrayList.add(MediaPlayer.Display_Image);
        arrayList.add("MediaPlayer.Play.Audio");
        arrayList.add("MediaPlayer.Play.Video");
        arrayList.add(MediaPlayer.Close);
        arrayList.add(MediaPlayer.MetaData_MimeType);
        arrayList.add(MediaPlayer.MetaData_Thumbnail);
        arrayList.add(MediaPlayer.MetaData_Title);
        arrayList.add(MediaPlayer.Subtitle_WebVTT);
        arrayList.add(MediaControl.Play);
        arrayList.add(MediaControl.Pause);
        arrayList.add(MediaControl.Stop);
        arrayList.add(MediaControl.Seek);
        arrayList.add(MediaControl.Duration);
        arrayList.add(MediaControl.Position);
        arrayList.add(MediaControl.PlayState);
        arrayList.add(MediaControl.PlayState_Subscribe);
        setCapabilities(arrayList);
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls != null) {
            if (cls.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            }
            if (cls.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            }
        }
        return CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void getMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        try {
            handleAsyncFutureWithConversion(mediaInfoListener, this.remoteMediaPlayer.getMediaInfo(), new ConvertResult<MediaInfo, MediaPlayerInfo>() {
                /* class com.inshot.cast.core.service.FireTVService.C58121 */

                public MediaInfo convert(MediaPlayerInfo mediaPlayerInfo) throws JSONException {
                    ArrayList arrayList;
                    JSONObject jSONObject = new JSONObject(mediaPlayerInfo.getMetadata());
                    if (jSONObject.has(FireTVService.META_ICON_IMAGE)) {
                        ArrayList arrayList2 = new ArrayList();
                        arrayList2.add(new ImageInfo(jSONObject.getString(FireTVService.META_ICON_IMAGE)));
                        arrayList = arrayList2;
                    } else {
                        arrayList = null;
                    }
                    return new MediaInfo(mediaPlayerInfo.getSource(), jSONObject.getString("type"), jSONObject.getString("title"), jSONObject.getString("description"), arrayList);
                }
            }, "Error getting media info");
        } catch (Exception unused) {
            Util.postError(mediaInfoListener, new FireTVServiceError("Error getting media info"));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        Util.postError(mediaInfoListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void displayImage(String str, String str2, String str3, String str4, String str5, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        stop(responseListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void playMedia(MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public CapabilityMethods.CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void play(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.play(), "Error playing");
        } catch (Exception e) {
            Util.postError(responseListener, new FireTVServiceError("Error playing", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void pause(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.pause(), "Error pausing");
        } catch (Exception e) {
            Util.postError(responseListener, new FireTVServiceError("Error pausing", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void stop(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.stop(), "Error stopping");
        } catch (Exception e) {
            Util.postError(responseListener, new FireTVServiceError("Error stopping", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void rewind(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void fastForward(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void previous(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void next(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void seek(long j, ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.seek(CustomMediaPlayer.PlayerSeekMode.Absolute, j), "Error seeking");
        } catch (Exception e) {
            Util.postError(responseListener, new FireTVServiceError("Error seeking", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void getDuration(MediaControl.DurationListener durationListener) {
        try {
            handleAsyncFuture(durationListener, this.remoteMediaPlayer.getDuration(), "Error getting duration");
        } catch (Exception e) {
            Util.postError(durationListener, new FireTVServiceError("Error getting duration", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void getPosition(MediaControl.PositionListener positionListener) {
        try {
            handleAsyncFuture(positionListener, this.remoteMediaPlayer.getPosition(), "Error getting position");
        } catch (Exception e) {
            Util.postError(positionListener, new FireTVServiceError("Error getting position", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void getPlayState(MediaControl.PlayStateListener playStateListener) {
        try {
            handleAsyncFutureWithConversion(playStateListener, this.remoteMediaPlayer.getStatus(), new ConvertResult<MediaControl.PlayStateStatus, MediaPlayerStatus>() {
                /* class com.inshot.cast.core.service.FireTVService.C58132 */

                public MediaControl.PlayStateStatus convert(MediaPlayerStatus mediaPlayerStatus) {
                    return FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus);
                }
            }, "Error getting play state");
        } catch (Exception e) {
            Util.postError(playStateListener, new FireTVServiceError("Error getting play state", e));
        }
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener playStateListener) {
        PlayStateSubscription playStateSubscription2 = this.playStateSubscription;
        if (playStateSubscription2 == null) {
            this.playStateSubscription = new PlayStateSubscription(playStateListener);
            Util.runInBackground(new Runnable() {
                /* class com.inshot.cast.core.service.FireTVService.RunnableC58143 */

                public void run() {
                    FireTVService.this.remoteMediaPlayer.addStatusListener(FireTVService.this.playStateSubscription);
                }
            });
        } else if (!playStateSubscription2.getListeners().contains(playStateListener)) {
            this.playStateSubscription.addListener(playStateListener);
        }
        getPlayState(playStateListener);
        return this.playStateSubscription;
    }

    /* access modifiers changed from: package-private */
    public MediaControl.PlayStateStatus createPlayStateStatusFromFireTVStatus(MediaPlayerStatus mediaPlayerStatus) {
        MediaControl.PlayStateStatus playStateStatus = MediaControl.PlayStateStatus.Unknown;
        if (mediaPlayerStatus == null) {
            return playStateStatus;
        }
        switch (mediaPlayerStatus.getState()) {
            case PreparingMedia:
                return MediaControl.PlayStateStatus.Buffering;
            case Playing:
                return MediaControl.PlayStateStatus.Playing;
            case Paused:
                return MediaControl.PlayStateStatus.Paused;
            case Finished:
                return MediaControl.PlayStateStatus.Finished;
            case NoSource:
                return MediaControl.PlayStateStatus.Idle;
            default:
                return playStateStatus;
        }
    }

    private String getMetadata(MediaInfo mediaInfo) throws JSONException {
        ImageInfo imageInfo;
        JSONObject jSONObject = new JSONObject();
        if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
            jSONObject.put("title", mediaInfo.getTitle());
        }
        if (mediaInfo.getDescription() != null && !mediaInfo.getDescription().isEmpty()) {
            jSONObject.put("description", mediaInfo.getDescription());
        }
        jSONObject.put("type", mediaInfo.getMimeType());
        if (!(mediaInfo.getImages() == null || mediaInfo.getImages().size() <= 0 || (imageInfo = mediaInfo.getImages().get(0)) == null || imageInfo.getUrl() == null || imageInfo.getUrl().isEmpty())) {
            jSONObject.put(META_ICON_IMAGE, imageInfo.getUrl());
        }
        jSONObject.put("noreplay", true);
        if (mediaInfo.getSubtitleInfo() != null) {
            JSONArray jSONArray = new JSONArray();
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("kind", "subtitles");
            jSONObject2.put("src", mediaInfo.getSubtitleInfo().getUrl());
            String label = mediaInfo.getSubtitleInfo().getLabel();
            if (label == null) {
                label = "";
            }
            jSONObject2.put("label", label);
            String language = mediaInfo.getSubtitleInfo().getLanguage();
            if (language == null) {
                language = "";
            }
            jSONObject2.put("srclang", language);
            jSONArray.put(jSONObject2);
            jSONObject.put("tracks", jSONArray);
        }
        return jSONObject.toString();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private MediaPlayer.MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(this.remoteMediaPlayer.getUniqueIdentifier());
        launchSession.setAppName(this.remoteMediaPlayer.getName());
        return new MediaPlayer.MediaLaunchObject(launchSession, this);
    }

    private void setMediaSource(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        try {
            handleAsyncFutureWithConversion(launchListener, this.remoteMediaPlayer.setMediaSource(mediaInfo.getUrl(), getMetadata(mediaInfo), true, false), new ConvertResult<MediaPlayer.MediaLaunchObject, Void>() {
                /* class com.inshot.cast.core.service.FireTVService.C58154 */

                public MediaPlayer.MediaLaunchObject convert(Void r1) {
                    return FireTVService.this.createMediaLaunchObject();
                }
            }, "Error setting media source");
        } catch (Exception e) {
            Util.postError(launchListener, new FireTVServiceError("Error setting media source", e));
        }
    }

    private void handleVoidAsyncFuture(ResponseListener<Object> responseListener, RemoteMediaPlayer.AsyncFuture<Void> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<Object, Void>() {
            /* class com.inshot.cast.core.service.FireTVService.C58165 */

            public Object convert(Void r1) {
                return r1;
            }
        }, str);
    }

    private <T> void handleAsyncFuture(ResponseListener<T> responseListener, RemoteMediaPlayer.AsyncFuture<T> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<T, T>() {
            /* class com.inshot.cast.core.service.FireTVService.C58176 */

            @Override // com.inshot.cast.core.service.FireTVService.ConvertResult
            public T convert(T t) {
                return t;
            }
        }, str);
    }

    private <Response, Result> void handleAsyncFutureWithConversion(final ResponseListener<Response> responseListener, RemoteMediaPlayer.AsyncFuture<Result> asyncFuture, final ConvertResult<Response, Result> convertResult, final String str) {
        if (asyncFuture != null) {
            asyncFuture.getAsync(new RemoteMediaPlayer.FutureListener<Result>() {
                /* class com.inshot.cast.core.service.FireTVService.C58187 */

                @Override // com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer.FutureListener
                public void futureIsNow(Future<Result> future) {
                    try {
                        Util.postSuccess(responseListener, convertResult.convert(future.get()));
                    } catch (ExecutionException e) {
                        Util.postError(responseListener, new FireTVServiceError(str, e.getCause()));
                    } catch (Exception e2) {
                        Util.postError(responseListener, new FireTVServiceError(str, e2));
                    }
                }
            });
        } else {
            Util.postError(responseListener, new FireTVServiceError(str));
        }
    }

    private static abstract class Subscription<Status, Listener extends ResponseListener<Status>> implements ServiceSubscription<Listener> {
        List<Listener> listeners = new ArrayList();
        Status prevStatus;

        public Subscription(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
        }

        /* access modifiers changed from: package-private */
        public synchronized void notifyListeners(final Status status) {
            if (!status.equals(this.prevStatus)) {
                Util.runOnUI(new Runnable() {
                    /* class com.inshot.cast.core.service.FireTVService.Subscription.RunnableC58211 */

                    public void run() {
                        for (Listener listener : Subscription.this.listeners) {
                            listener.onSuccess(status);
                        }
                    }
                });
                this.prevStatus = status;
            }
        }

        public Listener addListener(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return listener;
        }

        public void removeListener(Listener listener) {
            this.listeners.remove(listener);
        }

        @Override // com.inshot.cast.core.service.command.ServiceSubscription
        public List<Listener> getListeners() {
            return this.listeners;
        }
    }

    /* access modifiers changed from: package-private */
    public class PlayStateSubscription extends Subscription<MediaControl.PlayStateStatus, MediaControl.PlayStateListener> implements CustomMediaPlayer.StatusListener {
        public PlayStateSubscription(MediaControl.PlayStateListener playStateListener) {
            super(playStateListener);
        }

        @Override // com.amazon.whisperplay.fling.media.service.CustomMediaPlayer.StatusListener
        public void onStatusChange(MediaPlayerStatus mediaPlayerStatus, long j) {
            notifyListeners(FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus));
        }

        @Override // com.inshot.cast.core.service.command.ServiceSubscription
        public void unsubscribe() {
            Util.runInBackground(new Runnable() {
                /* class com.inshot.cast.core.service.FireTVService.PlayStateSubscription.RunnableC58201 */

                public void run() {
                    if (FireTVService.this.remoteMediaPlayer != null) {
                        try {
                            FireTVService.this.remoteMediaPlayer.removeStatusListener(PlayStateSubscription.this);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        FireTVService.this.playStateSubscription = null;
                    }
                }
            });
        }
    }
}

/*

import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerInfo;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.example.cast.core.ImageInfo;
import com.example.cast.core.MediaInfo;
import com.example.cast.core.Util;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.service.capability.CapabilityMethods;
import com.example.cast.service.capability.MediaControl;
import com.example.cast.service.capability.MediaPlayer;
import com.example.cast.service.capability.listeners.ResponseListener;
import com.example.cast.service.command.FireTVServiceError;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.command.ServiceSubscription;
import com.example.cast.service.config.ServiceConfig;
import com.example.cast.service.config.ServiceDescription;
import com.example.cast.service.sessions.LaunchSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

*/
/**
 * FireTVService provides capabilities for FireTV devices. FireTVService acts as a layer on top of
 * Fling SDK, and requires the Fling SDK library to function. FireTVService provides the following
 * functionality:
 * - Media playback
 * - Media control
 *
 * Using Connect SDK for discovery/control of FireTV devices will result in your app complying with
 * the Fling SDK terms of service.
 *//*

public class FireTVService extends DeviceService implements MediaPlayer, MediaControl {

    public static final String ID = "FireTV";

    private static final String META_TITLE = "title";
    private static final String META_DESCRIPTION = "description";
    private static final String META_MIME_TYPE = "type";
    private static final String META_ICON_IMAGE = "poster";
    private static final String META_NOREPLAY = "noreplay";
    private static final String META_TRACKS = "tracks";
    private static final String META_SRC = "src";
    private static final String META_KIND = "kind";
    private static final String META_SRCLANG = "srclang";
    private static final String META_LABEL = "label";

    private final RemoteMediaPlayer remoteMediaPlayer;
    private PlayStateSubscription playStateSubscription;

    public FireTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        if (serviceDescription != null
                && serviceDescription.getDevice() instanceof RemoteMediaPlayer) {
            this.remoteMediaPlayer = (RemoteMediaPlayer) serviceDescription.getDevice();
        } else {
            this.remoteMediaPlayer = null;
        }
    }

    */
/**
     * Get filter instance for this service which contains a name of service and id. It is used in
     * discovery process
     *//*

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "FireTV");
    }

    */
/**
     * Prepare a service for usage
     *//*

    @Override
    public void connect() {
        super.connect();
        if (remoteMediaPlayer != null) {
            connected = true;
            reportConnected(connected);
        }
    }

    */
/**
     * Check if service is ready
     *//*

    @Override
    public boolean isConnected() {
        return connected;
    }

    */
/**
     * Check if service implements connect/disconnect methods
     *//*

    @Override
    public boolean isConnectable() {
        return true;
    }

    */
/**
     * Disconnect a service and close all subscriptions
     *//*

    @Override
    public void disconnect() {
        super.disconnect();
        if (playStateSubscription != null) {
            playStateSubscription.unsubscribe();
            playStateSubscription = null;
        }
        connected = false;
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();
        capabilities.add(MediaPlayer.MediaInfo_Get);
        capabilities.add(MediaPlayer.Display_Image);
        capabilities.add("MediaPlayer.Play.Audio");
        capabilities.add("MediaPlayer.Play.Video");
        capabilities.add(MediaPlayer.Close);
        capabilities.add(MediaPlayer.MetaData_MimeType);
        capabilities.add(MediaPlayer.MetaData_Thumbnail);
        capabilities.add(MediaPlayer.MetaData_Title);
        capabilities.add(MediaPlayer.Subtitle_WebVTT);

        capabilities.add(MediaControl.Play);
        capabilities.add(MediaControl.Pause);
        capabilities.add(MediaControl.Stop);
        capabilities.add(MediaControl.Seek);
        capabilities.add(MediaControl.Duration);
        capabilities.add(MediaControl.Position);
        capabilities.add(MediaControl.PlayState);
        capabilities.add(MediaControl.PlayState_Subscribe);

        setCapabilities(capabilities);
    }

    */
/**
     * Get a priority level for a particular capability
     *//*

    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz != null) {
            if (clazz.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            } else if (clazz.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            }
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }


    */
/**
     * Get MediaPlayer implementation
     *//*

    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    */
/**
     * Get MediaPlayer priority level
     *//*

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    */
/**
     * Get MediaInfo available only during playback otherwise returns an error
     * @param listener
     *//*

    @Override
    public void getMediaInfo(final MediaInfoListener listener) {
//        final String error = "Error getting media info";
//        RemoteMediaPlayer.AsyncFuture<MediaPlayerInfo> asyncFuture = null;
//        try {
//            asyncFuture = remoteMediaPlayer.getMediaInfo();
//            handleAsyncFutureWithConversion(listener, asyncFuture,
//                    new ConvertResult<MediaInfo, MediaPlayerInfo>() {
//                        @Override
//                        public MediaInfo convert(MediaPlayerInfo data) throws JSONException {
//                            JSONObject metaJson = null;
//                            metaJson = new JSONObject(data.getMetadata());
//                            List<ImageInfo> images = null;
//                            if (metaJson.has(META_ICON_IMAGE)) {
//                                images = new ArrayList<ImageInfo>();
//                                images.add(new ImageInfo(metaJson.getString(META_ICON_IMAGE)));
//                            }
//                            MediaInfo mediaInfo = new MediaInfo(data.getSource(),
//                                    metaJson.getString(META_MIME_TYPE), metaJson.getString(META_TITLE),
//                                    metaJson.getString(META_DESCRIPTION), images);
//                            return mediaInfo;
//                        }
//                    }, error);
//        } catch (Exception e) {
//            Util.postError(listener, new FireTVServiceError(error));
//            return;
//        }

        try {
            handleAsyncFutureWithConversion(listener, this.remoteMediaPlayer.getMediaInfo(), new ConvertResult<MediaInfo, MediaPlayerInfo>() {
                */
/* class com.inshot.cast.core.service.FireTVService.C58121 *//*


                public MediaInfo convert(MediaPlayerInfo mediaPlayerInfo) throws JSONException {
                    ArrayList arrayList;
                    JSONObject jSONObject = new JSONObject(mediaPlayerInfo.getMetadata());
                    if (jSONObject.has(FireTVService.META_ICON_IMAGE)) {
                        ArrayList arrayList2 = new ArrayList();
                        arrayList2.add(new ImageInfo(jSONObject.getString(FireTVService.META_ICON_IMAGE)));
                        arrayList = arrayList2;
                    } else {
                        arrayList = null;
                    }
                    return new MediaInfo(mediaPlayerInfo.getSource(), jSONObject.getString("type"), jSONObject.getString("title"), jSONObject.getString("description"), arrayList);
                }
            }, "Error getting media info");
        } catch (Exception unused) {
            Util.postError(listener, new FireTVServiceError("Error getting media info"));
        }
    }

    */
/**
     * Not supported
     *//*

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
        return null;
    }

    */
/**
     * Display an image with metadata
     * @param url media source
     * @param mimeType
     * @param title
     * @param description
     * @param iconSrc
     * @param listener
     *//*

    @Override
    public void displayImage(String url, String mimeType, String title, String description,
                             String iconSrc, final LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    */
/**
     * Play audio/video
     * @param url media source
     * @param mimeType
     * @param title
     * @param description
     * @param iconSrc
     * @param shouldLoop skipped in current implementation
     * @param listener
     *//*

    @Override
    public void playMedia(String url, String mimeType, String title, String description,
                          String iconSrc, boolean shouldLoop, LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    */
/**
     * Stop and close media player on FireTV. In current implementation it's similar to stop method
     * @param launchSession
     * @param listener
     *//*

    @Override
    public void closeMedia(LaunchSession launchSession, final ResponseListener<Object> listener) {
        stop(listener);
    }

    */
/**
     * Display an image with metadata
     * @param mediaInfo
     * @param listener
     *//*

    @Override
    public void displayImage(MediaInfo mediaInfo, LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    */
/**
     * Play audio/video
     * @param mediaInfo
     * @param shouldLoop skipped in current implementation
     * @param listener
     *//*

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop, LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    */
/**
     * Get MediaControl capability. It should be used only during media playback.
     *//*

    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    */
/**
     * Get MediaControl priority level
     *//*

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    */
/**
     * Play current media.
     *//*

    @Override
    public void play(ResponseListener<Object> listener) {
        final String error = "Error playing";
        RemoteMediaPlayer.AsyncFuture<Void> asyncFuture = null;
        try {
            asyncFuture = remoteMediaPlayer.play();
            handleVoidAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
        }
    }

    */
/**
     * Pause current media.
     *//*

    @Override
    public void pause(ResponseListener<Object> listener) {
        final String error = "Error pausing";
        RemoteMediaPlayer.AsyncFuture<Void> asyncFuture = null;
        try {
            asyncFuture = remoteMediaPlayer.pause();
            handleVoidAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
        }
    }

    */
/**
     * Stop current media and close FireTV application.
     *//*

    @Override
    public void stop(ResponseListener<Object> listener) {
        final String error = "Error stopping";
        RemoteMediaPlayer.AsyncFuture<Void> asyncFuture = null;
        try {
            asyncFuture = remoteMediaPlayer.stop();
            handleVoidAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
        }
    }

    */
/**
     * Not supported
     *//*

    @Override
    public void rewind(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    */
/**
     * Not supported
     *//*

    @Override
    public void fastForward(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    */
/**
     * Not supported
     *//*

    @Override
    public void previous(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    */
/**
     * Not supported
     *//*

    @Override
    public void next(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    */
/**
     * Seek current media.
     * @param position time in milliseconds
     * @param listener
     *//*

    @Override
    public void seek(long position, ResponseListener<Object> listener) {
        final String error = "Error seeking";
        RemoteMediaPlayer.AsyncFuture<Void> asyncFuture = null;
        try {
            asyncFuture = remoteMediaPlayer.seek(CustomMediaPlayer.PlayerSeekMode.Absolute,
                    position);
            handleVoidAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
        }
    }

    */
/**
     * Get current media duration.
     *//*

    @Override
    public void getDuration(final DurationListener listener) {
        final String error = "Error getting duration";
        RemoteMediaPlayer.AsyncFuture<Long> asyncFuture;
        try {
            asyncFuture = remoteMediaPlayer.getDuration();
            handleAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
            return;
        }
    }

    */
/**
     * Get playback position
     *//*

    @Override
    public void getPosition(final PositionListener listener) {
        final String error = "Error getting position";
        RemoteMediaPlayer.AsyncFuture<Long> asyncFuture;
        try {
            asyncFuture = remoteMediaPlayer.getPosition();
            handleAsyncFuture(listener, asyncFuture, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
            return;
        }
    }

    */
/**
     * Get playback state
     *//*

    @Override
    public void getPlayState(final PlayStateListener listener) {
        final String error = "Error getting play state";
        RemoteMediaPlayer.AsyncFuture<MediaPlayerStatus> asyncFuture;
        try {
            asyncFuture = remoteMediaPlayer.getStatus();
            handleAsyncFutureWithConversion(listener, asyncFuture,
                    new ConvertResult<PlayStateStatus, MediaPlayerStatus>() {
                        @Override
                        public PlayStateStatus convert(MediaPlayerStatus data) {
                            return createPlayStateStatusFromFireTVStatus(data);
                        }
                    }, error);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
            return;
        }
    }

    */
/**
     * Subscribe to playback state. Only single instance of subscription is available. Each new
     * call returns the same subscription object.
     *//*

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(
            final PlayStateListener listener) {

        if (playStateSubscription == null) {
            playStateSubscription = new PlayStateSubscription(listener);
            remoteMediaPlayer.addStatusListener(playStateSubscription);
        } else if (!playStateSubscription.getListeners().contains(listener)) {
            playStateSubscription.addListener(listener);
        }
        getPlayState(listener);
        return playStateSubscription;
    }

    PlayStateStatus createPlayStateStatusFromFireTVStatus(MediaPlayerStatus status) {
        PlayStateStatus playState = PlayStateStatus.Unknown;
        if (status != null) {
            switch (status.getState()) {
                case PreparingMedia:
                    playState = PlayStateStatus.Buffering;
                    break;
                case Playing:
                    playState = PlayStateStatus.Playing;
                    break;
                case Paused:
                    playState = PlayStateStatus.Paused;
                    break;
                case Finished:
                    playState = PlayStateStatus.Finished;
                    break;
                case NoSource:
                    playState = PlayStateStatus.Idle;
            }
        }
        return playState;
    }

    private String getMetadata(MediaInfo mediaInfo)
            throws JSONException {
        JSONObject json = new JSONObject();
        if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
            json.put(META_TITLE, mediaInfo.getTitle());
        }
        if (mediaInfo.getDescription() != null && !mediaInfo.getDescription().isEmpty()) {
            json.put(META_DESCRIPTION, mediaInfo.getDescription());
        }
        json.put(META_MIME_TYPE, mediaInfo.getMimeType());
        if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
            ImageInfo image = mediaInfo.getImages().get(0);
            if (image != null) {
                if (image.getUrl() != null && !image.getUrl().isEmpty()) {
                    json.put(META_ICON_IMAGE, image.getUrl());
                }
            }
        }
        json.put(META_NOREPLAY, true);
        if (mediaInfo.getSubtitleInfo() != null) {
            JSONArray tracksArray = new JSONArray();
            JSONObject trackObj = new JSONObject();
            trackObj.put(META_KIND, "subtitles");
            trackObj.put(META_SRC, mediaInfo.getSubtitleInfo().getUrl());
            String label = mediaInfo.getSubtitleInfo().getLabel();
            trackObj.put(META_LABEL, label == null ? "" : label);
            String language = mediaInfo.getSubtitleInfo().getLanguage();
            trackObj.put(META_SRCLANG, language == null ? "" : language);
            tracksArray.put(trackObj);
            json.put(META_TRACKS, tracksArray);
        }
        return json.toString();
    }

    private MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(remoteMediaPlayer.getUniqueIdentifier());
        launchSession.setAppName(remoteMediaPlayer.getName());
        MediaLaunchObject mediaLaunchObject = new MediaLaunchObject(launchSession, this);
        return mediaLaunchObject;
    }

    private void setMediaSource(MediaInfo mediaInfo, final LaunchListener listener) {
        final String error = "Error setting media source";
        RemoteMediaPlayer.AsyncFuture<Void> asyncFuture = null;
        try {
            final String metadata = getMetadata(mediaInfo);
            asyncFuture = remoteMediaPlayer.setMediaSource(mediaInfo.getUrl(), metadata, true, false);
        } catch (Exception e) {
            Util.postError(listener, new FireTVServiceError(error, e));
            return;
        }
        handleAsyncFutureWithConversion(listener, asyncFuture,
                new ConvertResult<MediaLaunchObject, Void>() {
                    @Override
                    public MediaLaunchObject convert(Void data) {
                        return createMediaLaunchObject();
                    }
                }, error);
    }

    private void handleVoidAsyncFuture(final ResponseListener<Object> listener,
                                       final RemoteMediaPlayer.AsyncFuture<Void> asyncFuture,
                                       final String errorMessage) {
        handleAsyncFutureWithConversion(listener, asyncFuture, new ConvertResult<Object, Void>() {
            @Override
            public Object convert(Void data) {
                return data;
            }
        }, errorMessage);
    }

    private <T> void handleAsyncFuture(final ResponseListener<T> listener,
                                       final RemoteMediaPlayer.AsyncFuture<T> asyncFuture,
                                       final String errorMessage) {
        handleAsyncFutureWithConversion(listener, asyncFuture, new ConvertResult<T, T>() {
            @Override
            public T convert(T data) {
                return data;
            }
        }, errorMessage);
    }

    private <Response, Result> void handleAsyncFutureWithConversion(
            final ResponseListener<Response> listener,
            final RemoteMediaPlayer.AsyncFuture<Result> asyncFuture,
            final ConvertResult<Response, Result> conversion,
            final String errorMessage) {
        if (asyncFuture != null) {
            asyncFuture.getAsync(new RemoteMediaPlayer.FutureListener<Result>() {
                @Override
                public void futureIsNow(Future<Result> future) {
                    try {
                        Result result = future.get();
                        Util.postSuccess(listener, conversion.convert(result));
                    } catch (ExecutionException e) {
                        Util.postError(listener, new FireTVServiceError(errorMessage,
                                e.getCause()));
                    } catch (Exception e) {
                        Util.postError(listener, new FireTVServiceError(errorMessage, e));
                    }
                }
            });
        } else {
            Util.postError(listener, new FireTVServiceError(errorMessage));
        }
    }

    private interface ConvertResult<Response, Result> {
        Response convert(Result data) throws Exception;
    }

    private abstract static class Subscription<Status, Listener extends ResponseListener<Status>>
            implements ServiceSubscription<Listener> {

        List<Listener> listeners = new ArrayList<Listener>();

        Status prevStatus;

        public Subscription(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
        }

        synchronized void notifyListeners(final Status status) {
            if (!status.equals(prevStatus)) {
                Util.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        for (Listener listener : listeners) {
                            listener.onSuccess(status);
                        }
                    }
                });
                prevStatus = status;
            }
        }

        @Override
        public Listener addListener(Listener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
            return listener;
        }

        @Override
        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        @Override
        public List<Listener> getListeners() {
            return listeners;
        }
    }

    */
/**
     * Internal play state subscription implementation
     *//*

    class PlayStateSubscription extends Subscription<PlayStateStatus, PlayStateListener>
            implements CustomMediaPlayer.StatusListener {

        public PlayStateSubscription(PlayStateListener listener) {
            super(listener);
        }

        @Override
        public void onStatusChange(MediaPlayerStatus mediaPlayerStatus, long position) {
            final PlayStateStatus status = createPlayStateStatusFromFireTVStatus(mediaPlayerStatus);
            notifyListeners(status);
        }

        @Override
        public void unsubscribe() {
            remoteMediaPlayer.removeStatusListener(this);
            playStateSubscription = null;
        }

    }

}*/
