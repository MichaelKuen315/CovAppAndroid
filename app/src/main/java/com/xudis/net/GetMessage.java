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
import com.xudis.model.Message;

import java.io.InputStream;

public class GetMessage extends NetworkIO<Integer,Void, Message> {

    GetMessage(String mUrl, INextStep<Message> nextStep) {
        super(mUrl, "UTF-8",nextStep);
    }

    public Message perform(Integer id) throws Exception {

        init();
        addFormField("action", "READ");
        addFormField("id", ""+id);

        InputStream stream = getResponseStream();

        Message msg = Message.decode(XmlParser.create(stream));

        disconnect();

        return msg;
    }
}
