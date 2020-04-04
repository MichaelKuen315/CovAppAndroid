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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.xudis.R;
import com.xudis.model.Address;
import com.xudis.model.Message;
import com.xudis.model.Observations;
import com.xudis.net.GetNewMessages;
import com.xudis.net.NextStep;
import com.xudis.net.NetBroadcast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel =
                    new NotificationChannel("xudis_notifications",
                            (CharSequence) "XudisChannel",
                            NotificationManager.IMPORTANCE_MIN);

            notificationChannel.setDescription("no description");
            notificationChannel.setSound(null, null);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // restore/init persistent data
        SharedPreferences mPrefs = getSharedPreferences("APP_SETTINGS",MODE_PRIVATE);
        mScanEnabled        = mPrefs.getBoolean("scanEnabled", true);
        mLastMessageChecked = mPrefs.getInt("lastMsg",0);
        String encOwn       = mPrefs.getString("encOwn","");

        mOwnMessages = new Hashtable<Integer,Boolean>();
        if(!encOwn.isEmpty()) {
            String[] own = encOwn.split("\\,");

            for(int i=0; i<own.length; i++) {
                int val = Integer.parseInt(own[i]);

                mOwnMessages.put(val,true);
            }
        }
        setContentView(R.layout.activity_main);

        List<String> reqPerm = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            reqPerm.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED)
            reqPerm.add(Manifest.permission.SEND_SMS);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED)
                != PackageManager.PERMISSION_GRANTED)
            reqPerm.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);

        if(reqPerm.size()>0)
          ActivityCompat.requestPermissions(this,
                   reqPerm.toArray(new String[reqPerm.size()]),
                  1234);

        if(mScanEnabled)
            startCapture();

        setupUI();

        //doit();
    }

    private void setupUI() {

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.scan);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mScanEnabled = true;
                    MainActivity.this.startCapture();
                } else {
                    mScanEnabled = false;
                    MainActivity.this.stopCapture();
                }
            }
        });
        toggle.setChecked(mScanEnabled);

        Button button = (Button) findViewById(R.id.submit);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.broadcast();
            }
        });

        button = (Button) findViewById(R.id.check);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.check();
            }
        });

        button = (Button) findViewById(R.id.erase);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.erase();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case 1234: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++)
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            finish();
                }
                else {
                    finish();
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mSMSSentReceiver);

        SharedPreferences mPrefs = getSharedPreferences("APP_SETTINGS",MODE_PRIVATE);

        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putBoolean("scanEnabled", mScanEnabled);
        ed.putInt("lastMsg",mLastMessageChecked);

        String encOwn       = "";

        boolean first = true;
        Iterator<Hashtable.Entry<Integer,Boolean>> itOwn = mOwnMessages.entrySet().iterator();
        while(itOwn.hasNext()) {
            Hashtable.Entry<Integer,Boolean> entry = itOwn.next();

            if(!first)
                encOwn += ","+entry.getKey();
            else
                encOwn += entry.getKey();

            first = false;
        }
        ed.putString("encOwn",encOwn);

        ed.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mSMSSentReceiver,new IntentFilter("SENT"));
    }

    void startCapture() {

        Intent intent = new Intent(this, CaptureService.class);
        intent.putExtra("ACTION", "START");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);

    }

    void stopCapture() {

        Intent intent = new Intent(this, CaptureService.class);
        intent.putExtra("ACTION", "STOP");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);
    }
