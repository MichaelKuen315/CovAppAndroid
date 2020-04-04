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

public class Message {

    public Message(Address dest, String body) {
        super();

        this.mDest     = dest;
        this.mBody     = body;
    }

    public void xmlWrite(Writer fw) throws IOException {

        fw.write("<message>");
        fw.write("<dest>");
        mDest.xmlWrite(fw);
        fw.write("</dest>");

        fw.write("<body>"+mBody+"</body>");

        fw.write("</message>");
    }

    public static Message decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("message")) {

            parser.stepInto();

            if(parser.getTag("dest")) {

                parser.stepInto();
                Address addr = Address.decode(parser);
                parser.advance(); // /dest

                parser.getTag("body");
                String body = parser.nextText();
                parser.advance();

                parser.advance(); // /message

                return new Message(addr, body);
            }
            else
                return null;
        }
        return null;
    }

    public Address getDestination() {
        return mDest;
    }

    public String getBody() {
        return mBody;
    }

    private Address  mDest;
    private String[] meta; // include e.g. senders phone number if agreed by sender
    private String   mBody;
}
