package com.oseamiya.baserow;


import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.content.Context;
import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class Baserow extends AndroidNonvisibleComponent {
    private final Context context;
    private final Activity activity;
    private int tableId;
    private String apiToken;
    private final Utility utility;
    private String accessUrl;
    private int valueForAll = 1;
    private int valueForAllRows = 1;
    private String columnNames;
    private final ArrayList<String> allValuesInList;
    private final ArrayList<String> allResponsesInList;
    private ArrayList<Object> allValuesInListRows;
    private ArrayList<Object> allResponsesInListRows;
    private int testValue = 0;
    private int moreSizeOfGetColumn = 1; 
    private int moreSizeOfGetAllRows = 1; 
    // Added after version 3.2 of this extension
    private int countForGotAllRows = 0;
    private ArrayList<String> idListForGotAllRows;
    private int countForGotColumn = 0;
    private ArrayList<String> idListForGotColumn;
    public Baserow(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = (Activity) container.$context();
        tableId = 26314;
        utility = new Utility();
        accessUrl = "https://api.baserow.io/";
	columnNames = null;
	allValuesInList = new ArrayList<String>();
	allResponsesInList = new ArrayList<String>();
        allValuesInListRows = new ArrayList<Object>();
        allResponsesInListRows = new ArrayList<Object>();
	// Added after version 3.2 of this extension, to give all row ids in list to users <--@author-- oseamiya-->
	idListForGotAllRows = new ArrayList<String>();
        idListForGotColumn = new ArrayList<String>();
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
        apiToken = tok;
    }
    @SimpleEvent
    public void OnFieldsListed(YailList ids , YailList names , YailList types, YailList isPrimary ,int tableId ){
        EventDispatcher.dispatchEvent(this , "OnFieldsListed" , ids , names , types , isPrimary ,tableId);
    }
    @SimpleEvent
    public void OnError(String errorMessage, String errorFrom){
        if(errorMessage.contains("ERROR_PAGE_SIZE_LIMIT") || errorMessage.contains("ERROR_INVALID_PAGE")){
            if(columnNames != null) {
                valueForAll = 1;
                columnNames = null;
                GotColumn(YailList.makeList(allValuesInList), countForGotColumn, YailList.makeList(idListForGotColumn),YailList.makeList(allResponsesInList));
            }else if(testValue == 1){
                testValue = 2;
                valueForAllRows = -1;
                GotAllRows(YailList.makeList(allValuesInListRows),countForGotAllRows, YailList.makeList(idListForGotAllRows), YailList.makeList(allResponsesInListRows));
            }
        }else{
             EventDispatcher.dispatchEvent(this , "OnError" , errorMessage , errorFrom);
	}
    }
    @SimpleEvent
    public void GotCell(String value , String response){
        EventDispatcher.dispatchEvent(this , "GotCell" , value, response);
    }
    @SimpleFunction
    public void GetCell(int rowId , String columnName){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, apiToken, new Callback() {
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
        utility.DoHttpRequest(urlRequired, apiToken,  new Callback() {
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
    public void GotColumn(YailList values, int counts, YailList rowIds, Object response){
        EventDispatcher.dispatchEvent(this , "GotColumn" , values ,counts, rowIds, response);
	allValuesInList.clear();
        allResponsesInList.clear();
	idListForGotColumn.clear();
        columnNames = null;
	countForGotColumn = 0;
        Log.d("GotColumn", "All values saved in arraylist is cleared");
    }
    @SimpleFunction
    public void GetColumn(String columnName, int page, int size){
        if(page != 0 && size != 0) {
            if(size <= 200) {
                String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true" + "&page=" + page + "&size=" + size;
                utility.DoHttpRequest(url, apiToken, new Callback() {
                    @Override
                    public void onError(String error) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                OnError(error, "GetColumn");
                            }
                        });
                    }

                    @Override
                    public void onSuccess(String result) {
                        activity.runOnUiThread(() -> {
                            try {
                                JSONObject jsonObject = new JSONObject(result);
                                JSONArray jsonArray = jsonObject.getJSONArray("results");
				countForGotColumn = jsonObject.getInt("count");
                                ArrayList<String> arrayList = new ArrayList<>();
				ArrayList<String> arrayList2 = new ArrayList<>(); // This will store row ids for the column.
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                                    Object resultData = jsonObject1.get(columnName);
			            Object rowIdsDatas = jsonObject1.get("id");
                                    arrayList.add(resultData.toString());
			            arrayList2.add(rowIdsDatas.toString());
                                }
                                if(columnNames == null) {
                                    GotColumn(YailList.makeList(arrayList),countForGotColumn, YailList.makeList(arrayList2),  YailList.makeList(new String[]{result}));
                                }else{
                                    allValuesInList.addAll(arrayList);
                                    allResponsesInList.add(result);
				    idListForGotColumn.addAll(arrayList2);
                                    if(valueForAll != -1){
                                        valueForAll = valueForAll + 1;
                                        if(moreSizeOfGetColumn > allValuesInList.size()){
                                            Log.d("GetColumn", "Size of values loaded till is " + allValuesInList.size());
                                            GetColumn(columnNames, valueForAll, 200);
                                        }else{
                                            ArrayList<String> allValuesInList1 = new ArrayList<>(); // That filter out the data if data is loaded more than of size
				            ArrayList<String> idListForGotColumn1 = new ArrayList<>();
                                            for(int i=0; i<moreSizeOfGetColumn; i++){
                                                //Log.d("Index for removing extra values", String.valueOf(i));
                                                allValuesInList1.add(allValuesInList.get(i));
						idListForGotColumn1.add(idListForGotColumn.get(i));
                                            }
                                            GotColumn(YailList.makeList(allValuesInList1), countForGotColumn, YailList.makeList(idListForGotColumn1),YailList.makeList(allResponsesInList));
                                        }
                                        
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                OnError(e.getClass().getCanonicalName(), "GetColumn");
                            }
                        });
                    }
                });
            }else if(page == 1){
		moreSizeOfGetColumn = size;
                valueForAll = 1;
                HandleAll(columnName);
            }else{
                throw new YailRuntimeError("Your page should be 1 if your size is greater than 200", "RuntimeError");
            }
        }else{
            throw new YailRuntimeError("Page or Size cannot be 0, try page 1 & size is max value you wanted to load", "RuntimeError");
        }
    }
    private void HandleAll(String nameOfColumn){
        columnNames = nameOfColumn;
        GetColumn(columnNames, 1, 200);
    }
    @SimpleEvent
    public void GotAllRows(YailList values, int counts, YailList rowIds, Object response){
        EventDispatcher.dispatchEvent(this , "GotAllRows" , values, counts, rowIds, response);
	testValue = -1;
        allValuesInListRows.clear();
        allResponsesInListRows.clear();
	countForGotAllRows=0;
        idListForGotAllRows.clear();
        Log.d("GotAllRows", "All values saved in arraylist is cleared");
    }
    @SimpleFunction
    public void GetAllRows(int page, int size){
        if(page!=0 && size!=0) {
            if(size <= 200) {
                String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true" + "&page=" + page + "&size=" + size;
                utility.DoHttpRequest(url, apiToken, new Callback() {
                    @Override
                    public void onError(String error) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                OnError(error, "GetAllRows");
                            }
                        });
                    }

                    @Override
                    public void onSuccess(String result) {
                        activity.runOnUiThread(() -> {
                            try {
                                JSONObject jsonObject = new JSONObject(result);
                                JSONArray jsonArray = jsonObject.getJSONArray("results");
				countForGotAllRows = jsonObject.getInt("count"); // it is total no. of rows available in your table
				ArrayList<String> arrayListForRowIds = new ArrayList<>();
                                ArrayList<JSONObject> arrayList = new ArrayList<>();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                                    arrayList.add(jsonObject1);
			            Object rowIdsDatas = jsonObject1.get("id");
                                    arrayListForRowIds.add(rowIdsDatas.toString());
                                }
                                ArrayList<String> arrayList1 = new ArrayList<>();
                                Iterator<String> iterator = arrayList.get(0).keys();
                                for (Iterator<String> it = iterator; it.hasNext(); ) {
                                    arrayList1.add(it.next());
                                }
                                arrayList1.remove("id");
                                arrayList1.remove("order");
                                ArrayList<YailList> arrayLists = new ArrayList<>();
                                for (JSONObject jsonObject1 : arrayList) {
                                    ArrayList<String> arrayList2 = new ArrayList<>();
                                    for (String arr : arrayList1) {
                                        arrayList2.add(jsonObject1.get(arr).toString());
                                    }
                                    arrayLists.add(YailList.makeList(arrayList2));
                                }
                                if(testValue != 1) {
                                    GotAllRows(YailList.makeList(arrayLists), countForGotAllRows, YailList.makeList(arrayListForRowIds) ,YailList.makeList(new String[]{result}));
                                }else{
                                    // size is maximum than 200, this is only executed if page is 1 
                                    allValuesInListRows.addAll(arrayLists);
                                    allResponsesInListRows.add(result);
				    idListForGotAllRows.addAll(arrayListForRowIds);
                                    if(valueForAllRows != -1){
                                        valueForAllRows = valueForAllRows + 1;
                                        if(moreSizeOfGetAllRows > allValuesInListRows.size()){
                                            Log.d("GetAllRows", "Size of values loaded till is " + allValuesInListRows.size());
                                            GetAllRows(valueForAllRows, 200);
                                        }else{
                                            ArrayList<Object> allValuesInList1 = new ArrayList<>(); // That filter out the data if data is loaded more than of size
				            ArrayList<String> idListForGotAllRows1 = new ArrayList<>();
                                            for(int i=0; i<moreSizeOfGetAllRows; i++){
                                                //Log.d("Index for removing extra values", String.valueOf(i));
                                                allValuesInList1.add(allValuesInListRows.get(i));
						idListForGotAllRows1.add(idListForGotAllRows.get(i));
                                            }
                                            GotAllRows(YailList.makeList(allValuesInList1), countForGotAllRows, YailList.makeList(idListForGotAllRows1) ,YailList.makeList(allResponsesInListRows));
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
            }else if(page == 1){
                testValue = 1;
		moreSizeOfGetAllRows = size;
                valueForAllRows = 1;
		GetAllRows(1,200);
            }else{
                throw new YailRuntimeError("Your page should be 1 if your size is greater than 200", "RuntimeError");
            }
        }else{
            throw new YailRuntimeError("Page or Size cannot be 0, try page 1 & size is max value you wanted to load", "RuntimeError");
        }
    }
    @SimpleEvent
    public void GotRow(YailList values , String response){
        EventDispatcher.dispatchEvent(this , "GotRow" , values, response);
    }
    @SimpleFunction
    public void GetRow(int rowId){
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, apiToken, new Callback() {
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
                            arrayList.add(jsonObject.getString(it.next()));
                        }
                        arrayList.remove(arrayList.get(0));
                        arrayList.remove(arrayList.get(0)); // Actually i did it two times so that first two word of origin arraylist removes
                        GotRow(YailList.makeList(arrayList) , result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        OnError(e.getClass().getCanonicalName() , "GetRow");
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
        utility.PostHttpRequest(url, apiToken, stringBuilder.toString(), "POST","", new Callback() {
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
        utility.PostHttpRequest(url, apiToken, stringBuilder.toString(), "PATCH","" ,  new Callback() {
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
        String url = accessUrl + "api/database/rows/table/"+ tableId + "/" + rowId + "/move/?user_field_names=true&before_id=" + beforeId;
        utility.PostHttpRequest(url, apiToken, "", "PATCH", "", new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "MoveRow");
                    }
                });
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
        utility.PostHttpRequest(url, apiToken, "", "DELETE","", new Callback() {
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
    @SimpleEvent
    public void TokenGenerated(String token, String response){
        EventDispatcher.dispatchEvent(this, "TokenGenerated" , token, response);
    }
    @SimpleFunction
    public void GenerateToken(String username, String password){
        String url = accessUrl + "api/user/token-auth/";
        String jsonWithUserAndPass = "{" + "\"username\"" + ":" + "\"" + username + "\"," + "\"password\": \"" + password + "\"}";
        utility.PostHttpRequest(url, apiToken, jsonWithUserAndPass, "POST","" , new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "GenerateToken");
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String awtToken = jsonObject.getString("token");
                            TokenGenerated(awtToken , result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName() , "GenerateToken");
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void RefreshToken(String token){
        String url = accessUrl + "api/user/token-refresh/";
        String jsonWithToken = "{" + "\"token\":" + "\"" + token + "\"" + "}";
        utility.PostHttpRequest(url, apiToken, jsonWithToken, "POST","" ,  new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "RefreshToken");
                    }
                });
            }
            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String awtToken = jsonObject.getString("token");
                            TokenGenerated(awtToken , result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName() , "RefreshToken");
                        }
                    }
                });

            }
        });
    }
    @SimpleEvent
    public void TokenVerified(boolean isVerified, String token){
        EventDispatcher.dispatchEvent(this , "TokenVerified", isVerified, token);
    }
    @SimpleFunction
    public void VerifyToken(String token){
        String url = accessUrl + "api/user/token-verify/";
        String jsonWithToken = "{" + "\"token\":" + "\"" + token + "\"" + "}";
        utility.PostHttpRequest(url, apiToken, jsonWithToken, "POST","" , new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TokenVerified(false , token);
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
               activity.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       try {
                           JSONObject jsonObject = new JSONObject(result);
                           String jwtToken = jsonObject.getString("token");
                           if(token.equals(jwtToken)){
                               TokenVerified(true, jwtToken);
                           }else{
                               TokenVerified(false, token);
                           }
                       } catch (JSONException e) {
                           e.printStackTrace();
                           OnError(e.getClass().getCanonicalName() , "VerifyToken");
                       }
                   }
               });
            }
        });
    }
    @SimpleEvent
    public void FileUploadedByUrl(int size, String url, String mimeType , boolean isImage , String name, String originalName ,String response){
        EventDispatcher.dispatchEvent(this , "FileUploadedByUrl" , size, url, mimeType, isImage, name, originalName, response);
    }
    @SimpleFunction
    public void UploadFileByUrl(String token , String fileUrl){
        String url = accessUrl +"api/user-files/upload-via-url/";
        String jsonFileUrl = "{\"url\":\"" + fileUrl + "\"}";
        utility.PostHttpRequest(url, "", jsonFileUrl, "POST", token, new Callback() {
            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error , "UploadFileByUrl");
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            int sizeOfFile = jsonObject.getInt("size");
                            String uploadedFileUrl = jsonObject.getString("url");
                            String mimeType = jsonObject.getString("mime_type");
                            boolean isImage = jsonObject.getBoolean("is_image");
                            String name = jsonObject.getString("name");
                            String originalName = jsonObject.getString("original_name");
                            FileUploadedByUrl(sizeOfFile ,uploadedFileUrl,  mimeType , isImage, name, originalName, result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "UploadFileByUrl");
                        }
                    }
                });
            }
        });
    }
	
    
}