/*
    private void doit() {

        final Handler handler = new Handler();

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {

                broadcast();
                check();

                handler.postDelayed(this, 10000);
            }
        };
        handler.post(runnableCode);
    }
*/
    void broadcast() {

        try {
            Observations obs = Observations.load(getFilesDir());
            if(obs==null) {

                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(this, "No tracking data!", duration);
                toast.show();

                return;
            }
            Address addr = obs.buildAddress(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date());
            //Address addr = obs.buildAddress(new Date(System.currentTimeMillis() - (1000 * 20)), new Date());

            new NetBroadcast(getUrl(),
                    new NextStep<Integer>() {

                        public void next(Integer i) {

                            mOwnMessages.put(i,true);

                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;

                            CharSequence chr = getText(R.string.message_sent).toString()+" "+i;

                            Toast toast = Toast.makeText(context, chr, duration);
                            toast.show();
                        }

                        public void exception(Exception e) {

                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, R.string.send_failed, duration);
                            toast.show();
                        }

                    }).execute(new Message(addr, "Messagebody"));

        } catch (IOException | ParseException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    void check() {

        if(bChecking)
            return;

        bChecking = true;
        new GetNewMessages(getUrl(),mLastMessageChecked,mOwnMessages,getFilesDir(),mMessenger).start();
    }

    void erase() {
        new File(getFilesDir(),"observations.xml").delete();

        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(this, "Data erased!", duration);
        toast.show();
    }

    private String getUrl() {

        if(mUrl!=null)
            return mUrl;

        mUrl = getText(R.string.url).toString();
        return mUrl;
    }

    private Hashtable<Integer,Boolean>
                              mOwnMessages;

    private String            mUrl;

    private boolean           mScanEnabled;

    private int               mLastMessageChecked;

    // currently checking for new messages
    private boolean           bChecking = false;

    // messenger listening for network task notifications
    private final Messenger   mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {

        public IncomingHandler() {
            super();

            mSent = new Intent("SENT");
        }

        @Override
        public void handleMessage(android.os.Message msg) {

            switch (msg.what) {
                case 1:
                    MainActivity.this.bChecking = false;
                    Integer[] msgIds = (Integer[])msg.obj;

                    if(msgIds==null) {

                        int duration = Toast.LENGTH_LONG;

                        Toast toast;
                        toast = Toast.makeText(MainActivity.this, "keine neuen Nachrichten", duration);
                        toast.show();
                        return;
                    }
                    if(msgIds.length>0) {
                        askAndNotify(msgIds);
                        //Button button = (Button) findViewById(R.id.submit);
                        //button.setBackgroundColor(0xFFFF0000);
                    }
                    else {

                        //Button button = (Button) findViewById(R.id.submit);
                        //button.setBackgroundColor(0xFF00FF00);

                        int duration = Toast.LENGTH_LONG;

                        Toast toast;
                        toast = Toast.makeText(MainActivity.this, "keine Übereinstimmung gefunden", duration);
                        toast.show();

                    }
                    break;

                case 2:
                case 3:
                    mLastMessageChecked = msg.arg1;

                    if(System.currentTimeMillis()-mTS<500)
                        return;

                    mTS = System.currentTimeMillis();

                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, "Nachricht "+msg.arg1+((msg.what==2)?" positiv getestet!":" negativ getestet!"), duration);
                    toast.show();

                    break;

                default:
                    super.handleMessage(msg);
            }
        }

        public void askAndNotify(final Integer[] msgIds) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setMessage(getText(R.string.quest));
            alertDialogBuilder.setPositiveButton("Ja",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            String arg = "";
                            for(int i=0; i<msgIds.length; i++)
                                arg += ","+msgIds[i];

                            sendSMS(arg);
                        }
                    });

            alertDialogBuilder.setNegativeButton("No",null);

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        public void sendSMS(String msg) {

            PendingIntent mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 1, mSent, PendingIntent.FLAG_UPDATE_CURRENT);

            //SmsManager smsManager = SmsManager.getDefault();
            //smsManager.sendTextMessage("<enter phone number here>", null, msg, mPendingIntent, null);
        }

        private long mTS = 0;

        private Intent   mSent;
    }

    // sms sent
    private BroadcastReceiver mSMSSentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int duration = Toast.LENGTH_LONG;

            Toast toast;

            if(getResultCode()== Activity.RESULT_OK)
                toast = Toast.makeText(context, R.string.message_sent, duration);
            else
                toast = Toast.makeText(context, R.string.send_failed, duration);

            toast.show();
        }
    };

}
