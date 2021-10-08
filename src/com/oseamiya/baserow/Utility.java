package com.oseamiya.baserow;
import android.net.ParseException;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;


public class Utility {
    public void DoHttpRequest(String urls, String token, Callback callback) {
        (new Thread(() -> {
            HttpURLConnection http = null;
            try {
                URL url = new URL(urls);
                http = (HttpURLConnection) url.openConnection();
                http.setRequestProperty("Authorization", "Token " + token);
                int responseCode = http.getResponseCode();
                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                if (responseCode / 100 == 2) {
                    InputStream inputStream = http.getInputStream();
                    callback.onSuccess(convertStreamToString(inputStream));
                } else {
                    callback.onError(convertStreamToString(http.getErrorStream()));
                }
            } catch (Exception e) {
                callback.onError(e.getClass().getCanonicalName());
            } finally {
                if (http != null) {
                    http.disconnect();
                }
            }
        })).start();

    }

    public void PostHttpRequest(String urls, String token, String jsonString, String method, String jwtToken, Callback callback) {
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
                        callback.onSuccess(Utility.this.convertStreamToString(inputStream));
                    } else {
                        callback.onError(Utility.this.convertStreamToString(httpURLConnection.getErrorStream()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onError(e.getClass().getCanonicalName());
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    httpURLConnection.disconnect();
                }
            }
        })).start();
    }

    public void multipartRequest(String urlTo, Map<String, String> parmas, String filepath, String filefield, String fileMimeType, String jwtToken, Callback callback) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                DataOutputStream outputStream = null;
                InputStream inputStream = null;

                String twoHyphens = "--";
                String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
                String lineEnd = "\r\n";

                String result = "";

                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;

                String[] q = filepath.split("/");
                int idx = q.length - 1;

                try {
                    File file = new File(filepath);
                    FileInputStream fileInputStream = new FileInputStream(file);

                    URL url = new URL(urlTo);
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    connection.setRequestProperty("Authorization", "JWT "+ jwtToken);

                    outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
                    outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
                    outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

                    outputStream.writeBytes(lineEnd);

                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        outputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    outputStream.writeBytes(lineEnd);

                    // Upload POST Data
                    Iterator<String> keys = parmas.keySet().iterator();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = parmas.get(key);

                        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                        outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                        outputStream.writeBytes(lineEnd);
                        outputStream.writeBytes(value);
                        outputStream.writeBytes(lineEnd);
                    }

                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                    if (200 != connection.getResponseCode()) {
                        throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
                    }

                    inputStream = connection.getInputStream();

                    result = convertStreamToString(inputStream);

                    fileInputStream.close();
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();

                    callback.onSuccess(result);
                } catch (Exception e) {
                    callback.onError(e.getClass().getCanonicalName());
                }
            }
        }).start();
    }
    private String convertStreamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
