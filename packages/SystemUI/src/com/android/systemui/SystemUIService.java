/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.Object;
import java.lang.System;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
/**
 * Forward Port Tablet UI toggle
 * TODO: Fix DateView
 * Original Patch by Scott Brady <sbradymobile@gmail.com>
 * Change-Id: Ibc688afd5e643165a2ceeba9f832ed50e6af3715
 */
import android.content.ContentResolver;
import android.provider.Settings;
import android.os.Build;
import android.os.SystemProperties;
/**
 * Port end
 */
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;

public class SystemUIService extends Service {
    static final String TAG = "SystemUIService";

    /**
     * Forward Port Tablet UI toggle
     * TODO: Fix DateView
     * Original Patch by Scott Brady <sbradymobile@gmail.com>
     * Change-Id: Ibc688afd5e643165a2ceeba9f832ed50e6af3715
     */
    Context context = this;
    /**
     * Port end
     */

    /**
     * The class names of the stuff to start.
     */
    final Object[] SERVICES = new Object[] {
            0, // system bar or status bar, filled in below.
            com.android.systemui.power.PowerUI.class,
            com.android.systemui.media.RingtonePlayer.class,
        };

    /**
     * Hold a reference on the stuff we start.
     */
    SystemUI[] mServices;

    private Class chooseClass(Object o) {
        if (o instanceof Integer) {
            final String cl = getString((Integer)o);
            try {
                return getClassLoader().loadClass(cl);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } else if (o instanceof Class) {
            return (Class)o;
        } else {
            throw new RuntimeException("Unknown system ui service: " + o);
        }
    }

    @Override
    public void onCreate() {
        // Tell the accessibility layer that this process will
        // run as the current user, i.e. run across users.
        AccessibilityManager.createAsSharedAcrossUsers(this);

        // Pick status bar or system bar.
        /**
         * Forward Port Tablet UI toggle
         * TODO: Fix DateView
         * Original Patch by Scott Brady <sbradymobile@gmail.com>
         * Change-Id: Ibc688afd5e643165a2ceeba9f832ed50e6af3715
         */
        boolean tabletModeOverride = false;
        int tabletModeCheckDone = Settings.System.getInt(context.getContentResolver(), Settings.System.TABLET_MODE_CHECK, 0);
Slog.d("TabletUI", "tabletModeCheckDone at onCreate = "+tabletModeCheckDone);
        if (tabletModeCheckDone == 0) {
Slog.d("TabletUI", "tabletModeCheckDone == 0 => Check builds.prop -> tablet.mode");
            String tabletModePreset = SystemProperties.get("tablet.mode","0");
Slog.d("TabletUI", "tablet.mode at onCreate = "+tabletModePreset);
            if (tabletModePreset.equals("1")) {
Slog.d("TabletUI", "tabletModePreset == 1 => Force tablet UI");
                Settings.System.putInt(context.getContentResolver(), Settings.System.TABLET_MODE,1);
                tabletModeOverride = true;
                Settings.System.putInt(context.getContentResolver(), Settings.System.TABLET_MODE_CHECK,1);
            } else {
Slog.d("TabletUI", "tabletModePreset == 0 => Leave decision to Android");
                tabletModeOverride = false;
            }
        } else {
Slog.d("TabletUI", "tabletModeCheckDone == 1 => Just get Settings.System.TABLET_MODE");
            tabletModeOverride = (Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.TABLET_MODE, 0) == 1);
Slog.d("TabletUI", "tabletModeOverride at onCreate = "+tabletModeOverride);
        } 
        SERVICES[0] = tabletModeOverride
            ? R.string.config_systemBarComponent
            : R.string.config_statusBarComponent;

        /*
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            SERVICES[0] = wm.hasSystemNavBar()
                    ? R.string.config_systemBarComponent
                    : R.string.config_statusBarComponent;
        } catch (RemoteException e) {
            Slog.w(TAG, "Failing checking whether status bar can hide", e);
        }
        */
        /**
         * Port end
         */

        final int N = SERVICES.length;
        mServices = new SystemUI[N];
        for (int i=0; i<N; i++) {
            Class cl = chooseClass(SERVICES[i]);
            Slog.d(TAG, "loading: " + cl);
            try {
                mServices[i] = (SystemUI)cl.newInstance();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            }
            mServices[i].mContext = this;
            Slog.d(TAG, "running: " + mServices[i]);

            mServices[i].start();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        for (SystemUI ui: mServices) {
            ui.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Nobody binds to us.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args == null || args.length == 0) {
            for (SystemUI ui: mServices) {
                pw.println("dumping service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
        } else {
            String svc = args[0];
            for (SystemUI ui: mServices) {
                String name = ui.getClass().getName();
                if (name.endsWith(svc)) {
                    ui.dump(fd, pw, args);
                }
            }
        }
    }
}

