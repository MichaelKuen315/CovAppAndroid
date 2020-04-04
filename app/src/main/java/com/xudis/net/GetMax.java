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

import com.xudis.util.XmlParser;

import java.io.InputStream;

import static java.lang.Integer.parseInt;

public class GetMax extends NetworkIO<String,Void,int[]> {

    GetMax(String mUrl, INextStep<int[]> nextStep) {
        super(mUrl, "UTF-8", nextStep);
    }

    public int[] perform(String str) throws Exception {

        init();
        addFormField("action", "GETMAX");

        InputStream stream = getResponseStream();
        XmlParser parser = XmlParser.create(stream);

        parser.getTag("min");
        int min = parseInt(parser.nextText());
        parser.advance();

        parser.getTag("max");
        int max = parseInt(parser.nextText());
        parser.advance();

        disconnect();

        return new int[] {min,max};
    }
}
