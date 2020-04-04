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
package com.xudis.net;

import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.xudis.model.Address;
import com.xudis.model.Message;
import com.xudis.model.Observations;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class GetNewMessages {

    public GetNewMessages(String url, int lastKnown, Hashtable<Integer,Boolean> ownMessages,  File filesDir, Messenger messenger) {
        super();

        this.mUrl         = url;
        this.mLastKnown   = lastKnown;
        this.mOwnMessages = ownMessages;
        this.mFilesDir    = filesDir;
        this.mMessenger   = messenger;
    }

    public void start() {
        new GetMax(mUrl, new GotMinMax()).execute("");
    }

    class GotMinMax extends NextStep<int[]> {

        @Override
        public void next(int[] minMax) throws RemoteException {

            GetNewMessages.this.mLastMessage = minMax[1];

            mExpectedMsg = Math.max(minMax[0],mLastKnown+1);

            if(mExpectedMsg>mLastMessage) {
                mMessenger.send(android.os.Message.obtain(null,1, new Integer[0]));
                return;
            }

            try {
                Observations obs = Observations.load(mFilesDir);
                if(obs==null) {
                    mMessenger.send(android.os.Message.obtain(null,1, null));
                    return; // nothing to match against
                }
                mAddr = obs.buildAddress(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)), new Date());
                //mAddr = obs.buildAddress(new Date(System.currentTimeMillis() - (1000 * 20)), new Date());

                new GetMessage(mUrl, new GotMessage()).execute(mExpectedMsg);
            }
            catch(IOException | ParseException | XmlPullParserException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    class GotMessage extends NextStep<Message> {

        @Override
        public void next(Message msg) throws RemoteException {

            if(!mOwnMessages.containsKey(mExpectedMsg)) {

                Address dest = msg.getDestination();

//                Log.v("TAG","---");
                if (mAddr.intersects(dest)) {
                    mMatches.add(mExpectedMsg);
                    mMessenger.send(android.os.Message.obtain(null,2, mExpectedMsg, 0));
                }
                else
                    mMessenger.send(android.os.Message.obtain(null,3, mExpectedMsg, 0));
            }
            mExpectedMsg++;
            if(mExpectedMsg<=mLastMessage)
                new GetMessage(mUrl, this).execute(mExpectedMsg);
            else
                mMessenger.send(android.os.Message.obtain(null,1, mMatches.toArray(new Integer[mMatches.size()])));
        }
    }

    private final String        mUrl;
    private final int           mLastKnown;
    private final Hashtable<Integer,Boolean>
                                mOwnMessages;

    private final File          mFilesDir;
    private final Messenger     mMessenger;

    private       int           mExpectedMsg;
    private       int           mLastMessage;
    private       Address       mAddr;
    private       List<Integer> mMatches = new ArrayList<Integer>();
}
