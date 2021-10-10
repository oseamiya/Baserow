package com.oseamiya.baserow;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;



public class Utility {
    public void DoHttpRequest(String urls ,String token,Callback callback ){
        (new Thread(() -> {
            HttpURLConnection http = null;
            try {
                URL url = new URL(urls);
                http = (HttpURLConnection) url.openConnection();
                http.setRequestProperty("Authorization", "Token " + token);
                int responseCode = http.getResponseCode();
                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                if(responseCode / 100 == 2){
                    InputStream inputStream = http.getInputStream();
                    callback.onSuccess(convertStreamToString(inputStream));
                }else {
                    callback.onError(convertStreamToString(http.getErrorStream()));
                }
            } catch (Exception e){
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
