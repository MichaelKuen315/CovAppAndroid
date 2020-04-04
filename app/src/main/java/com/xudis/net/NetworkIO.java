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

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// https://stackoverflow.com/questions/54211988/how-to-construct-correct-multipartentity-to-send-a-multipart-related-request-in


// https://stackoverflow.com/questions/21553507/multipart-form-data-construction-with-android
// android http sample
public abstract class NetworkIO<A,P,R> extends AsyncTask<A, P, Result<R>> {

    private final static String TAG = "NetworkIO";

    private final String        mUrl;
    private final String        boundary     = UUID.randomUUID().toString();
    private static final String LINE_FEED    = "\r\n";
    private HttpURLConnection   mConnection;
    private String              charset;
    private OutputStream        outputStream;
    private PrintWriter         mWriter;

    private INextStep<R>        mNextStep;

    public NetworkIO(String url, String charset, INextStep<R> nextStep) {
        super();

        this.mUrl      = url;
        this.charset   = charset;
        this.mNextStep = nextStep;
    }

    @Override
    protected Result<R> doInBackground(A... args) {

        Result<R> result = null;

        if (!isCancelled() && args != null && args.length>0) {
            try {
                R r = perform(args[0]);
                result = new Result(r);
            } catch(Exception e) {
                result = new Result(e);
            }
        }
        return result;
    }

    @Override
//    @MainThread
    protected void onPostExecute(Result<R> result) {

        if(isCancelled())
            mNextStep.canceled();
        else
            try {
                mNextStep.next(result.getValue());
            }
            catch(Exception e) {
                mNextStep.exception(e);
            }
    }

    protected abstract R perform(A a) throws Exception;

    protected void init() throws IOException {

        URL url = new URL(mUrl);

        mConnection = (HttpURLConnection) url.openConnection();
        mConnection.setUseCaches(false);
        mConnection.setDoOutput(true); // indicates POST method
        mConnection.setDoInput(true);
        mConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"");
        mConnection.setRequestProperty("Accept-Encoding", "gzip");
        outputStream = mConnection.getOutputStream();

        mWriter = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

    public void addFormField(String name, String value) {
        addFormField(name,value,null);
    }

    public void addFormField(String name, String value, String contentType) {

        mWriter.append("--").append(boundary).append(LINE_FEED);
        mWriter.append("Content-Disposition: form-data; name=\""+name+"\"").append(LINE_FEED);

        if(contentType!=null)
            mWriter.append("Content-Type: \""+contentType+"\"").append(LINE_FEED);

        mWriter.append(LINE_FEED);
        mWriter.flush();

        try {
            outputStream.write(value.getBytes());
            outputStream.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mWriter.append(LINE_FEED);
    }

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        mWriter.append(name + ": " + value).append(LINE_FEED);
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public InputStream getResponseStream() throws IOException {

        int status = 0;

        mWriter.flush();
        mWriter.append("--").append(boundary).append("--").append(LINE_FEED);
        mWriter.println();
        mWriter.close();

        // checks server's status code first
        status = mConnection.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK)
            return mConnection.getInputStream();
        else
            throw new IOException("Server returned non-OK status: " + status + " : " + mConnection.getResponseMessage());
    }

    public String getResponseString() throws IOException {

        String response = "";
        int status = 0;

        InputStream inputStream = getResponseStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            response += line;
        }
        reader.close();
        mConnection.disconnect();

        return response;
    }

    public void disconnect() {
        mConnection.disconnect();
    }

    private String printErrorStream() throws IOException {
        InputStream errorStream = mConnection.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
        String errLine = "", tempLine;
        while ((tempLine = reader.readLine()) != null) {
            errLine += tempLine;
        }

        return errLine;
    }

    private void getRequestHeaders(HttpURLConnection httpURLConnection) {
        for (Map.Entry<String, List<String>> entries : httpURLConnection.getRequestProperties().entrySet()) {
            String values = "";
            for (String value : entries.getValue()) {
                values += value + ",";
            }
            Log.v(TAG,"Request" + " " + entries.getKey() + " - " +  values );
        }
    }
}
