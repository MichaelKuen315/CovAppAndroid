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

import com.xudis.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

// accessor and utility functions for file stored on device
public class Observations {

    public Observations(ArrayList<Observation> observations) {
        super();

        this.mObservations = observations;
    }

    public Address buildAddress(Date from, Date to) {

        Hashtable<String,NamedPlace> knownPlaces = new Hashtable<String,NamedPlace>();

        Iterator<Observation> itObservations = mObservations.iterator();
        while(itObservations.hasNext()) {
            Observation obs = itObservations.next();

            if(obs.endsAfter(from) && obs.startsBefore(to)) {

                Iterator<NamedPlace> itNamedPlaces = obs.getNamedPlaces().iterator();
                while(itNamedPlaces.hasNext()) {
                    NamedPlace namedPlace = itNamedPlaces.next();

                    NamedPlace knownPlace = knownPlaces.get(namedPlace.getPlaceName());
                    if(knownPlace==null) {
                        knownPlaces.put(namedPlace.getPlaceName(),namedPlace);
                        knownPlace = namedPlace;
                    }
                    knownPlace.addObservation(obs);
                }
            }
            obs.flushKnownPlaces();
        }
        Address address = new Address(new ArrayList<NamedPlace>(knownPlaces.values()));
        address.join();

        return address;
    }

    public static Observations decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("observations")) {

            parser.stepInto();

            ArrayList<Observation> lst = new ArrayList<Observation>();

            Observation observation = Observation.decode(parser);

            while(observation!=null) {

              lst.add(observation);
              observation = Observation.decode(parser);
            }
            parser.advance();

            return new Observations(lst);
        }
        else
            return null;
    }

    public static Observations load(File filesDir) throws IOException, ParseException, XmlPullParserException {

        byte[] prefix = "<observations>".getBytes();
        byte[] postfix = "</observations>".getBytes();

        File file = new File(filesDir, "observations.xml");
        if(!file.exists())
            return null;

        FileInputStream fis = new FileInputStream(file);

        int len = (int) file.length();

        byte[] data = new byte[len + prefix.length + postfix.length];

        System.arraycopy(prefix, 0, data, 0, prefix.length);
        System.arraycopy(postfix, 0, data, prefix.length + len, postfix.length);

        fis.read(data, prefix.length, len);
        fis.close();

        ByteArrayInputStream in = new ByteArrayInputStream(data);

        XmlParser parser = XmlParser.create(in);

        return decode(parser);
    }

    private final ArrayList<Observation> mObservations;
}
