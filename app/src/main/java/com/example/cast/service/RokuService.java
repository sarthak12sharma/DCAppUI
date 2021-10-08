/*
 * RokuService
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 26 Feb 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cast.service;

import android.text.TextUtils;
import android.util.Log;

import com.amazon.whisperlink.util.WhisperLinkUtil;
import com.example.cast.core.AndroidAppInfo;
import com.example.cast.core.MediaInfo;
import com.example.cast.core.Util;
import com.example.cast.device.ConnectableDevice;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryManager;
import com.example.cast.etc.helper.DeviceServiceReachability;
import com.example.cast.etc.helper.HttpConnection;
import com.example.cast.etc.helper.HttpMessage;
import com.example.cast.service.capability.CapabilityMethods;
import com.example.cast.service.capability.KeyControl;
import com.example.cast.service.capability.Launcher;
import com.example.cast.service.capability.MediaControl;
import com.example.cast.service.capability.MediaPlayer;
import com.example.cast.service.capability.TextInputControl;
import com.example.cast.service.capability.listeners.ResponseListener;
import com.example.cast.service.command.NotSupportedServiceSubscription;
import com.example.cast.service.command.ServiceCommand;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.command.ServiceSubscription;
import com.example.cast.service.command.URLServiceSubscription;
import com.example.cast.service.config.ServiceConfig;
import com.example.cast.service.config.ServiceDescription;
import com.example.cast.service.roku.RokuApplicationListParser;
import com.example.cast.service.sessions.LaunchSession;
import com.integralads.avid.library.mopub.session.internal.InternalAvidAdSessionContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RokuService extends DeviceService implements KeyControl, Launcher, MediaControl, MediaPlayer, TextInputControl {

    /* renamed from: ID */
    public static final String ID = "Roku";
    private static List<String> registeredApps = new ArrayList();
    DIALService dialService;

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public KeyControl getKeyControl() {
        return this;
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public Launcher getLauncher() {
        return this;
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public MediaControl getMediaControl() {
        return this;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    @Override // com.inshot.cast.core.service.DeviceService, com.inshot.cast.core.service.command.ServiceCommand.ServiceCommandProcessor
    public void unsubscribe(URLServiceSubscription<?> uRLServiceSubscription) {
    }

    static {
        registeredApps.add("YouTube");
        registeredApps.add("Netflix");
        registeredApps.add("Amazon");
    }

    public static void registerApp(String str) {
        if (!registeredApps.contains(str)) {
            registeredApps.add(str);
        }
    }

    public RokuService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);
        if (this.serviceDescription != null) {
            this.serviceDescription.setPort(8060);
        }
        probeForAppSupport();
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "roku:ecp");
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        }
        if (cls.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        }
        if (cls.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        }
        if (cls.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        }
        if (cls.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        }
        return CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public CapabilityMethods.CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    class RokuLaunchSession extends LaunchSession {
        RokuLaunchSession() {
        }

        @Override // com.inshot.cast.core.service.sessions.LaunchSession
        public void close(ResponseListener<Object> responseListener) {
            RokuService.this.home(responseListener);
        }
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchApp(String str, Launcher.AppLaunchListener appLaunchListener) {
        if (str == null) {
            Util.postError(appLaunchListener, new ServiceCommandError(0, "Must supply a valid app id", null));
            return;
        }
        AndroidAppInfo androidAppInfo = new AndroidAppInfo();
        androidAppInfo.setId(str);
        launchAppWithInfo(androidAppInfo, appLaunchListener);
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchAppWithInfo(AndroidAppInfo androidAppInfo, Launcher.AppLaunchListener appLaunchListener) {
        launchAppWithInfo(androidAppInfo, null, appLaunchListener);
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchAppWithInfo(final AndroidAppInfo androidAppInfo, Object obj, final Launcher.AppLaunchListener appLaunchListener) {
        String str;
        String str2 = null;
        String str3;
        if (androidAppInfo == null || androidAppInfo.getId() == null) {
            Util.postError(appLaunchListener, new ServiceCommandError(-1, "Cannot launch app without valid AndroidAppInfo object", androidAppInfo));
            return;
        }
        String requestURL = requestURL("launch", androidAppInfo.getId());
        String str4 = "";
        if (obj != null && (obj instanceof JSONObject)) {
            JSONObject jSONObject = (JSONObject) obj;
            int i = 0;
            Iterator<String> keys = jSONObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                try {
                    str = jSONObject.getString(next);
                } catch (JSONException unused) {
                    str = null;
                }
                if (str != null) {
                    String str5 = i == 0 ? "?" : "&";
                    try {
                        str3 = URLEncoder.encode(next, "UTF-8");
                        try {
                            str2 = URLEncoder.encode(str, "UTF-8");
                        } catch (UnsupportedEncodingException unused2) {
                        }
                    } catch (UnsupportedEncodingException unused3) {
                        str3 = null;
                        str2 = null;
                        str4 = str4 + (str5 + str3 + "=" + str2);
                        i++;
                    }
                    if (!(str3 == null || str2 == null)) {
                        str4 = str4 + (str5 + str3 + "=" + str2);
                        i++;
                    }
                }
            }
        }
        if (str4.length() > 0) {
            requestURL = requestURL + str4;
        }
        new ServiceCommand(this, requestURL, null, new ResponseListener<Object>() {
            /* class com.inshot.cast.core.service.RokuService.C58701 */

            @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                RokuLaunchSession rokuLaunchSession = new RokuLaunchSession();
                rokuLaunchSession.setService(RokuService.this);
                rokuLaunchSession.setAppId(androidAppInfo.getId());
                rokuLaunchSession.setAppName(androidAppInfo.getName());
                rokuLaunchSession.setSessionType(LaunchSession.LaunchSessionType.App);
                Util.postSuccess(appLaunchListener, rokuLaunchSession);
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appLaunchListener, serviceCommandError);
            }
        }).send();
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void closeApp(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        home(responseListener);
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void getAppList(final Launcher.AppListListener appListListener) {
        ServiceCommand serviceCommand = new ServiceCommand(this, requestURL("query", "apps"), null, new ResponseListener<Object>() {
            /* class com.inshot.cast.core.service.RokuService.C58742 */

            @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                String str = (String) obj;
                SAXParserFactory newInstance = SAXParserFactory.newInstance();
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str.getBytes("UTF-8"));
                    SAXParser newSAXParser = newInstance.newSAXParser();
                    RokuApplicationListParser rokuApplicationListParser = new RokuApplicationListParser();
                    newSAXParser.parse(byteArrayInputStream, rokuApplicationListParser);
                    Util.postSuccess(appListListener, rokuApplicationListParser.getApplicationList());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e2) {
                    e2.printStackTrace();
                } catch (SAXException e3) {
                    e3.printStackTrace();
                } catch (IOException e4) {
                    e4.printStackTrace();
                } catch (NullPointerException e5) {
                    e5.printStackTrace();
                }
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appListListener, serviceCommandError);
            }
        });
        serviceCommand.setHttpMethod(ServiceCommand.TYPE_GET);
        serviceCommand.send();
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void getRunningApp(Launcher.AppInfoListener appInfoListener) {
        Util.postError(appInfoListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public ServiceSubscription<Launcher.AppInfoListener> subscribeRunningApp(Launcher.AppInfoListener appInfoListener) {
        Util.postError(appInfoListener, ServiceCommandError.notSupported());
        return new NotSupportedServiceSubscription();
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void getAppState(LaunchSession launchSession, Launcher.AppStateListener appStateListener) {
        Util.postError(appStateListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public ServiceSubscription<Launcher.AppStateListener> subscribeAppState(LaunchSession launchSession, Launcher.AppStateListener appStateListener) {
        Util.postError(appStateListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchBrowser(String str, Launcher.AppLaunchListener appLaunchListener) {
        Util.postError(appLaunchListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchYouTube(String str, Launcher.AppLaunchListener appLaunchListener) {
        launchYouTube(str, 0.0f, appLaunchListener);
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchYouTube(String str, float f, Launcher.AppLaunchListener appLaunchListener) {
        if (getDIALService() != null) {
            getDIALService().getLauncher().launchYouTube(str, f, appLaunchListener);
        } else {
            Util.postError(appLaunchListener, new ServiceCommandError(0, "Cannot reach DIAL service for launching with provided start time", null));
        }
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchNetflix(final String str, final Launcher.AppLaunchListener appLaunchListener) {
        getAppList(new Launcher.AppListListener() {
            /* class com.inshot.cast.core.service.RokuService.C58753 */

            public void onSuccess(List<AndroidAppInfo> list) {
                for (AndroidAppInfo androidAppInfo : list) {
                    if (androidAppInfo.getName().equalsIgnoreCase("Netflix")) {
                        JSONObject jSONObject = new JSONObject();
                        try {
                            jSONObject.put(InternalAvidAdSessionContext.CONTEXT_MEDIA_TYPE, "movie");
                            if (str != null && str.length() > 0) {
                                jSONObject.put("contentId", str);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RokuService.this.launchAppWithInfo(androidAppInfo, jSONObject, appLaunchListener);
                        return;
                    }
                }
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appLaunchListener, serviceCommandError);
            }
        });
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchHulu(final String str, final Launcher.AppLaunchListener appLaunchListener) {
        getAppList(new Launcher.AppListListener() {
            /* class com.inshot.cast.core.service.RokuService.C58764 */

            public void onSuccess(List<AndroidAppInfo> list) {
                for (AndroidAppInfo androidAppInfo : list) {
                    if (androidAppInfo.getName().contains("Hulu")) {
                        JSONObject jSONObject = new JSONObject();
                        try {
                            jSONObject.put("contentId", str);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RokuService.this.launchAppWithInfo(androidAppInfo, jSONObject, appLaunchListener);
                        return;
                    }
                }
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appLaunchListener, serviceCommandError);
            }
        });
    }

    @Override // com.inshot.cast.core.service.capability.Launcher
    public void launchAppStore(final String str, Launcher.AppLaunchListener appLaunchListener) {
        JSONObject r1;
        AndroidAppInfo androidAppInfo = new AndroidAppInfo("11");
        androidAppInfo.setName("Channel Store");
        try {
            r1 = new JSONObject() {
                /* class com.inshot.cast.core.service.RokuService.C58775 */

                {
                    put("contentId", str);
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
            r1 = null;
        }
        launchAppWithInfo(androidAppInfo, r1, appLaunchListener);
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public CapabilityMethods.CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    /* renamed from: up */
    public void up(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Up"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void down(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Down"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void left(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Left"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void right(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Right"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    /* renamed from: ok */
    public void ok(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Select"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void back(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Back"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void home(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Home"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public CapabilityMethods.CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void play(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Play"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void pause(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Play"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void stop(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL(null, "input?a=sto"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void rewind(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Rev"), null, responseListener).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void fastForward(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, requestURL("keypress", "Fwd"), null, responseListener).send();
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
    public void getDuration(MediaControl.DurationListener durationListener) {
        Util.postError(durationListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void getPosition(MediaControl.PositionListener positionListener) {
        Util.postError(positionListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void seek(long j, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void getMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        Util.postError(mediaInfoListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        mediaInfoListener.onError(ServiceCommandError.notSupported());
        return null;
    }

    private void displayMedia(String str, String str2, String str3, String str4, String str5, final MediaPlayer.LaunchListener launchListener) {
        String str6;
        Log.i("fjldjfldjfs", "roku_info : url=" + str + "\nmimeType=" + str2 + "\ntitle=" + str3 + "\ndes=" + str4 + "\niconSrc=" + str5);
        ResponseListener r0 = new ResponseListener<Object>() {
            /* class com.inshot.cast.core.service.RokuService.C58786 */

            @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                RokuLaunchSession rokuLaunchSession = new RokuLaunchSession();
                rokuLaunchSession.setService(RokuService.this);
                rokuLaunchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
                Util.postSuccess(launchListener, new MediaPlayer.MediaLaunchObject(rokuLaunchSession, RokuService.this));
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }
        };
        String substring = str2.contains("/") ? str2.substring(str2.indexOf("/") + 1) : str2;
        if (str2.contains("image")) {
            str6 = String.format("15985?t=p&u=%s&tr=crossfade", HttpMessage.encode(str));
        } else if (str2.contains("video") || str2.contains("x-mpegurl")) {
            if (str2.contains("x-mpegurl")) {
                substring = "hls";
            }
            Object[] objArr = new Object[3];
            objArr[0] = HttpMessage.encode(str);
            objArr[1] = TextUtils.isEmpty(str3) ? "(null)" : HttpMessage.encode(str3);
            objArr[2] = HttpMessage.encode(substring);
            str6 = String.format("15985?t=v&u=%s&k=(null)&videoName=%s&videoFormat=%s", objArr);
        } else {
            Object[] objArr2 = new Object[5];
            objArr2[0] = HttpMessage.encode(str);
            objArr2[1] = TextUtils.isEmpty(str3) ? "(null)" : HttpMessage.encode(str3);
            objArr2[2] = TextUtils.isEmpty(str4) ? "(null)" : HttpMessage.encode(str4);
            objArr2[3] = HttpMessage.encode(Util.getSuffix(str3));
            objArr2[4] = TextUtils.isEmpty(str5) ? "(null)" : HttpMessage.encode(str5);
            str6 = String.format("15985?t=a&u=%s&k=(null)&h=(null)&songname=%s&artistname=%s&songformat=%s&albumarturl=%s", objArr2);
        }
        new ServiceCommand(this, requestURL("input", str6), null, r0).send();
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void displayImage(String str, String str2, String str3, String str4, String str5, MediaPlayer.LaunchListener launchListener) {
        displayMedia(str, str2, str3, str4, str5, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        if (mediaInfo != null) {
            String url = mediaInfo.getUrl();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() == null || mediaInfo.getImages().size() <= 0) {
                str = null;
                str5 = url;
                str4 = mimeType;
                str3 = title;
                str2 = description;
            } else {
                str = mediaInfo.getImages().get(0).getUrl();
                str5 = url;
                str4 = mimeType;
                str3 = title;
                str2 = description;
            }
        } else {
            str5 = null;
            str4 = null;
            str3 = null;
            str2 = null;
            str = null;
        }
        displayImage(str5, str4, str3, str2, str, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        displayMedia(str, str2, str3, str4, str5, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void playMedia(MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        if (mediaInfo != null) {
            String url = mediaInfo.getUrl();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() == null || mediaInfo.getImages().size() <= 0) {
                str = null;
                str5 = url;
                str4 = mimeType;
                str3 = title;
                str2 = description;
            } else {
                str = mediaInfo.getImages().get(0).getUrl();
                str5 = url;
                str4 = mimeType;
                str3 = title;
                str2 = description;
            }
        } else {
            str5 = null;
            str4 = null;
            str3 = null;
            str2 = null;
            str = null;
        }
        playMedia(str5, str4, str3, str2, str, z, launchListener);
    }

    @Override // com.inshot.cast.core.service.capability.MediaPlayer
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        home(responseListener);
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public CapabilityMethods.CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public ServiceSubscription<TextInputControl.TextInputStatusListener> subscribeTextInputStatus(TextInputControl.TextInputStatusListener textInputStatusListener) {
        Util.postError(textInputStatusListener, ServiceCommandError.notSupported());
        return new NotSupportedServiceSubscription();
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public void sendText(String str) {
        String str2;
        if (str != null && str.length() != 0) {
            ResponseListener r0 = new ResponseListener<Object>() {
                /* class com.inshot.cast.core.service.RokuService.C58797 */

                @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                }

                @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
                public void onSuccess(Object obj) {
                }
            };
            try {
                str2 = "Lit_" + URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                str2 = null;
            }
            String requestURL = requestURL("keypress", str2);
            Log.d(Util.T, "RokuService::send() | uri = " + requestURL);
            new ServiceCommand(this, requestURL, null, r0).send();
        }
    }

    @Override // com.inshot.cast.core.service.capability.KeyControl
    public void sendKeyCode(KeyControl.KeyCode keyCode, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public void sendEnter() {
        new ServiceCommand(this, requestURL("keypress", "Enter"), null, new ResponseListener<Object>() {
            /* class com.inshot.cast.core.service.RokuService.C58808 */

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
            }
        }).send();
    }

    @Override // com.inshot.cast.core.service.capability.TextInputControl
    public void sendDelete() {
        new ServiceCommand(this, requestURL("keypress", "Backspace"), null, new ResponseListener<Object>() {
            /* class com.inshot.cast.core.service.RokuService.C58819 */

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
            }

            @Override // com.inshot.cast.core.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
            }
        }).send();
    }

    @Override // com.inshot.cast.core.service.DeviceService, com.inshot.cast.core.service.command.ServiceCommand.ServiceCommandProcessor
    public void sendCommand(final ServiceCommand<?> serviceCommand) {
        Util.runInBackground(new Runnable() {
            /* class com.inshot.cast.core.service.RokuService.RunnableC587110 */

            public void run() {
                Object payload = serviceCommand.getPayload();
                try {
                    Log.d("", "RESP " + serviceCommand.getTarget());
                    HttpConnection newInstance = HttpConnection.newInstance(URI.create(serviceCommand.getTarget()));
                    if (serviceCommand.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
                        newInstance.setMethod(HttpConnection.Method.POST);
                        if (payload != null) {
                            newInstance.setPayload(payload.toString());
                        }
                    }
                    newInstance.execute();
                    int responseCode = newInstance.getResponseCode();
                    Log.d("", "RESP " + responseCode);
                    if (responseCode != 200) {
                        if (responseCode != 201) {
                            Util.postError(serviceCommand.getResponseListener(), ServiceCommandError.getError(responseCode));
                            return;
                        }
                    }
                    Util.postSuccess(serviceCommand.getResponseListener(), newInstance.getResponseString());
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.postError(serviceCommand.getResponseListener(), new ServiceCommandError(0, e.getMessage(), null));
                }
            }
        });
    }

    private String requestURL(String str, String str2) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(this.serviceDescription.getIpAddress());
        sb.append(":");
        sb.append(this.serviceDescription.getPort());
        sb.append("/");
        if (str != null) {
            sb.append(str);
        }
        if (str2 != null) {
            sb.append("/");
            sb.append(str2);
        }
        return sb.toString();
    }

    private void probeForAppSupport() {
        getAppList(new Launcher.AppListListener() {
            /* class com.inshot.cast.core.service.RokuService.C587211 */

            @Override // com.inshot.cast.core.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
            }

            public void onSuccess(List<AndroidAppInfo> list) {
                ArrayList arrayList = new ArrayList();
                for (String str : RokuService.registeredApps) {
                    for (AndroidAppInfo androidAppInfo : list) {
                        if (androidAppInfo.getName().contains(str)) {
                            arrayList.add("Launcher." + str);
                            arrayList.add("Launcher." + str + ".Params");
                        }
                    }
                }
                RokuService.this.addCapabilities(arrayList);
            }
        });
    }

    /* access modifiers changed from: protected */
    @Override // com.inshot.cast.core.service.DeviceService
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(KeyControl.Up);
        arrayList.add(KeyControl.Down);
        arrayList.add(KeyControl.Left);
        arrayList.add(KeyControl.Right);
        arrayList.add(KeyControl.OK);
        arrayList.add(KeyControl.Back);
        arrayList.add(KeyControl.Home);
        arrayList.add(KeyControl.Send_Key);
        arrayList.add(Launcher.Application);
        arrayList.add(Launcher.Application_Params);
        arrayList.add(Launcher.Application_List);
        arrayList.add(Launcher.AppStore);
        arrayList.add(Launcher.AppStore_Params);
        arrayList.add(Launcher.Application_Close);
        arrayList.add(MediaPlayer.Display_Image);
        arrayList.add("MediaPlayer.Play.Video");
        arrayList.add("MediaPlayer.Play.Audio");
        arrayList.add(MediaPlayer.Close);
        arrayList.add(MediaPlayer.MetaData_Title);
        arrayList.add(MediaControl.FastForward);
        arrayList.add(MediaControl.Rewind);
        arrayList.add(MediaControl.Play);
        arrayList.add(MediaControl.Pause);
        arrayList.add(TextInputControl.Send);
        arrayList.add(TextInputControl.Send_Delete);
        arrayList.add(TextInputControl.Send_Enter);
        setCapabilities(arrayList);
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public void getPlayState(MediaControl.PlayStateListener playStateListener) {
        Util.postError(playStateListener, ServiceCommandError.notSupported());
    }

    @Override // com.inshot.cast.core.service.capability.MediaControl
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener playStateListener) {
        Util.postError(playStateListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public boolean isConnected() {
        return this.connected;
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public void connect() {
        this.connected = true;
        reportConnected(true);
    }

    @Override // com.inshot.cast.core.service.DeviceService
    public void disconnect() {
        this.connected = false;
        if (this.mServiceReachability != null) {
            this.mServiceReachability.stop();
        }
        Util.runOnUI(new Runnable() {
            /* class com.inshot.cast.core.service.RokuService.RunnableC587312 */

            public void run() {
                if (RokuService.this.listener != null) {
                    RokuService.this.listener.onDisconnect(RokuService.this, null);
                }
            }
        });
    }

    @Override // com.inshot.cast.core.service.DeviceService, com.inshot.cast.core.etc.helper.DeviceServiceReachability.DeviceServiceReachabilityListener
    public void onLoseReachability(DeviceServiceReachability deviceServiceReachability) {
        if (this.connected) {
            disconnect();
        } else if (this.mServiceReachability != null) {
            this.mServiceReachability.stop();
        }
    }

    public DIALService getDIALService() {
        if (this.dialService == null) {
            DiscoveryManager instance = DiscoveryManager.getInstance();
            ConnectableDevice device = instance.getDevice(this.serviceDescription.getIpAddress() + WhisperLinkUtil.CALLBACK_DELIMITER + DIALService.ID);
            if (device != null) {
                DIALService dIALService = null;
                Iterator<DeviceService> it = device.getServices().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    DeviceService next = it.next();
                    if (DIALService.class.isAssignableFrom(next.getClass())) {
                        dIALService = (DIALService) next;
                        break;
                    }
                }
                this.dialService = dIALService;
            }
        }
        return this.dialService;
    }
}

/*
import android.text.TextUtils;
import android.util.Log;

import com.example.cast.core.AppInfo;
import com.example.cast.core.ImageInfo;
import com.example.cast.core.MediaInfo;
import com.example.cast.core.Util;
import com.example.cast.device.ConnectableDevice;
import com.example.cast.discovery.DiscoveryFilter;
import com.example.cast.discovery.DiscoveryManager;
import com.example.cast.etc.helper.DeviceServiceReachability;
import com.example.cast.etc.helper.HttpConnection;
import com.example.cast.etc.helper.HttpMessage;
import com.example.cast.service.capability.CapabilityMethods;
import com.example.cast.service.capability.KeyControl;
import com.example.cast.service.capability.Launcher;
import com.example.cast.service.capability.MediaControl;
import com.example.cast.service.capability.MediaPlayer;
import com.example.cast.service.capability.TextInputControl;
import com.example.cast.service.capability.listeners.ResponseListener;
import com.example.cast.service.command.NotSupportedServiceSubscription;
import com.example.cast.service.command.ServiceCommand;
import com.example.cast.service.command.ServiceCommandError;
import com.example.cast.service.command.ServiceSubscription;
import com.example.cast.service.command.URLServiceSubscription;
import com.example.cast.service.config.ServiceConfig;
import com.example.cast.service.config.ServiceDescription;
import com.example.cast.service.roku.RokuApplicationListParser;
import com.example.cast.service.sessions.LaunchSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RokuService extends DeviceService implements Launcher, MediaPlayer, MediaControl, KeyControl, TextInputControl {

    public static final String ID = "Roku";

    private static List<String> registeredApps = new ArrayList<String>();

    DIALService dialService;

    static {
        registeredApps.add("YouTube");
        registeredApps.add("Netflix");
        registeredApps.add("Amazon");
    }

    public static void registerApp(String appId) {
        if (!registeredApps.contains(appId))
            registeredApps.add(appId);
    }

    public RokuService(ServiceDescription serviceDescription,
                       ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);

        if (this.serviceDescription != null)
            this.serviceDescription.setPort(8060);

        probeForAppSupport();
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "roku:ecp");
    }


    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        } else if (clazz.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        } else if (clazz.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        } else if (clazz.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        } else if (clazz.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override
    public Launcher getLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    class RokuLaunchSession extends LaunchSession {

        public void close(ResponseListener<Object> responseListener) {
            home(responseListener);
        }
    }

    @Override
    public void launchApp(String appId, AppLaunchListener listener) {
        if (appId == null) {
            Util.postError(listener, new ServiceCommandError(0,
                    "Must supply a valid app id", null));
            return;
        }

        AppInfo appInfo = new AppInfo();
        appInfo.setId(appId);

        launchAppWithInfo(appInfo, listener);
    }

    @Override
    public void launchAppWithInfo(AppInfo appInfo,
                                  AppLaunchListener listener) {
        launchAppWithInfo(appInfo, null, listener);
    }

    @Override
    public void launchAppWithInfo(final AppInfo appInfo, Object params,
                                  final AppLaunchListener listener) {
        if (appInfo == null || appInfo.getId() == null) {
            Util.postError(listener, new ServiceCommandError(-1,
                    "Cannot launch app without valid AppInfo object",
                    appInfo));

            return;
        }

        String baseTargetURL = requestURL("launch", appInfo.getId());
        String queryParams = "";

        if (params != null && params instanceof JSONObject) {
            JSONObject jsonParams = (JSONObject) params;

            int count = 0;
            Iterator<?> jsonIterator = jsonParams.keys();

            while (jsonIterator.hasNext()) {
                String key = (String) jsonIterator.next();
                String value = null;

                try {
                    value = jsonParams.getString(key);
                } catch (JSONException ex) {
                }

                if (value == null)
                    continue;

                String urlSafeKey = null;
                String urlSafeValue = null;
                String prefix = (count == 0) ? "?" : "&";

                try {
                    urlSafeKey = URLEncoder.encode(key, "UTF-8");
                    urlSafeValue = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException ex) {

                }

                if (urlSafeKey == null || urlSafeValue == null)
                    continue;

                String appendString = prefix + urlSafeKey + "=" + urlSafeValue;
                queryParams = queryParams + appendString;

                count++;
            }
        }

        String targetURL = null;

        if (queryParams.length() > 0)
            targetURL = baseTargetURL + queryParams;
        else
            targetURL = baseTargetURL;

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                LaunchSession launchSession = new RokuLaunchSession();
                launchSession.setService(RokuService.this);
                launchSession.setAppId(appInfo.getId());
                launchSession.setAppName(appInfo.getName());
                launchSession.setSessionType(LaunchSession.LaunchSessionType.App);
                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, targetURL, null, responseListener);
        request.send();
    }

    @Override
    public void closeApp(LaunchSession launchSession,
                         ResponseListener<Object> listener) {
        home(listener);
    }

    @Override
    public void getAppList(final AppListListener listener) {
        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                String msg = (String) response;

                SAXParserFactory saxParserFactory = SAXParserFactory
                        .newInstance();
                InputStream stream;
                try {
                    stream = new ByteArrayInputStream(msg.getBytes("UTF-8"));
                    SAXParser saxParser = saxParserFactory.newSAXParser();

                    RokuApplicationListParser parser = new RokuApplicationListParser();
                    saxParser.parse(stream, parser);

                    List<AppInfo> appList = parser.getApplicationList();

                    Util.postSuccess(listener, appList);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        String action = "query";
        String param = "apps";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, responseListener);
        request.setHttpMethod(ServiceCommand.TYPE_GET);
        request.send();
    }

    @Override
    public void getRunningApp(AppInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<AppInfoListener> subscribeRunningApp(
            AppInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return new NotSupportedServiceSubscription<AppInfoListener>();
    }

    @Override
    public void getAppState(LaunchSession launchSession,
                            AppStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<AppStateListener> subscribeAppState(
            LaunchSession launchSession, AppStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return null;
    }

    @Override
    public void launchBrowser(String url, AppLaunchListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void launchYouTube(String contentId,
                              AppLaunchListener listener) {
        launchYouTube(contentId, (float) 0.0, listener);
    }

    @Override
    public void launchYouTube(String contentId, float startTime,
                              AppLaunchListener listener) {
        if (getDIALService() != null) {
            getDIALService().getLauncher().launchYouTube(contentId, startTime,
                    listener);
        } else {
            Util.postError(listener, new ServiceCommandError(
                    0,
                    "Cannot reach DIAL service for launching with provided start time",
                    null));
        }
    }

    @Override
    public void launchNetflix(final String contentId,
                              final AppLaunchListener listener) {
        getAppList(new AppListListener() {

            @Override
            public void onSuccess(List<AppInfo> appList) {
                for (AppInfo appInfo : appList) {
                    if (appInfo.getName().equalsIgnoreCase("Netflix")) {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("mediaType", "movie");

                            if (contentId != null && contentId.length() > 0)
                                payload.put("contentId", contentId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        launchAppWithInfo(appInfo, payload, listener);
                        break;
                    }
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void launchHulu(final String contentId,
                           final AppLaunchListener listener) {
        getAppList(new AppListListener() {

            @Override
            public void onSuccess(List<AppInfo> appList) {
                for (AppInfo appInfo : appList) {
                    if (appInfo.getName().contains("Hulu")) {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("contentId", contentId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        launchAppWithInfo(appInfo, payload, listener);
                        break;
                    }
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void launchAppStore(final String appId, AppLaunchListener listener) {
        AppInfo appInfo = new AppInfo("11");
        appInfo.setName("Channel Store");

        JSONObject params = null;
        try {
            params = new JSONObject() {
                {
                    put("contentId", appId);
                }
            };
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public KeyControl getKeyControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void up(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Up";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void down(final ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Down";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void left(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Left";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void right(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Right";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void ok(final ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Select";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void back(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Back";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void home(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Home";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void play(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Play";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void pause(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Play";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void stop(ResponseListener<Object> listener) {
        String action = null;
        String param = "input?a=sto";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void rewind(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Rev";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void fastForward(ResponseListener<Object> listener) {
        String action = "keypress";
        String param = "Fwd";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void previous(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void next(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void getDuration(DurationListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void getPosition(PositionListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void seek(long position, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void getMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(
            MediaInfoListener listener) {
        listener.onError(ServiceCommandError.notSupported());
        return null;
    }

    private void displayMedia(String url, String mimeType, String title,
                              String description, String iconSrc,
                              final LaunchListener listener) {
        ResponseListener<Object> responseListener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                Log.e("displayMedia", "onSuccess: " + response.toString());
                LaunchSession launchSession = new RokuLaunchSession();
                launchSession.setService(RokuService.this);
                launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
                Util.postSuccess(listener, new MediaLaunchObject(launchSession, RokuService.this));
            }

            @Override
            public void onError(ServiceCommandError error) {
                Log.e("displayMedia", "onError: " + error.getMessage());
                Util.postError(listener, error);
            }
        };

        String action = "input";
        String mediaFormat = mimeType;
        if (mimeType.contains("/")) {
            int index = mimeType.indexOf("/") + 1;
            mediaFormat = mimeType.substring(index);
        }

        String param;
        if (mimeType.contains("image")) {
            param = String.format("15985?t=p&u=%s&tr=crossfade", HttpMessage.encode(url));
        } else if (mimeType.contains("video")) {
            param = String.format(
                    "15985?t=v&u=%s&k=(null)&videoName=%s&videoFormat=%s",
                    HttpMessage.encode(url),
                    TextUtils.isEmpty(title) ? "(null)" : HttpMessage.encode(title),
                    HttpMessage.encode(mediaFormat));
        } else { // if (mimeType.contains("audio")) {
            param = String
                    .format("15985?t=a&u=%s&k=(null)&songname=%s&artistname=%s&songformat=%s&albumarturl=%s",
                            HttpMessage.encode(url),
                            TextUtils.isEmpty(title) ? "(null)" : HttpMessage.encode(title),
                            TextUtils.isEmpty(description) ? "(null)" : HttpMessage.encode(description),
                            HttpMessage.encode(mediaFormat),
                            TextUtils.isEmpty(iconSrc) ? "(null)" : HttpMessage.encode(iconSrc));
        }

        String uri = requestURL(action, param);
        Log.e("RokuServices", "displayMedia: " + uri.toString());
        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, responseListener);
        request.send();
    }

    @Override
    public void displayImage(String url, String mimeType, String title,
                             String description, String iconSrc,
                             LaunchListener listener) {
        displayMedia(url, mimeType, title, description, iconSrc, listener);
    }

    @Override
    public void displayImage(MediaInfo mediaInfo,
                             LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        Log.e("RokuServices", "displayImage: " + mediaUrl);
        displayImage(mediaUrl, mimeType, title, desc, iconSrc, listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title,
                          String description, String iconSrc, boolean shouldLoop,
                          LaunchListener listener) {
        displayMedia(url, mimeType, title, description, iconSrc, listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop,
                          LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        playMedia(mediaUrl, mimeType, title, desc, iconSrc, shouldLoop, listener);
    }

    @Override
    public void closeMedia(LaunchSession launchSession,
                           ResponseListener<Object> listener) {
        home(listener);
    }

    @Override
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(
            TextInputStatusListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return new NotSupportedServiceSubscription<TextInputStatusListener>();
    }

    @Override
    public void sendText(String input) {
        if (input == null || input.length() == 0) {
            return;
        }

        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = null;
        try {
            param = "Lit_" + URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This can be safetly ignored since it isn't a dynamic encoding.
            e.printStackTrace();
        }

        String uri = requestURL(action, param);

        Log.d(Util.T, "RokuService::send() | uri = " + uri);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void sendKeyCode(KeyCode keyCode, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void sendEnter() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = "Enter";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void sendDelete() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(ServiceCommandError error) {
                // TODO Auto-generated method stub

            }
        };

        String action = "keypress";
        String param = "Backspace";

        String uri = requestURL(action, param);

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(
                this, uri, null, listener);
        request.send();
    }

    @Override
    public void unsubscribe(URLServiceSubscription<?> subscription) {
    }

    @Override
    public void sendCommand(final ServiceCommand<?> mCommand) {
        Util.runInBackground(new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mCommand;
                Object payload = command.getPayload();

                try {
                    Log.d("", "RESP " + command.getTarget());
                    HttpConnection connection = HttpConnection.newInstance(URI.create(command.getTarget()));
                    if (command.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
                        connection.setMethod(HttpConnection.Method.POST);
                        if (payload != null) {
                            connection.setPayload(payload.toString());
                        }
                    }
                    connection.execute();
                    int code = connection.getResponseCode();
                    Log.d("", "RESP " + code);
                    if (code == 200 || code == 201) {
                        Util.postSuccess(command.getResponseListener(), connection.getResponseString());
                    } else {
                        Util.postError(command.getResponseListener(), ServiceCommandError.getError(code));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.postError(command.getResponseListener(), new ServiceCommandError(0, e.getMessage(), null));
                }

            }
        });
    }

    private String requestURL(String action, String parameter) {
        StringBuilder sb = new StringBuilder();

        sb.append("http://");
        sb.append(serviceDescription.getIpAddress()).append(":");
        sb.append(serviceDescription.getPort()).append("/");

        if (action != null)
            sb.append(action);

        if (parameter != null)
            sb.append("/").append(parameter);

        return sb.toString();
    }

    private void probeForAppSupport() {
        getAppList(new AppListListener() {

            @Override
            public void onError(ServiceCommandError error) {
            }

            @Override
            public void onSuccess(List<AppInfo> object) {
                List<String> appsToAdd = new ArrayList<String>();

                for (String probe : registeredApps) {
                    for (AppInfo app : object) {
                        if (app.getName().contains(probe)) {
                            appsToAdd.add("Launcher." + probe);
                            appsToAdd.add("Launcher." + probe + ".Params");
                        }
                    }
                }

                addCapabilities(appsToAdd);
            }
        });
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();

        capabilities.add(Up);
        capabilities.add(Down);
        capabilities.add(Left);
        capabilities.add(Right);
        capabilities.add(OK);
        capabilities.add(Back);
        capabilities.add(Home);
        capabilities.add(Send_Key);

        capabilities.add(Application);

        capabilities.add(Application_Params);
        capabilities.add(Application_List);
        capabilities.add(AppStore);
        capabilities.add(AppStore_Params);
        capabilities.add(Application_Close);

        capabilities.add(Display_Image);
        capabilities.add(Play_Video);
        capabilities.add(Play_Audio);
        capabilities.add(Close);
        capabilities.add(MetaData_Title);

        capabilities.add(FastForward);
        capabilities.add(Rewind);
        capabilities.add(Play);
        capabilities.add(Pause);

        capabilities.add(Send);
        capabilities.add(Send_Delete);
        capabilities.add(Send_Enter);

        setCapabilities(capabilities);
    }

    @Override
    public void getPlayState(PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(
            PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());

        return null;
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect() {
        // TODO: Fix this for roku. Right now it is using the InetAddress
        // reachable function. Need to use an HTTP Method.
        // mServiceReachability =
        // DeviceServiceReachability.getReachability(serviceDescription.getIpAddress(),
        // this);
        // mServiceReachability.start();

        connected = true;

        reportConnected(true);
    }

    @Override
    public void disconnect() {
        connected = false;

        if (mServiceReachability != null)
            mServiceReachability.stop();

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                if (listener != null)
                    listener.onDisconnect(RokuService.this, null);
            }
        });
    }

    @Override
    public void onLoseReachability(DeviceServiceReachability reachability) {
        if (connected) {
            disconnect();
        } else {
            if (mServiceReachability != null)
                mServiceReachability.stop();
        }
    }

    public DIALService getDIALService() {
        if (dialService == null) {
            DiscoveryManager discoveryManager = DiscoveryManager.getInstance();
            ConnectableDevice device = discoveryManager.getAllDevices().get(
                    serviceDescription.getIpAddress());

            if (device != null) {
                DIALService foundService = null;

                for (DeviceService service : device.getServices()) {
                    if (DIALService.class.isAssignableFrom(service.getClass())) {
                        foundService = (DIALService) service;
                        break;
                    }
                }

                dialService = foundService;
            }
        }

        return dialService;
    }
}
*/
