package com.readium_react_bridge;

import android.content.Intent;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.readium.r2.testapp.CatalogActivity;

import javax.annotation.Nonnull;

public class ActivityStarterModule extends ReactContextBaseJavaModule {
    ActivityStarterModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);
    }
    @Nonnull
    @Override
    public String getName() {
        return "ActivityStarterModule";
    }
    @ReactMethod
    void navigateToExample() {
        ReactApplicationContext context = getReactApplicationContext();
        Intent intent = new Intent(context, CatalogActivity.class);
        context.startActivity(intent);
    }
    @ReactMethod
    void navigateToReadium() {

        // -- AudiobookActivity,
        // publicationPath, <-path
        // epubName, <- book.fileName
        // publication <- publication
        // bookId <- book.id

        // -- R2CbzActivity,

        // -- R2EpubActivity
        //


        ReactApplicationContext context = getReactApplicationContext();
        Intent intent = new Intent(context, CatalogActivity.class);
        context.startActivity(intent);
    }
}
