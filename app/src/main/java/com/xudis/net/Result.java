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

import java.io.IOException;

// wrappes a result of type R or an exception
public class Result<R> {

    public R         mResultValue;
    public Exception mException;

    public Result(R resultValue) {
        super();

        mResultValue = resultValue;
    }
    public Result(Exception exception) {
        super();

        mException = exception;
    }

    public R getValue() throws Exception {

        if(mResultValue!=null)
            return mResultValue;

        if(mException!=null)
            throw mException;

        throw new IOException("no result");
    }
}
