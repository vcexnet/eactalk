package com.platform.middlewares;

import android.content.Context;

import com.eacpay.EacApp;
import com.eacpay.tools.crypto.CryptoHelper;
import com.eacpay.tools.util.TypesConverter;
import com.eacpay.tools.util.Utils;
import com.platform.APIClient;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Middleware;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;
public class HTTPFileMiddleware implements Middleware {
    private final static String DEBUG_URL = null; //modify for testing

    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.equals("/")) return false;
        if (target.equals("/favicon.ico")) {
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        }
        Context app = EacApp.getBreadContext();
        if (app == null) {
            Timber.e("handle: app is null!");
            return true;
        }
        File temp = null;
        byte[] body = null;
        if (DEBUG_URL == null) {
            // fetch the file locally
            String requestedFile = APIClient.getInstance(app).getExtractedPath(app, target);
            temp = new File(requestedFile);
            if (temp.exists() && !temp.isDirectory()) {
                Timber.d("handle: found bundle for:%s", target);
            } else {
                Timber.i("handle: no bundle found for: %s", target);
                return false;
            }

            Timber.i("handling: %s %s", target, baseRequest.getMethod());
            boolean modified = true;
            byte[] md5 = CryptoHelper.md5(TypesConverter.long2byteArray(temp.lastModified()));
            String hexEtag = Utils.bytesToHex(md5);
            response.setHeader("ETag", hexEtag);

            // if the client sends an if-none-match header, determine if we have a newer version of the file
            String etag = request.getHeader("if-none-match");
            if (etag != null && etag.equalsIgnoreCase(hexEtag)) modified = false;

            if (modified) {
                try {
                    body = FileUtils.readFileToByteArray(temp);
                } catch (IOException e) {
                    Timber.e(e);
                }
                if (body == null) {
                    return BRHTTPHelper.handleError(400, "could not read the file", baseRequest, response);
                }
            } else {
                return BRHTTPHelper.handleSuccess(304, null, baseRequest, response, null);
            }
            response.setContentType(detectContentType(temp));

        } else {
            // download the file from the debug endpoint
            String debugUrl = DEBUG_URL + target;

            Request debugRequest = new Request.Builder()
                    .url(debugUrl)
                    .get().build();
            Response debugResp = null;
            try {
                debugResp = APIClient.getInstance(app).sendRequest(debugRequest, false, 0);
                if (debugResp != null)
                    body = debugResp.body().bytes();
            } catch (IOException e) {
                Timber.e(e);
            } finally {
                debugResp.close();
            }

        }

        String rangeString = request.getHeader("range");
        if (!Utils.isNullOrEmpty(rangeString)) {
            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            return handlePartialRequest(baseRequest, response, temp);
        } else {
            if (body == null) {
                return BRHTTPHelper.handleError(404, "not found", baseRequest, response);
            } else {
                return BRHTTPHelper.handleSuccess(200, body, baseRequest, response, null);
            }
        }

    }

    private boolean handlePartialRequest(org.eclipse.jetty.server.Request request, HttpServletResponse response, File file) {
        try {
            String rangeHeader = request.getHeader("range");
            String rangeValue = rangeHeader.trim()
                    .substring("bytes=".length());
            int fileLength = (int) file.length();
            int start, end;
            if (rangeValue.startsWith("-")) {
                end = fileLength - 1;
                start = fileLength - 1
                        - Integer.parseInt(rangeValue.substring("-".length()));
            } else {
                String[] range = rangeValue.split("-");
                start = Integer.parseInt(range[0]);
                end = range.length > 1 ? Integer.parseInt(range[1])
                        : fileLength - 1;
            }
            if (end > fileLength - 1) {
                end = fileLength - 1;
            }
            if (start <= end) {
                int contentLength = end - start + 1;
                response.setHeader("Content-Length", contentLength + "");
                response.setHeader("Content-Range", "bytes " + start + "-"
                        + end + "/" + fileLength);
                byte[] respBody = Arrays.copyOfRange(FileUtils.readFileToByteArray(file), start, contentLength);
                return BRHTTPHelper.handleSuccess(206, respBody, request, response, detectContentType(file));
            }
        } catch (Exception e) {
            Timber.e(e);
            try {
                request.setHandled(true);
                response.getWriter().write("Invalid Range Header");
                response.sendError(400, "Bad Request");
            } catch (IOException e1) {
                Timber.e(e1);
            }

            return true;
        }
        return BRHTTPHelper.handleError(500, "unknown error", request, response);
    }

    private String detectContentType(File file) {
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        switch (extension) {
            case "ttf":
                return "application/font-truetype";
            case "woff":
                return "application/font-woff";
            case "otf":
                return "application/font-opentype";
            case "svg":
                return "image/svg+xml";
            case "html":
                return "text/html";
            case "png":
                return "image/png";
            case "jpeg":
                return "image/jpeg";
            case "jpg":
                return "image/jpeg";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            default:
                break;
        }
        return "application/octet-stream";
    }
}
