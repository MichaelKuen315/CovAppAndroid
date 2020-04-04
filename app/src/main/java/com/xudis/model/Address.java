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

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class Address {

    public Address(ArrayList<NamedPlace> namedPlaces) {
        super();

        this.mNamedPlaces = namedPlaces;
    }

    public void join() {

        Iterator<NamedPlace> itPlaces = mNamedPlaces.iterator();
        while(itPlaces.hasNext())
            itPlaces.next().join();
    }

    public boolean intersects(Address addr) {

        boolean result = false;

        Hashtable<String,NamedPlace> lookup = new Hashtable<String,NamedPlace>();
        Iterator<NamedPlace> itNamedPlace = mNamedPlaces.iterator();
        while(itNamedPlace.hasNext()) {
            NamedPlace namedPlace = itNamedPlace.next();

            lookup.put(namedPlace.getPlaceName(),namedPlace);
        }
        itNamedPlace = addr.getNamedPlaces().iterator();
        while(itNamedPlace.hasNext()) {
            NamedPlace namedPlace1 = itNamedPlace.next();
            NamedPlace namedPlace2 = lookup.get(namedPlace1.getPlaceName());

            if(namedPlace2!=null && namedPlace1.intersects(namedPlace2))
                result = true;
        }
        return result;
    }

    public ArrayList<NamedPlace> getNamedPlaces() {
        return mNamedPlaces;
    }

    public void xmlWrite(Writer fw) throws IOException {

        fw.write("<address>");

        Iterator<NamedPlace> itPlaces = mNamedPlaces.iterator();
        while(itPlaces.hasNext())
            itPlaces.next().xmlWrite(fw);

        fw.write("</address>");
    }

    public static Address decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("address")) {

            parser.stepInto();

            ArrayList<NamedPlace> lst = new ArrayList<NamedPlace>();

            NamedPlace namedPlace = NamedPlace.decode(parser);
            while(namedPlace!=null) {
                lst.add(namedPlace);
                namedPlace = NamedPlace.decode(parser);
            }

            parser.advance();

            return new Address(lst);
        }
        return null;
    }

    private final ArrayList<NamedPlace> mNamedPlaces;
}
