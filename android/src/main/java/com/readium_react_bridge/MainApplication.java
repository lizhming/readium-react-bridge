package com.readium_react_bridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

//import com.facebook.react.PackageList;
import com.facebook.react.bridge.JavaScriptExecutorFactory;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import org.readium.r2.streamer.server.Server;

import nl.komponents.kovenant.android.KovenantAndroid;
import timber.log.Timber;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application  {

//  private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
//    @Override
//    public boolean getUseDeveloperSupport() {
//      return BuildConfig.DEBUG;
//    }
//
//    @Override
//    protected List<ReactPackage> getPackages() {
//      @SuppressWarnings("UnnecessaryLocalVariable")
//      List<ReactPackage> packages = new ArrayList<>(Arrays.<ReactPackage>asList(
//              new MainReactPackage()
//      ));//new PackageList(this).getPackages();
//      //packages.add(new ActivityStarterReactPackage());
//      // Packages that cannot be autolinked yet can be added manually here, for example:
//      // packages.add(new MyReactNativePackage());
//      Log.w("LLLL","getPackages!");
//      return packages;
//    }
//
//    @Override
//    protected String getJSMainModuleName() {
//      return "index";
//    }
//  };
//
//  @Override
//  public ReactNativeHost getReactNativeHost() {
//    return mReactNativeHost;
//  }


  public static Server server;
  public static int localPort = 0;

  @Override
  public void onCreate() {
    super.onCreate();
    KovenantAndroid.startKovenant();
    SoLoader.init(this, /* native exopackage */ false);

    Log.w("LLLL","readium_react_onCreate!");

    singleton = this;
    ServerSocket s = null;
    try {
      s = new ServerSocket(0);
      s.getLocalPort();
      s.close();
      localPort = s.getLocalPort();
      server = new Server(localPort);

      //SharedPreferences preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE);
      //preferences.edit().putString("publicationPort", ""+localPort).apply();
      if (!server.isAlive()) {
        try {
          server.start();
        } catch (IOException e) {
          // do nothing
          Timber.e(e);
        }
        server.loadResources(getAssets(), getApplicationContext());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTerminate() {
    super.onTerminate();

    if (server.isAlive()) {
      server.stop();
    }
    KovenantAndroid.stopKovenant();
  }

  private static MainApplication singleton;
  public static MainApplication getInstance() {
    return singleton;
  }

}
