package com.platform.middlewares;

import android.content.Context;

import com.eacpay.EacApp;
import com.platform.APIClient;
import com.platform.interfaces.Middleware;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;
public class APIProxy implements Middleware {

    private final APIClient apiInstance;
    private static final String MOUNT_POINT = "/_api";
    private final String SHOULD_VERIFY_HEADER = "x-should-verify";
    private final String SHOULD_AUTHENTICATE = "x-should-authenticate";
    private final String[] bannedSendHeaders = new String[]{
            SHOULD_VERIFY_HEADER,
            SHOULD_AUTHENTICATE,
            "connection",
            "authorization",
            "host",
            "user-agent"};

    private final String[] bannedReceiveHeaders = new String[]{
            "content-length",
            "content-encoding",
            "connection"};

    public APIProxy() {
        Context app = EacApp.getBreadContext();
        if (app == null) {
            Timber.i("APIProxy: app is null!");
        }
        apiInstance = APIClient.getInstance(app);
    }

    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (!target.startsWith(MOUNT_POINT)) return false;
        Timber.d("handling: " + target + " " + baseRequest.getMethod());
        String path = target.substring(MOUNT_POINT.length());
        String queryString = baseRequest.getQueryString();
        if (queryString != null && queryString.length() > 0)
            path += "?" + queryString;
        boolean auth = false;
        Request req = mapToOkHttpRequest(baseRequest, path, request);
        String authHeader = baseRequest.getHeader(SHOULD_AUTHENTICATE);

        if (authHeader != null && (authHeader.equalsIgnoreCase("yes") || authHeader.equalsIgnoreCase("true"))) {
            auth = true;
        }

        Response res = apiInstance.sendRequest(req, auth, 0);
        try {
            ResponseBody body = res.body();
            String cType = body.contentType() == null ? null : body.contentType().toString();
            String resString = null;
            byte[] bodyBytes = new byte[0];
            try {
                bodyBytes = body.bytes();
                resString = new String(bodyBytes);
            } catch (IOException e) {
                Timber.e(e);
            }

            response.setContentType(cType);
            Headers headers = res.headers();
            for (String s : headers.names()) {
                if (Arrays.asList(bannedReceiveHeaders).contains(s.toLowerCase())) continue;
                response.addHeader(s, res.header(s));
            }
            response.setContentLength(bodyBytes.length);

            if (!res.isSuccessful()) {
                Timber.d("RES IS NOT SUCCESSFUL: " + res.request().url() + ": " + res.code() + "(" + res.message() + ")");
            }

            try {
                response.setStatus(res.code());
                if (cType != null && !cType.isEmpty())
                    response.setContentType(cType);
                response.getOutputStream().write(bodyBytes);
                baseRequest.setHandled(true);
            } catch (IOException e) {
                Timber.e(e);
            }
        } finally {
            if (res != null) res.close();
        }
        return true;

    }

    private Request mapToOkHttpRequest(org.eclipse.jetty.server.Request baseRequest, String path, HttpServletRequest request) {
        Request req;
        Request.Builder builder = new Request.Builder()
                .url(apiInstance.buildUrl(path));

        Enumeration<String> headerNames = baseRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hName = headerNames.nextElement();
            if (Arrays.asList(bannedSendHeaders).contains(hName.toLowerCase())) continue;
            builder.addHeader(hName, baseRequest.getHeader(hName));
        }

        byte[] bodyText = new byte[0];
        try {
            bodyText = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException e) {
            Timber.e(e);
        }
        String contentType = baseRequest.getContentType() == null ? null : baseRequest.getContentType();
        RequestBody reqBody = RequestBody.create(contentType == null ? null : MediaType.parse(contentType), bodyText);

        switch (baseRequest.getMethod()) {
            case "GET":
                builder.get();
                break;
            case "DELETE":
                builder.delete();
                break;
            case "POST":
                builder.post(reqBody);
                break;
            case "PUT":
                builder.put(reqBody);
                break;
            default:
                Timber.d("mapToOkHttpRequest: WARNING: method: %s", baseRequest.getMethod());
                break;
        }

        req = builder.build();
        return req;
    }
}
