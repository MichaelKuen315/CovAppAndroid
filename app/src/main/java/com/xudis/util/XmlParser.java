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
package com.xudis.util;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import static java.lang.Integer.*;

// https://developer.android.com/training/basics/network-ops/xml
public class XmlParser {

    public static XmlParser create(InputStream in) throws XmlPullParserException, IOException, ParseException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            return new XmlParser(parser);

        } catch(Exception e) {
            Log.e("XmlParser",e.toString());
            return null;
        }
    }

    public XmlParser(XmlPullParser parser) {
        super();

        this.parser = parser;
    }

    public boolean getTag(String tagName) throws IOException, XmlPullParserException {

        while (parser.getEventType() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                parser.next();
                continue;
            }
            if (parser.getName().equals(tagName))
                return true;

            skip();
        }
        // we are on an end_tag
        return false;
    }

    public String nextText() throws XmlPullParserException, IOException {

        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException();

        return parser.nextText();
    }

    public void stepInto() throws XmlPullParserException, IOException {

        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException();

        this.advance();
    }

    public void advance() throws IOException, XmlPullParserException {

        do {
            parser.next();
        } while(parser.getEventType() != XmlPullParser.START_TAG &&
                parser.getEventType() != XmlPullParser.END_TAG   &&
                parser.getEventType() != XmlPullParser.END_DOCUMENT);
    }

    public String getAttr(String name, String _default) {

        String attrValue = parser.getAttributeValue(null,name);
        if(attrValue==null)
            return _default;

        return attrValue;
    }

    public int getAttrInt(String name, int _default) {

        String attrValue = parser.getAttributeValue(null,name);
        if(attrValue==null)
            return _default;

        return parseInt(attrValue);
    }


    private void skip() throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    private final XmlPullParser parser;
}
