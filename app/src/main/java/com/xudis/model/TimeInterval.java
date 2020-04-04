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

import com.xudis.CaptureService;
import com.xudis.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;

public class TimeInterval {

    public TimeInterval(Date from, Date to) {
        super();

        this.from = from;
        this.to   = to;
    }

    public boolean isImmediateBefore(TimeInterval interv, long capturePeriod) {

        if((interv.from.getTime() - this.to.getTime()) < capturePeriod+500) // 500 - scheduling jitter
            return true;
        else
            return false;
    }

    public void extendWith(TimeInterval interv) {
        this.to = interv.to;
    }

    /*
    https://scicomp.stackexchange.com/questions/26258/the-easiest-way-to-find-intersection-of-two-intervals
     */
    public boolean intersects(TimeInterval ti) {

        if( ti.from.getTime()>to.getTime()-1500 ||
            from.getTime()>ti.to.getTime()-1500)
            return false;

        Date dbgFrom = new Date(Math.max(ti.from.getTime(),from.getTime()));
        Log.v("TimeInterval","from:"+ Observation.formatter.format(dbgFrom));

        Date dbgTo = new Date(Math.min(ti.to.getTime(),to.getTime()));
        Log.v("TimeInterval","to:"+Observation.formatter.format(dbgTo));

        return true;
    }

    public static TimeInterval decode(XmlParser parser) throws XmlPullParserException, IOException, ParseException {

        if(parser.getTag("time-interval")) {

            TimeInterval ti = new TimeInterval(Observation.formatter.parse(parser.getAttr("from",null)),
                                               Observation.formatter.parse(parser.getAttr("to",null)));

            parser.advance();

            return ti;
        }
        else
            return null;
    }

    public void xmlWrite(Writer fw) throws IOException {

        fw.write("<time-interval from=\"" + Observation.formatter.format(from) + "\" to=\"" + Observation.formatter.format(to) + "\"/>");
    }

    public Date from;
    public Date to;
}
