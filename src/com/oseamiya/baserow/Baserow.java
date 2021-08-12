package com.oseamiya.baserow;


import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

import android.content.Context;
import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;


public class Baserow extends AndroidNonvisibleComponent {
    private final Context context;
    private final Activity activity;
    private int tableId;
    private String token;
    private final Utility utility;
    private String accessUrl;

    public Baserow(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = (Activity) container.$context();
        tableId = 26314;
        utility = new Utility();
        accessUrl = "https://api.baserow.io/";
    }
    @DesignerProperty()
    @SimpleProperty
    public void TableId(int id) {
        tableId = id;
    }
    @DesignerProperty(defaultValue = "https://api.baserow.io/")
    @SimpleProperty
    public void Url(String url){
        accessUrl = url;
    }
    @DesignerProperty()
    @SimpleProperty
    public void Token(String tok) {
        token = tok;
    }
    @SimpleEvent
    public void OnFieldsListed(YailList ids , YailList names , YailList types, YailList isPrimary ,int tableId ){
        EventDispatcher.dispatchEvent(this , "OnFieldsListed" , ids , names , types , isPrimary ,tableId);
    }
    @SimpleEvent
    public void OnError(String errorMessage, String errorFrom){
        EventDispatcher.dispatchEvent(this , "OnError" , errorMessage , errorFrom);
    }
    @SimpleEvent
    public void GotCell(String value , String response){
        EventDispatcher.dispatchEvent(this , "GotCell" , value, response);
    }
    @SimpleFunction
    public void GetCell(int rowId , String columnName){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, token, new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "GetCell");
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Object object = jsonObject.get(columnName);
                        GotCell(object.toString() , result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        OnError(e.getClass().getCanonicalName() , "GetCell");
                    }
                });

            }
        });
    }
    @SimpleFunction
    public void GetListFields() {
        String urlRequired = accessUrl + "api/database/fields/table/" + Integer.toString(tableId) + "/";
        utility.DoHttpRequest(urlRequired, token,  new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "GetListFields"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> {
                    ArrayList<String> nameList = new ArrayList<>();
                    ArrayList<String> typeList = new ArrayList<>();
                    ArrayList<Integer> idList = new ArrayList<>();
                    ArrayList<Boolean> isPrimaryList = new ArrayList<>();
                    try {
                        JSONArray jsonArray = new JSONArray(result);
                        for(int i=0 ; i< jsonArray.length() ; i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            nameList.add(jsonObject.getString("name"));
                            typeList.add(jsonObject.getString("type"));
                            idList.add(jsonObject.getInt("id"));
                            isPrimaryList.add(jsonObject.getBoolean("primary"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    OnFieldsListed(YailList.makeList(idList) , YailList.makeList(nameList) , YailList.makeList(typeList) , YailList.makeList(isPrimaryList) , tableId);
                });
            }
        });
    }
    @SimpleEvent
    public void GotColumn(YailList values, String response){
        EventDispatcher.dispatchEvent(this , "GotColumn" , values);
    }
    @SimpleFunction
    public void GetColumn(String columnName, int page, int size){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true" + "/?page=" + page + "/?size=" + size;
        utility.DoHttpRequest(url, token, new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "GetColumn");
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray jsonArray = jsonObject.getJSONArray("results");
                        ArrayList<String> arrayList = new ArrayList<>();
                        for(int i=0; i< jsonArray.length() ; i++){
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            Object resultData = jsonObject1.get(columnName);
                            arrayList.add(resultData.toString());
                        }
                        GotColumn(YailList.makeList(arrayList) , result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        OnError(e.getClass().getCanonicalName() , "GetColumn");
                    }
                });
            }
        });
    }
    @SimpleEvent
    public void GotAllRows(YailList values, int numberOfRows, String response){
        EventDispatcher.dispatchEvent(this , "GotAllRows" , values, numberOfRows , response);
    }
    @SimpleFunction
    public void GetAllRows(int page, int size){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true" + "/?page=" + page + "/?size=" + size;
        utility.DoHttpRequest(url, token, new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "GetAllRows");
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray jsonArray = jsonObject.getJSONArray("results");
                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                        for(int i=0; i<jsonArray.length(); i++){
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            arrayList.add(jsonObject1);
                        }
                        ArrayList<String> arrayList1 = new ArrayList<>();
                        Iterator<String> iterator = arrayList.get(0).keys();
                        for(Iterator<String> it = iterator ; it.hasNext();){
                            arrayList1.add(it.next());
                        }
                        arrayList1.remove("id");
                        arrayList1.remove("order");
                        ArrayList<YailList> arrayLists = new ArrayList<>();
                        for(JSONObject jsonObject1 : arrayList){
                            ArrayList<String> arrayList2 = new ArrayList<>();
                            for(String arr : arrayList1){
                                arrayList2.add(jsonObject1.get(arr).toString());
                            }
                            arrayLists.add(YailList.makeList(arrayList2));
                        }
                        GotAllRows(YailList.makeList(arrayLists) , arrayLists.size() + 1 , result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                });
            }
        });
    }
    @SimpleEvent
    public void GotRow(YailList values , String response){
        EventDispatcher.dispatchEvent(this , "GotRow" , values);
    }
    @SimpleFunction
    public void GetRow(int rowId){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, token, new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "GetRow"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Iterator<String> iterator = jsonObject.keys();
                        ArrayList<String> arrayList = new ArrayList<>();
                        for(Iterator<String> it = iterator; it.hasNext();){
                            arrayList.add(it.next());
                        }
                        arrayList.remove("id");
                        arrayList.remove("order");
                        GotRow(YailList.makeList(arrayList) , result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                });
            }
        });
    }
    @SimpleEvent
    public void RowCreated(String response){
        EventDispatcher.dispatchEvent(this , "RowCreated" , response);
    }
    @SimpleFunction
    public void CreateRow(YailList columns , YailList values){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true";
        String[] list = columns.toStringArray();
        String[] value = values.toStringArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for(int i=0 ; i<list.length ; i++){
            stringBuilder.append("\"").append(list[i]).append("\"").append(":").append("\"").append(value[i]).append("\"").append(",");
        }
        stringBuilder.replace(stringBuilder.length() - 1 , stringBuilder.length() , "}");
        utility.PostHttpRequest(url, token, stringBuilder.toString(), "POST", new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "CreateRow"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> RowCreated(result));
            }
        });
    }
    @SimpleEvent
    public void RowUpdated(String response){
        EventDispatcher.dispatchEvent(this , "RowUpdated" , response);
    }
    @SimpleFunction
    public void UpdateRow(int rowId , YailList columns , YailList values){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        String[] list = columns.toStringArray();
        String[] value = values.toStringArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for(int i=0 ; i<list.length ; i++){
            stringBuilder.append("\"").append(list[i]).append("\"").append(":").append("\"").append(value[i]).append("\"").append(",");
        }
        stringBuilder.replace(stringBuilder.length() - 1 , stringBuilder.length() , "}");
        utility.PostHttpRequest(url, token, stringBuilder.toString(), "PATCH", new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "UpdateRow"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> RowUpdated(result));
            }
        });
    }
    @SimpleEvent
    public void RowMoved(String response){
        EventDispatcher.dispatchEvent(this , "RowMoved", response);
    }
    @SimpleFunction
    public void MoveRow(int rowId , int beforeId){
        String url = accessUrl + "api/database/rows/table/"+ tableId + "/" + rowId + "/move/?user_field_names=true?before_id=" + beforeId;
        utility.PostHttpRequest(url, token, "", "PATCH", new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "MoveRow"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> RowMoved(result));
            }
        });
    }
    @SimpleEvent
    public void RowDeleted(String response){
        EventDispatcher.dispatchEvent(this , "RowDeleted" , response);
    }
    @SimpleFunction
    public void DeleteRow(int rowId){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/";
        utility.PostHttpRequest(url, token, "", "DELETE", new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> OnError(error , "DeleteRow"));
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(() -> RowDeleted("Success Deletion of " + rowId));
            }
        });
    }
}