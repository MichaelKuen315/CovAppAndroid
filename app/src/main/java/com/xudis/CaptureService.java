/*
    CovApp, a tracking based messaging app preserving privacy
    Copyright (C) 2020 DI Michael Kuen, http://www.xudis.com/

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
    -------------------
    Parts of this program are based on code provided as example with android sdk
    or were taken from solutions posted at www.stackoverflow.com
    For these parts other licenses may apply.
 */
package com.xudis;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import com.xudis.model.NamedPlace;
import com.xudis.model.Observation;
import com.xudis.model.TimeInterval;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

// derived from Android SDK sample
// https://developer.android.com/guide/components/services
// https://developer.android.com/guide/topics/connectivity/bluetooth-le

public class CaptureService extends Service {

    public  static final int CAPTURE_PERIOD = 30*1000;
    private static final int SCAN_PERIOD    =    1000;

    private Handler             mHandler;

    private Looper              mServiceLooper;
    private ServiceHandler      mServiceHandler;

    private BluetoothManager    mBluetoothManager;
    private BluetoothAdapter    mBluetoothAdapter;
    private BluetoothLeScanner  mLeScanner;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private boolean                mScanning = false;
        private ArrayList<NamedPlace>  mObservedNamedPlaces;

        private ScanCallback mLeScanCallback = new ScanCallback() {

            @Override
            public void onBatchScanResults(List<ScanResult> results) {

                Iterator<ScanResult> itResult = results.iterator();
                while(itResult.hasNext()) {
                    ScanResult result = itResult.next();

                    BluetoothDevice device       = result.getDevice();
                    int             rssi         = result.getRssi();
                    int             txPowerLevel = result.getScanRecord().getTxPowerLevel();

                    NamedPlace namedPlace = new NamedPlace(device.getAddress(),rssi,txPowerLevel);
                    int idx = mObservedNamedPlaces.indexOf(namedPlace);
                    if(idx<0)
                        mObservedNamedPlaces.add(namedPlace);
                    else {
                        NamedPlace prevNamedPlace = mObservedNamedPlaces.get(idx);

                        prevNamedPlace.updateWith(namedPlace);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                if(mObservedNamedPlaces==null)
                    return;

                BluetoothDevice device       = result.getDevice();
                int             rssi         = result.getRssi();
                int             txPowerLevel = result.getScanRecord().getTxPowerLevel();

                NamedPlace namedPlace = new NamedPlace(device.getAddress(),rssi,txPowerLevel);
                int idx = mObservedNamedPlaces.indexOf(namedPlace);
                if(idx<0)
                    mObservedNamedPlaces.add(namedPlace);
                else {
                    NamedPlace prevNamedPlace = mObservedNamedPlaces.get(idx);

                    prevNamedPlace.updateWith(namedPlace);
                }
            }
        };

        @Override
        public void handleMessage(Message msg) {

            mWakeLock.acquire(2000);
            mObservedNamedPlaces = new ArrayList<NamedPlace>();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if(mScanning)
                        mLeScanner.stopScan(mLeScanCallback);

                    mWakeLock.release();
                    mScanning = false;

                    Date date = new Date(System.currentTimeMillis());

                    if(!mObservedNamedPlaces.isEmpty())
                      try {
                          File xmlFile = new File(getFilesDir(),"observations.xml");

                          FileWriter fw = new FileWriter(xmlFile,true);

                          new Observation(new TimeInterval(date,date),mObservedNamedPlaces).xmlWrite(fw);

                          fw.close();

                          mObservedNamedPlaces = null;

                      } catch (IOException e) {
                          e.printStackTrace();
                      };

                }
            }, SCAN_PERIOD);

            mLeScanner.startScan(null,
                                  new ScanSettings.Builder()
                                              .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                              .build(),

                                   mLeScanCallback);

            mScanning = true;
        }
    }

    PowerManager.WakeLock  mWakeLock;

    @Override
    public void onCreate() {

        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

            mWakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "covapp:wakeLockTag");
        else
            mWakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "covapp:wakeLockTag");

        mHandler = new Handler();

        Notification notification;

        Intent resultIntent = new Intent(this,MainActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

            notification = new Notification.Builder(this)
                                             .setContentTitle("CovApp")
                                             .setContentText("observing")
                                             .setSmallIcon(R.drawable.ic_camera)
                                             .setContentIntent(resultPendingIntent)
                                             .build();
        }
        else {

            notification = new Notification.Builder(this,"xudis_notifications")
                            .setContentTitle("CovApp")
                            .setContentText("observing")
                            .setSmallIcon(R.drawable.ic_camera)
                            .setContentIntent(resultPendingIntent)
                            .build();
        }

        startForeground(1335, notification);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            stopSelf();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            //Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                                                         Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    private boolean       mAlarmWait     = false;
    private PendingIntent mPendingIntent = null;

    private PendingIntent getIntent() {

        if(mPendingIntent!=null)
            return mPendingIntent;

        Intent intent = new Intent(this, CaptureService.class);
        intent.putExtra("ACTION", "CONTINUE");

        mPendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mPendingIntent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action;

        if(intent==null)
            action = "START";
        else
            action = intent.getStringExtra("ACTION");

        if(action.equals("STOP")) {

            if(mAlarmWait) { // Stop may be hit several times

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(getIntent());

                this.mAlarmWait = false;
            }
            this.stopSelf(startId);
            return START_NOT_STICKY;
        }
        else if(action.equals("START") && !mAlarmWait) {

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, getIntent());
            else
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, getIntent());

            mAlarmWait = true;
        }
        else if(action.equals("CONTINUE")) {

            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            mServiceHandler.sendMessage(msg);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CAPTURE_PERIOD, getIntent());
            else
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CAPTURE_PERIOD, getIntent());

            mAlarmWait = true;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("CaptureService","done");
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
