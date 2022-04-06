package com.oseamiya.baserow;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Utility {
    public void DoHttpRequest(final String urls, final String token, final Callback callback) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection http = null;
                InputStream inputStream = null;
                try {
                    URL url = new URL(urls);
                    http = (HttpURLConnection) url.openConnection();
                    http.setRequestProperty("Authorization", "Token " + token);
                    int responseCode = http.getResponseCode();
                    System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                    if (responseCode / 100 == 2) {
                        inputStream = http.getInputStream();
                        String res = Utility.this.convertStreamToString(inputStream);
                        if (res != null) {
                            callback.onSuccess(res);
                        }
                    } else {
                        String res = Utility.this.convertStreamToString(http.getErrorStream());
                        if (res != null) {
                            callback.onError(res);
                        }
                    }
                } catch (Exception e) {
                    callback.onError(e.getClass().getCanonicalName());
                } finally {
                    if (http != null) {
                        http.disconnect();
                    }
                    if (inputStream != null){
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        })).start();

    }

    public void PostHttpRequest(final String urls, final String token, final String jsonString, final String method, final String jwtToken, final Callback callback) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection httpURLConnection = null;
                InputStream inputStream = null;
                try {
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.setRequestProperty("Content-Type", "application/json");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setRequestMethod(method);
                    if (!token.equals("")) {
                        httpURLConnection.setRequestProperty("Authorization", "Token " + token);
                    }
                    if (!jwtToken.equals("")) {
                        httpURLConnection.setRequestProperty("Authorization", "JWT " + jwtToken);
                    }
                    if (!jsonString.equals("")) {
                        byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);
                        OutputStream outputStream = httpURLConnection.getOutputStream();
                        outputStream.write(out);
                        outputStream.close();
                    }
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode / 100 == 2) {
                        inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                        String res = convertStreamToString(inputStream);
                        if(res != null){
                            callback.onSuccess(res);
                        }
                    } else {
                        String res = convertStreamToString(httpURLConnection.getErrorStream());
                        if(res != null){
                            callback.onError(res);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e.getClass().getCanonicalName());
                } finally {
                    try {
                        if(inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        })).start();
    }
    

    private String convertStreamToString(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
			if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
