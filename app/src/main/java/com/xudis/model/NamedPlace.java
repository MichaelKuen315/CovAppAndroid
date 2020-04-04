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
package com.xudis.model;

import android.util.Log;

import com.xudis.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

public class NamedPlace {

    public NamedPlace(String placeName, int rssi, int txPower) {
        super();

        this.mPlaceName = placeName;
        this.rssi        = rssi;
        this.txPower     = txPower;
    }

    public void join() {

        Iterator<Observation> itObservations = mObservations.iterator();

        Observation prev = itObservations.next();
        while(itObservations.hasNext()) {
            Observation obs = itObservations.next();

            if(prev.isImmediateBefore(obs)) {
                prev.extendWith(obs);
                itObservations.remove();
            }
            else
                prev = obs;
        }
    }

    @Override
    public boolean equals(Object o) {
        return this.mPlaceName.equals(((NamedPlace)o).mPlaceName);
    }

    @Override
    public int hashCode() {
        return this.mPlaceName.hashCode();
    }

    public void updateWith(NamedPlace placeInfo) {

        if(placeInfo.rssi>this.rssi)
            this.rssi = placeInfo.rssi;
    }

    public void addObservation(Observation obs) {
        mObservations.add(obs);
    }

    public boolean intersects(NamedPlace namedPlace) {

        int diffLimit;

        if (this.rssi < -80 && namedPlace.rssi < -80)
            return false;

        if (this.rssi < -50 || namedPlace.rssi < -50)
            diffLimit = 10;
        else
            diffLimit = 6;

        int diff = Math.abs(this.rssi-namedPlace.rssi);

        Log.v("NamedPlace",mPlaceName+"("+rssi+") ("+namedPlace.rssi+") rssi diff:"+diff);

        if(diff>diffLimit)
            return false;

        boolean result = false;

        Iterator<Observation> it1 = this.iterateObservations();
        Iterator<Observation> it2 = namedPlace.iterateObservations();

        Observation obs1;
        if(it1.hasNext())
            obs1 = it1.next();
        else
            return false;

        Observation obs2;
        if(it2.hasNext())
            obs2 = it2.next();
        else
            return false;

        while(true) {

            if(obs1.intersects(obs2))
                result = true;

            if(obs1.endsLaterThan(obs2))
                if(it2.hasNext())
                    obs2 = it2.next();
                else
                    return result;

            else
                if(it1.hasNext())
                  obs1 = it1.next();
                else
                  return result;
        }
    }

    public Iterator<Observation> iterateObservations() {
        return mObservations.iterator();
    }

    public String getPlaceName() {
        return mPlaceName;
    }

    public static NamedPlace decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("named-place")) {

            NamedPlace namedPlace = new NamedPlace(parser.getAttr("place-name",null),
                                                   parser.getAttrInt("rssi", Integer.MIN_VALUE),
                                                   parser.getAttrInt("txpower",Integer.MAX_VALUE));

            parser.stepInto();

            Observation obs = Observation.decode(parser);
            while(obs!=null) {
                namedPlace.addObservation(obs);
                obs = Observation.decode(parser);
            }
            parser.advance();

            return namedPlace;
        }
        else
            return null;
    }

    public void xmlWrite(Writer fw) throws IOException {

        if(txPower> Integer.MIN_VALUE)
            fw.write("<named-place place-name=\""+ mPlaceName +"\" rssi=\""+rssi+"\" tx=\""+txPower+"\">");
        else
            fw.write("<named-place place-name=\""+ mPlaceName +"\" rssi=\""+rssi+"\">");

        Iterator<Observation> itObs = mObservations.iterator();
        while(itObs.hasNext())
            itObs.next().xmlWrite(fw);

        fw.write("</named-place>");
    }

    private final String   mPlaceName;
    private       int      rssi;
    private final int      txPower;

    private final ArrayList<Observation> mObservations = new ArrayList<Observation>();
}
