package com.platform.middlewares.plugins;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eacpay.EacApp;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.platform.BRHTTPHelper;
import com.platform.GeoLocationManager;
import com.platform.interfaces.Plugin;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;
public class GeoLocationPlugin implements Plugin {

    private static Continuation continuation;
    private static Request globalBaseRequest;

    public static void handleGeoPermission(final boolean granted) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (continuation == null) {
                    Timber.d("handleGeoPermission: WARNING continuation is null");
                    return;
                }

                try {
                    if (granted) {
                        globalBaseRequest.setHandled(true);
                        ((HttpServletResponse) continuation.getServletResponse()).setStatus(204);

                    } else {
                        try {
                            Timber.d("handleGeoPermission: granted is false");
                            globalBaseRequest.setHandled(true);
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(400);
                        } catch (IOException e) {
                            Timber.e(e);
                        }
                    }
                } finally {
                    continuation.complete();
                    continuation = null;
                    globalBaseRequest = null;
                }
            }
        });
    }


    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.startsWith("/_permissions/geo")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            Context app = EacApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            switch (request.getMethod()) {
                // GET /_permissions/geo
                //
                // Call this method to retrieve the current permission status for geolocation.
                // The returned JSON dictionary contains the following keys:
                //
                // "status" = "denied" | "restricted | "undetermined" | "inuse" | "always"
                // "user_queried" = true | false
                // "location_enabled" = true | false
                //
                // The status correspond to those found in the apple CLLocation documentation: http://apple.co/1O0lHFv
                //
                // "user_queried" indicates whether or not the user has already been asked for geolocation
                // "location_enabled" indicates whether or not the user has geo location enabled on their phone
                case "GET":
                    JSONObject jsonResult = new JSONObject();
                    String status;
                    boolean enabled;
                    boolean permRequested = BRSharedPrefs.getGeoPermissionsRequested(app);
                    int permissionCheck = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        status = "always";
                        enabled = true;
                    } else {
                        Timber.d("handle: sending permission denied: " + target + " " + baseRequest.getMethod());
                        status = permRequested ? "denied" : "undetermined";
                        enabled = false;
                    }
                    try {
                        jsonResult.put("status", status);
                        jsonResult.put("user_queried", permRequested);
                        jsonResult.put("location_enabled", enabled);
                        return BRHTTPHelper.handleSuccess(200, jsonResult.toString().getBytes(), baseRequest, response, null);
                    } catch (JSONException e) {
                        Timber.e(e);
                        Timber.d("handle: failed to send permission status: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(500, null, baseRequest, response);
                    }
                    // POST /_permissions/geo
                    //
                    // Call this method to request the geo permission from the user.
                    // The request body should be a JSON dictionary containing a single key, "style"
                    // the value of which should be either "inuse" or "always" - these correspond to the
                    // two ways the user can authorize geo access to the app. "inuse" will request
                    // geo availability to the app when the app is foregrounded, and "always" will request
                    // full time geo availability to the app
                case "POST":
                    if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Timber.d("handle: requesting permissions: " + target + " " + baseRequest.getMethod());
                        ActivityCompat.requestPermissions((Activity) app, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BRConstants.GEO_REQUEST_ID);
                    }
                    BRSharedPrefs.putGeoPermissionsRequested(app, true);
                    continuation = ContinuationSupport.getContinuation(request);
                    continuation.suspend(response);
                    globalBaseRequest = baseRequest;
                    return true;

            }
        } else if (target.startsWith("/_geo") && !target.startsWith("/_geosocket")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            // GET /_geo
            //
            // Calling this method will query CoreLocation for a location object. The returned value may not be returned
            // very quick (sometimes getting a geo lock takes some time) so be sure to display to the user some status
            // while waiting for a response.
            //
            // Response Object:
            //
            // "coordinates" = { "latitude": double, "longitude": double }
            // "altitude" = double
            // "description" = "a string representation of this object"
            // "timestamp" = "ISO-8601 timestamp of when this location was generated"
            // "horizontal_accuracy" = double
            Context app = EacApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }

            if (request.getMethod().equalsIgnoreCase("GET")) {
                JSONObject obj = getAuthorizationError(app);
                if (obj != null) {
                    Timber.d("handle: error getting location: " + obj + ", " + target + " " + baseRequest.getMethod());
                    return BRHTTPHelper.handleError(500, obj.toString(), baseRequest, response);
                }

                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                GeoLocationManager.getInstance().getOneTimeGeoLocation(continuation, baseRequest);
                return true;
            }
        } else if (target.startsWith("/_geosocket")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            // GET /_geosocket
            //
            // This opens up a websocket to the location manager. It will return a new location every so often (but with no
            // predetermined interval) with the same exact structure that is sent via the GET /_geo call.
            //
            // It will start the location manager when there is at least one client connected and stop the location manager
            // when the last client disconnects.
            return true;
        }

        return false;
    }

    private JSONObject getAuthorizationError(Context app) {
        String error = null;

        LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
            Timber.e(ignored);
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        if (!gps_enabled && !network_enabled) {
            error = "Location services are disabled";
        }
        int permissionCheck = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            error = "Location services are not authorized";
        }

        if (error != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error", error);
            } catch (JSONException e) {
                Timber.e(e);
            }
            return obj;
        } else {
            return null;
        }
    }
}
