/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.content.Intent
import android.net.Uri


/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2IntentMapper(private val mContext: Context, private val mIntents: R2IntentHelper) {

    fun dispatchIntent(intent: Intent) {

        // Get intent, action and MIME type
        val action = intent.action
        val type = intent.type
        val uri: Uri
        uri = if (Intent.ACTION_SEND == action && type != null) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else {
            // Handle other intents, such as being started from the home screen
            intent.data ?: throw IllegalArgumentException("Uri cannot be null")
        }

        if (uri.toString().contains(".")) {
            val extension = uri.toString().substring(uri.toString().lastIndexOf("."))
            if (extension == ".lcpl") {
                mContext.startActivity(mIntents.catalogActivityIntent(mContext, uri, true))
            } else {
                val dispatchIntent = mIntents.catalogActivityIntent(mContext, uri)
                mContext.startActivity(dispatchIntent)
            }
        }
    }
}
