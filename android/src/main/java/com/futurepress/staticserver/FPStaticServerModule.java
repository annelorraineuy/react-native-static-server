
package com.futurepress.staticserver;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.LifecycleEventListener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


import android.util.Log;


import fi.iki.elonen.SimpleWebServer;

public class FPStaticServerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

  private final ReactApplicationContext reactContext;

  private static final String LOGTAG = "FPStaticServerModule";

  private File www_root = null;
  private int port = 9999;
  private boolean localhost_only = false;

  private String localPath = "";
  private SimpleWebServer server = null;
  private String	url = "";

  public FPStaticServerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private String __getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (! inetAddress.isLoopbackAddress()) {
            String ip = inetAddress.getHostAddress();
            if(InetAddressUtils.isIPv4Address(ip)) {
              Log.w(LOGTAG, "local IP: "+ ip);
              return ip;
            }
          }
        }
      }
    } catch (SocketException ex) {
      Log.e(LOGTAG, ex.toString());
    }

    return "127.0.0.1";
  }

  @Override
  public String getName() {
    return "FPStaticServer";
  }

  @ReactMethod
  public void start(String _port, String root, Boolean localhost, Promise promise) {

    if (_port != null) {
      try {
        port = Integer.parseInt(_port);
      } catch(NumberFormatException nfe) {
        port = 9999;
      }
    }

    if (root != null) {
      if(root.startsWith("/")) {
        www_root = new File(root);
        localPath = www_root.getAbsolutePath();
      } else {
        www_root = this.reactContext.getFilesDir();
        localPath = www_root.getAbsolutePath();
      }
    }

    if (localhost != null) {
      localhost_only = localhost;
    }


    try {

      if(localhost_only) {
        byte[] ipAddr = new byte[]{127, 0, 0, 1};
        InetAddress localAddr = InetAddress.getByAddress(ipAddr);
        server = new WebServer(localAddr.toString(), port, www_root);
      } else {
        server = new WebServer(__getLocalIpAddress(), port, www_root);
      }


      if (localhost_only) {
        url = "http://127.0.0.1:" + port;
      } else {
        url = "http://" + __getLocalIpAddress() + ":" + port;
      }

      server.start();

      promise.resolve(url);

    } catch (IOException e) {
      String msg = e.getMessage();


      Log.w(LOGTAG, e);

      // Server doesn't stop on refresh
      if (server != null && msg.equals("bind failed: EADDRINUSE (Address already in use)")){
        server.stop();
        start(_port, root, localhost, promise);
      } else {
        promise.reject(null, msg);
      }

    }


  }

  @ReactMethod
  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  /* Shut down the server if app is destroyed or paused */
  @Override
  public void onHostResume() {
    start(null, null, null, null);
  }

  @Override
  public void onHostPause() {
    stop();
  }

  @Override
  public void onHostDestroy() {
    stop();
  }
}
