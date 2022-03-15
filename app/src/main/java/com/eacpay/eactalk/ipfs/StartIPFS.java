package com.eacpay.eactalk.ipfs;

import android.os.AsyncTask;
import android.util.Log;

import com.eacpay.eactalk.MainActivity;
import com.eacpay.tools.manager.BRSharedPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ipfs.gomobile.android.IPFS;

public class StartIPFS extends AsyncTask<Void, Void, String> {
    private static final String TAG = "oldfeel";

    private WeakReference<MainActivity> activityRef;
    private boolean backgroundError;

    public StartIPFS(MainActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected String doInBackground(Void... v) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            cancel(true);
            return null;
        }

        try {
            IPFS ipfs = new IPFS(activity.getApplicationContext());
            ipfs.setBootstrap("[\n" +
                    "        \"/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ\",\n" +
                    "        \"/ip4/104.131.131.82/udp/4001/quic/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ\",\n" +
                    "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN\",\n" +
                    "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmQCU2EcMqAqQPR2i9bChDtGNJchTbq5TbXJJ16u19uLTa\",\n" +
                    "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmbLHAnMoJPWSCR5Zhtx6BHJX9KiKNN6tpvbUcqanj75Nb\",\n" +
                    "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmcZf59bWwK5XFi76CZX8cbJ4BhTzzA3gU1ZjYZcYW3dwt\",\n" +
                    "        \"/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3\",\n" +
                    "        \"/ip4/39.108.226.205/udp/4001/quic/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3\"\n" +
                    "    ]");
            ipfs.start();

            ArrayList<JSONObject> jsonList = ipfs.newRequest("id").sendToJSONList();

            Log.d(TAG, "doInBackground: " + jsonList.toString());
            JSONArray addresses = jsonList.get(0).getJSONArray("Addresses");
            for (int i = 0; i < addresses.length(); i++) {
                Log.d(TAG, "doInBackground: address " + addresses.get(i).toString());
            }

            try {
                String ipfsNode = BRSharedPrefs.getString(activityRef.get(), "ipfs_node", "/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3");

                byte[] bootStrapData = ipfs.newRequest("bootstrap")
                        .withArgument("add")
                        .withArgument(ipfsNode)
                        .send();

                Log.d(TAG, "onClick: bootStrapData " + new String(bootStrapData));

                ipfs.pubSubSubscribe("oldfeel", new IPFS.SubListener() {
                    @Override
                    public void onListener(String var1) {
                        Log.d(TAG, "onListener: " + var1);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: " + e.getMessage());
            }
            IpfsManager.getInstance().setIpfs(ipfs);
            return jsonList.get(0).getString("ID");
        } catch (Exception err) {
            Log.e(TAG, "doInBackground: " + err.getMessage());
            backgroundError = true;
            return MainActivity.exceptionToString(err);
        }
    }

    protected void onPostExecute(String result) {
        MainActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        if (backgroundError) {
//            activity.displayPeerIDError(result);
            Log.e(TAG, "IPFS start error: " + result);
        } else {
            activity.displayPeerIDResult(result);
            Log.i(TAG, "Your PeerID is: " + result);
        }
    }
}
