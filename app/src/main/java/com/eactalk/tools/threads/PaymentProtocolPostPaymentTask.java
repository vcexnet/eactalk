package com.eactalk.tools.threads;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.eactalk.EactalkApp;
import com.eactalk.R;
import com.eactalk.presenter.activities.BreadActivity;
import com.eactalk.presenter.customviews.BRToast;
import com.eactalk.presenter.entities.PaymentRequestWrapper;
import com.eactalk.tools.security.BitcoinUrlHandler;
import com.eactalk.tools.util.BytesUtil;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;
public class PaymentProtocolPostPaymentTask extends AsyncTask<String, String, String> {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";

    public HttpURLConnection urlConnection;
    public PaymentRequestWrapper paymentRequest = null;
    public static String message;

    public static boolean waiting = true;
    public static boolean sent = false;
    public static Map<String, String> pendingErrorMessages = new HashMap<>();

    public PaymentProtocolPostPaymentTask(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    @Override
    protected String doInBackground(String... uri) {
        InputStream in;
        try {
            waiting = true;
            sent = false;
            URL url = new URL(paymentRequest.paymentURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/litecoin-payment");
            urlConnection.addRequestProperty("Accept", "application/litecoin-paymentack");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
            dStream.write(paymentRequest.payment);

            in = urlConnection.getInputStream();

            if (in == null) {
                Timber.i("The inputStream is null!");
                return null;
            }
            byte[] serializedBytes = BytesUtil.readBytesFromStream(in);
            if (serializedBytes == null || serializedBytes.length == 0) {
                Timber.d("serializedBytes are null!!!");
                return null;
            }

            message = BitcoinUrlHandler.parsePaymentACK(serializedBytes);
//            PostAuth.getInstance().setTmpPaymentRequest(paymentRequest);
//            PostAuth.getInstance().onPaymentProtocolRequest(app,false);
        } catch (Exception e) {
            Context app = EactalkApp.getBreadContext();
            if (e instanceof java.net.UnknownHostException) {
                if (app != null) {
                    pendingErrorMessages.put(TITLE, app.getString(R.string.Alert_error));
                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.Send_remoteRequestError));
                }
            }
//            else if (e instanceof FileNotFoundException) {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.bad_payment_request));
//                }
//            } else if (e instanceof SocketTimeoutException) {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.connection_timed_out));
//                }
//            } else {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.could_not_transmit_payment));
////                    if (!((EactalkApp) app.getApplication()).hasInternetAccess())
////                        BreadDialog.
////                                showCustomDialog(app,app.getString(R.string.could_not_make_payment), app.getString(R.string.not_connected_network), app.getString(R.string.ok));
//
//                }
//
//            }
            Timber.e(e);
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
            waiting = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        handleMessage();
    }

    public static void handleMessage() {
        Context app = EactalkApp.getBreadContext();
        if (app != null && message != null) {
            if (!message.isEmpty()) {
                BRToast.
                        showCustomToast(app, message, BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
            } else {
                if (!waiting && !sent && pendingErrorMessages.get(MESSAGE) != null) {
//                    BreadDialog.
//                            showCustomDialog(app,pendingErrorMessages.get(TITLE), pendingErrorMessages.get(MESSAGE), app.getString(R.string.ok));
                    pendingErrorMessages = null;
                }
            }
        }
    }

}
