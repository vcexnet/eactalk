package com.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.eactalk.EactalkApp;
import com.eactalk.tools.threads.BRExecutor;
import com.eactalk.tools.util.Utils;
import com.platform.interfaces.Middleware;
import com.platform.interfaces.Plugin;
import com.platform.middlewares.APIProxy;
import com.platform.middlewares.HTTPFileMiddleware;
import com.platform.middlewares.HTTPIndexMiddleware;
import com.platform.middlewares.HTTPRouter;
import com.platform.middlewares.plugins.CameraPlugin;
import com.platform.middlewares.plugins.GeoLocationPlugin;
import com.platform.middlewares.plugins.KVStorePlugin;
import com.platform.middlewares.plugins.LinkPlugin;
import com.platform.middlewares.plugins.WalletPlugin;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;
public class HTTPServer {
    private static Set<Middleware> middlewares;
    private static Server server;
    public static final int PORT = 31120;
    public static final String URL_EA = "http://localhost:" + PORT + "/ea";
    public static final String URL_BUY = "http://localhost:" + PORT + "/buy";
    public static final String URL_SUPPORT = "http://localhost:" + PORT + "/support";
    public static ServerMode mode;

    public enum ServerMode {
        SUPPORT,
        BUY,
        EA
    }

    public HTTPServer() {
        init();
    }

    private static void init() {
        middlewares = new LinkedHashSet<>();
        server = new Server(PORT);
        try {
            server.dump(System.err);
        } catch (IOException e) {
            Timber.e(e);
        }

        HandlerCollection handlerCollection = new HandlerCollection();

        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(BRGeoWebSocketHandler.class);
            }
        };

        ServerHandler serverHandler = new ServerHandler();
        handlerCollection.addHandler(serverHandler);
        handlerCollection.addHandler(wsHandler);

        server.setHandler(handlerCollection);

        setupIntegrations();

    }

    public synchronized static void startServer() {
        Timber.d("startServer");
        try {
            if (server != null && server.isStarted()) {
                return;
            }
            if (server == null) init();
            server.start();
            server.join();
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    public static void stopServer() {
        Timber.d("stopServer");
        try {
            if (server != null)
                server.stop();
        } catch (Exception e) {
            Timber.e(e);
        }
        server = null;
    }

    public boolean isStarted() {
        return server != null && server.isStarted();
    }

    private static class ServerHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            boolean success;
            success = dispatch(target, baseRequest, request, response);
            if (!success) {
                Timber.i("handle: NO MIDDLEWARE HANDLED THE REQUEST: %s", target);
            }
        }
    }

    private static boolean dispatch(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        Timber.d("TRYING TO HANDLE: " + target + " (" + request.getMethod() + ")");
        final Context app = EactalkApp.getBreadContext();
        boolean result = false;
        if (target.equalsIgnoreCase("/_close")) {
            if (app != null) {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        ((Activity) app).onBackPressed();
                    }
                });
                return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
            }
            return true;
        } else if (target.toLowerCase().startsWith("/_email")) {
            Timber.d("dispatch: uri: %s", baseRequest.getUri().toString());
            String address = Uri.parse(baseRequest.getUri().toString()).getQueryParameter("address");
            Timber.d("dispatch: address: %s", address);
            if (Utils.isNullOrEmpty(address)) {
                return BRHTTPHelper.handleError(400, "no address", baseRequest, response);
            }

            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{address});

            //need this to prompts email client only
            email.setType("message/rfc822");

            app.startActivity(Intent.createChooser(email, "Choose an Email client :"));
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        } else if (target.toLowerCase().startsWith("/didload")) {
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        }

        for (Middleware m : middlewares) {
            result = m.handle(target, baseRequest, request, response);
            if (result) {
                String className = m.getClass().getName().substring(m.getClass().getName().lastIndexOf(".") + 1);
                if (!className.contains("HTTPRouter"))
                    Timber.d("dispatch: " + className + " succeeded:" + request.getRequestURL());
                break;
            }
        }
        return result;
    }

    private static void setupIntegrations() {
        // proxy api for signing and verification
        APIProxy apiProxy = new APIProxy();
        middlewares.add(apiProxy);

        // http router for native functionality
        HTTPRouter httpRouter = new HTTPRouter();
        middlewares.add(httpRouter);

        // basic file server for static assets
        HTTPFileMiddleware httpFileMiddleware = new HTTPFileMiddleware();
        middlewares.add(httpFileMiddleware);

        // middleware to always return index.html for any unknown GET request (facilitates window.history style SPAs)
        HTTPIndexMiddleware httpIndexMiddleware = new HTTPIndexMiddleware();
        middlewares.add(httpIndexMiddleware);

        // geo plugin provides access to onboard geo location functionality
        Plugin geoLocationPlugin = new GeoLocationPlugin();
        httpRouter.appendPlugin(geoLocationPlugin);

        // camera plugin
        Plugin cameraPlugin = new CameraPlugin();
        httpRouter.appendPlugin(cameraPlugin);

        // wallet plugin provides access to the wallet
        Plugin walletPlugin = new WalletPlugin();
        httpRouter.appendPlugin(walletPlugin);

        // link plugin which allows opening links to other apps
        Plugin linkPlugin = new LinkPlugin();
        httpRouter.appendPlugin(linkPlugin);

        // kvstore plugin provides access to the shared replicated kv store
        Plugin kvStorePlugin = new KVStorePlugin();
        httpRouter.appendPlugin(kvStorePlugin);
    }

}
