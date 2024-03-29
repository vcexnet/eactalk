package com.platform.middlewares.plugins;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eacpay.EacApp;
import com.eacpay.R;
import com.eacpay.presenter.activities.camera.CameraActivity;
import com.eacpay.presenter.customviews.BRDialogView;
import com.eacpay.tools.animation.BRDialog;
import com.eacpay.tools.crypto.CryptoHelper;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;
public class CameraPlugin implements Plugin {
    private static Request globalBaseRequest;
    private static Continuation continuation;

    @Override
    public boolean handle(String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) {

        // GET /_camera/take_picture
        //
        // Optionally pass ?overlay=<id> (see overlay ids below) to show an overlay
        // in picture taking mode
        //
        // Status codes:
        //   - 200: Successful image capture
        //   - 204: User canceled image picker
        //   - 404: Camera is not available on this device
        //   - 423: Multiple concurrent take_picture requests. Only one take_picture request may be in flight at once.
        //

        if (target.startsWith("/_camera/take_picture")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            final Context app = EacApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(404, "context is null", baseRequest, response);
            }

            if (globalBaseRequest != null) {
                Timber.i("handle: already taking a picture: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(423, null, baseRequest, response);
            }

            PackageManager pm = app.getPackageManager();

            if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Timber.i("handle: no camera available: ");
                return BRHTTPHelper.handleError(404, null, baseRequest, response);
            }

            globalBaseRequest = baseRequest;
            continuation = ContinuationSupport.getContinuation(request);
            continuation.suspend(response);

            try {
                // Check if the camera permission is granted
                if (ContextCompat.checkSelfPermission(app,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(((Activity) app),
                            Manifest.permission.CAMERA)) {
                        BRDialog.showCustomDialog(app, app.getString(R.string.Send_cameraUnavailabeTitle_android),
                                app.getString(R.string.Send_cameraUnavailabeMessage_android),
                                app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                    }
                                }, null, null, 0);
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(((Activity) app),
                                new String[]{Manifest.permission.CAMERA},
                                BRConstants.CAMERA_REQUEST_ID);
                        globalBaseRequest = null;
                    }
                } else {
                    // Permission is granted, open camera
                    Intent intent = new Intent(app, CameraActivity.class);
                    ((Activity) app).startActivityForResult(intent, BRConstants.REQUEST_IMAGE_CAPTURE);
                    ((Activity) app).overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            return true;
        } else if (target.startsWith("/_camera/picture/")) {
            Timber.i("handling: " + target + " " + baseRequest.getMethod());
            final Context app = EacApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(404, "context is null", baseRequest, response);
            }
            String id = target.replace("/_camera/picture/", "");
            byte[] pictureBytes = readPictureForId(app, id);
            if (pictureBytes == null) {
                Timber.i("handle: WARNING pictureBytes is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "pictureBytes is null", baseRequest, response);
            }
            byte[] imgBytes = pictureBytes;
            String b64opt = request.getParameter("base64");
            String contentType = "image/jpeg";
            if (b64opt != null && !b64opt.isEmpty()) {
                contentType = "text/plain";
                String b64 = "data:image/jpeg;base64," + Base64.encodeToString(pictureBytes, Base64.NO_WRAP);
                imgBytes = b64.getBytes(StandardCharsets.UTF_8);
            }
            return BRHTTPHelper.handleSuccess(200, imgBytes, baseRequest, response, contentType);
        } else return false;
    }

    public static void handleCameraImageTaken(final Context context, final byte[] data) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap img = getResizedBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), 1000);
                Timber.i("handleCameraImageTaken: w:" + img.getWidth() + "h:" + img.getHeight());
                if (globalBaseRequest == null || continuation == null) {
                    //shit should now happen
                    Timber.i("handleCameraImageTaken: WARNING: " + continuation + " " + globalBaseRequest);
                    return;
                }
                try {
                    if (img == null) {
                        //no image
                        globalBaseRequest.setHandled(true);
                        ((HttpServletResponse) continuation.getServletResponse()).setStatus(204);
                        continuation.complete();
                        return;
                    }
                    String id = writeToFile(context, img);
                    Timber.d("handleCameraImageTaken: %s", id);
                    if (id != null) {
                        JSONObject respJson = new JSONObject();
                        try {
                            respJson.put("id", id);
                        } catch (JSONException e) {
                            Timber.e(e);
                            globalBaseRequest.setHandled(true);
                            try {
                                ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                            } catch (IOException e1) {
                                Timber.e(e1);
                            }
                            continuation.complete();
                            return;
                        }
                        continuation.getServletResponse().setContentType("application/json");
                        ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                        BRHTTPHelper.handleSuccess(200, respJson.toString().getBytes(), globalBaseRequest, (HttpServletResponse) continuation.getServletResponse(), "application/json");
                        continuation.complete();

                    } else {
                        Timber.i("handleCameraImageTaken: error writing image");
                        try {
                            globalBaseRequest.setHandled(true);
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                            continuation.complete();
                        } catch (IOException e) {
                            Timber.e(e);
                        }
                    }
                } finally {
                    globalBaseRequest = null;
                    continuation = null;
                }
            }
        });
    }

    private static String writeToFile(Context context, Bitmap img) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            img.compress(Bitmap.CompressFormat.JPEG, 70, out);
            String name = CryptoHelper.base58ofSha256(out.toByteArray());
            File storageDir = new File(context.getFilesDir().getAbsolutePath() + "/pictures/");
            File image = new File(storageDir, name + ".jpeg");
            FileUtils.writeByteArrayToFile(image, out.toByteArray());
            return name;
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Timber.e(e);
            }
        }
        return null;
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public byte[] readPictureForId(Context context, String id) {
        Timber.d("readPictureForId: %s", id);
        try {
            //create FileInputStream object
            FileInputStream fin = new FileInputStream(new File(context.getFilesDir().getAbsolutePath() + "/pictures/" + id + ".jpeg"));

            //create string from byte array
            return IOUtils.toByteArray(fin);
        } catch (FileNotFoundException e) {
            Timber.e(e);
        } catch (IOException ioe) {
            Timber.e(ioe);
        }
        return null;
    }
}
