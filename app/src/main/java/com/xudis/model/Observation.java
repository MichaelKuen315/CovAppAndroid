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

import com.xudis.CaptureService;
import com.xudis.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class Observation {

    public static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");

    public Observation(TimeInterval ti, ArrayList<NamedPlace> namedPlaces) {
        super();

        this.mTimeInterval = ti;
        this.mNamedPlaces  = namedPlaces;
    }

    public boolean intersects(Observation o) {
        return mTimeInterval.intersects(o.mTimeInterval);
    }

    public boolean startsBefore(Date date) {
        return mTimeInterval.from.getTime()<date.getTime();
    }

    public boolean endsAfter(Date date) {
        return mTimeInterval.to.getTime()>date.getTime();
    }

    public boolean endsLaterThan(Observation o1) {
        return mTimeInterval.to.getTime()>o1.mTimeInterval.to.getTime();
    }

    public boolean isImmediateBefore(Observation obs) {
        return this.mTimeInterval.isImmediateBefore(obs.mTimeInterval, CaptureService.CAPTURE_PERIOD);
    }

    public void extendWith(Observation obs) {
        assert(mNamedPlaces.isEmpty());

        this.mTimeInterval.extendWith(obs.mTimeInterval);
    }

    public ArrayList<NamedPlace> getNamedPlaces() {
        return mNamedPlaces;
    }

    public void flushKnownPlaces() {
        mNamedPlaces = new ArrayList<NamedPlace>();
    }

    public void xmlWrite(Writer fw) throws IOException {

        fw.write("<observation from=\""+formatter.format(mTimeInterval.from)+"\" to=\""+formatter.format(mTimeInterval.to)+"\">");

        Iterator<NamedPlace> itNamedPlaces = mNamedPlaces.iterator();
        while(itNamedPlaces.hasNext())
            itNamedPlaces.next().xmlWrite(fw);

        fw.write("</observation>");
    }

    public static Observation decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("observation")) {

            Date dateFrom = formatter.parse(parser.getAttr("from",null));
            Date dateTo   = formatter.parse(parser.getAttr("to",null));

            TimeInterval timeInterval = new TimeInterval(dateFrom,dateTo);

            ArrayList<NamedPlace> namedPlaces = new ArrayList<NamedPlace>();
            parser.stepInto();

            NamedPlace namedPlace = NamedPlace.decode(parser);

            while(namedPlace!=null) {

                namedPlaces.add(namedPlace);
                namedPlace = NamedPlace.decode(parser);
            }
            parser.advance();

            return new Observation(timeInterval,namedPlaces);
        }
        else
            return null;
    }

    private final TimeInterval          mTimeInterval;
    private       ArrayList<NamedPlace> mNamedPlaces;
}
