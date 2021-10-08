package com.example.cast.service.google_cast;

import androidx.annotation.NonNull;

import com.example.cast.core.Util;
import com.example.cast.service.sessions.CastWebAppSession;
import com.example.cast.service.sessions.WebAppSessionListener;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

import org.json.JSONException;
import org.json.JSONObject;

public class CastServiceChannel implements Cast.MessageReceivedCallback{
    final String webAppId;
    final CastWebAppSession session;

    public CastServiceChannel(String webAppId, @NonNull CastWebAppSession session) {
        this.webAppId = webAppId;
        this.session = session;
    }

    public String getNamespace() {
        return "urn:x-cast:com.example.mirrorcast";
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, final String message) {
        final WebAppSessionListener webAppSession = session.getWebAppSessionListener();
        if (webAppSession == null) {
            return;
        }

        JSONObject messageJSON = null;

        try {
            messageJSON = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        final JSONObject mMessage = messageJSON;

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                if (mMessage == null) {
                    webAppSession.onReceiveMessage(session, message);
                } else {
                    webAppSession.onReceiveMessage(session, mMessage);
                }
            }
        });
    }
}
