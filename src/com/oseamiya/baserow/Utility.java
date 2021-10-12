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

    public void RequestMultipart(String filePath, String urls,  String jwtToken, String fieldName, Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String boundary = "==" + System.currentTimeMillis() + "===" ;
                String LINE_FEED = "\r\n";
                HttpURLConnection httpURLConnection = null;
                OutputStream outputStream;
                PrintWriter printWriter;
                FileInputStream fileInputStream = null;
                File file = new File(filePath);
                try {
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    httpURLConnection.setRequestProperty("Authorization", "JWT "+ jwtToken);
                    httpURLConnection.setRequestMethod("POST");
                    outputStream = httpURLConnection.getOutputStream();
                    printWriter = new PrintWriter(new OutputStreamWriter(outputStream , StandardCharsets.UTF_8), true);
                    String fileName = file.getName();
                    printWriter.append("--").append(boundary).append(LINE_FEED);
                    printWriter.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
                    // To get file mime type
                    InputStream inputStreamForFileMime = new BufferedInputStream(new FileInputStream(file));
                    String mimeType = URLConnection.guessContentTypeFromStream(inputStreamForFileMime);
                    inputStreamForFileMime.close();
                    printWriter.append("Content-Type: ").append(mimeType).append(LINE_FEED);
                    printWriter.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                    printWriter.flush();

                    fileInputStream = new FileInputStream(file);
                    byte[] arrBy = new byte[4096];
                    int len;
                    while((len = fileInputStream.read(arrBy)) != -1){
                        outputStream.write(arrBy , 0 , len);
                    }
                    outputStream.flush();
                    outputStream.close();

                    
                    printWriter.append("--").append(boundary).append("--").append(LINE_FEED);
                    printWriter.flush();
                    printWriter.close();

                    int responseCode = httpURLConnection.getResponseCode();
                    if(responseCode/100 == 2){
                        InputStream inputStream = httpURLConnection.getInputStream();
                        String result = convertStreamToString(inputStream);
                        if(result != null){
                            callback.onSuccess(result);
                        }else{
                            callback.onSuccess("Null Result");
                        }
                        inputStream.close();
                    } else{
                      InputStream errorStream = httpURLConnection.getErrorStream();
                      String result = convertStreamToString(errorStream);
                      if(result != null){
                          callback.onError(result);
                      }else{
                          callback.onError("Null result");
                      }
                      errorStream.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if(fileInputStream != null){
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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
