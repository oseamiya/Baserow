package com.oseamiya.baserow;


import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;

import android.content.Context;
import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Baserow extends AndroidNonvisibleComponent {
    private final Context context;
    private final Activity activity;
    private int tableId;
    private String apiToken;
    private final Utility utility;
    private String accessUrl;
    private boolean autoReset = true;

    /**
     * Here I didn't use same global variables for GetAllRows & GetColumn method
     * to avoid any errors when users use both methods at the same time.
     */

    // These are the global variables required for GetAllRows Method
    private int sizeForGetAllRows = 0;
    private int whichPageIAmAtForGetAllRows = 1;
    private int countForGetAllRows = 0;
    private final ArrayList<String> listOfRowIdsForGetAllRows;
    private final ArrayList<YailList> listOfValuesForGetAllRows;
    private final ArrayList<String> nameOfColumnsForGetAllRows;
    private final ArrayList<String> listOfResponsesForGetAllRows;
    private boolean isFirstTimeForGetAllRows = true;

    // These are the global variables required for GetColumn Method
    private int sizeForGetColumn = 0;
    private int whichPageIAmAtForGetColumn = 1;
    private int countForGetColumn = 0;
    private final ArrayList<String> listOfRowIdsForGetColumn;
    private final ArrayList<YailList> listOfValuesForGetColumn;
    private final ArrayList<String> nameOfColumnsForGetColumn;
    private final ArrayList<String> listOfResponsesForGetColumn;
    private String urlOfGetColumn;
    private boolean isFirstTimeForGetColumn = true;

    // These are global variables required for search and order
    private String search = "";
    private String order = "";
    // These are global variables required for filters and type
    private final ArrayList<String> fieldIdsOfFilter;
    private final ArrayList<String> valuesOfFilter;
    private final ArrayList<String> filtersOfFilter;
    private String filterType = "";

    public Baserow(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = (Activity) container.$context();
        tableId = 26314;
        utility = new Utility();
        accessUrl = "https://api.baserow.io/";

        listOfRowIdsForGetColumn = new ArrayList<>();
        listOfValuesForGetColumn = new ArrayList<>();
        nameOfColumnsForGetColumn = new ArrayList<>();
        listOfResponsesForGetColumn = new ArrayList<>();

        listOfRowIdsForGetAllRows = new ArrayList<>();
        listOfValuesForGetAllRows = new ArrayList<>();
        nameOfColumnsForGetAllRows = new ArrayList<>();
        listOfResponsesForGetAllRows = new ArrayList<>();

        fieldIdsOfFilter = new ArrayList<>();
        valuesOfFilter = new ArrayList<>();
        filtersOfFilter = new ArrayList<>();
    }

    @DesignerProperty()
    @SimpleProperty
    public void TableId(int id) {
        tableId = id;
    }

    @DesignerProperty(defaultValue = "https://api.baserow.io/")
    @SimpleProperty
    public void Url(String url) {
        accessUrl = url;
    }

    @DesignerProperty()
    @SimpleProperty
    public void Token(String tok) {
        apiToken = tok;
    }

    @DesignerProperty(defaultValue = "True", editorType = "boolean")
    @SimpleProperty
    public void AutoReset(boolean auto) {
        this.autoReset = auto;
    }

    @SimpleEvent
    public void OnFieldsListed(YailList ids, YailList names, YailList types, YailList isPrimary, int tableId) {
        EventDispatcher.dispatchEvent(this, "OnFieldsListed", ids, names, types, isPrimary, tableId);
    }

    @SimpleEvent
    public void OnError(String errorMessage, String errorFrom) {
        EventDispatcher.dispatchEvent(this, "OnError", errorMessage, errorFrom);
    }

    @SimpleEvent
    public void GotCell(String value, String response) {
        EventDispatcher.dispatchEvent(this, "GotCell", value, response);
    }

    @SimpleFunction
    public void GetCell(int rowId, final String columnName) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, apiToken, new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "GetCell");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            Object object = jsonObject.get(columnName);
                            GotCell(object.toString(), result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "GetCell");
                        }
                    }
                });

            }
        });
    }

    @SimpleFunction
    public void GetListFields() {
        String urlRequired = accessUrl + "api/database/fields/table/" + Integer.toString(tableId) + "/";
        utility.DoHttpRequest(urlRequired, apiToken, new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "GetListFields");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> nameList = new ArrayList<>();
                        ArrayList<String> typeList = new ArrayList<>();
                        ArrayList<Integer> idList = new ArrayList<>();
                        ArrayList<Boolean> isPrimaryList = new ArrayList<>();
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                nameList.add(jsonObject.getString("name"));
                                typeList.add(jsonObject.getString("type"));
                                idList.add(jsonObject.getInt("id"));
                                isPrimaryList.add(jsonObject.getBoolean("primary"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        OnFieldsListed(YailList.makeList(idList), YailList.makeList(nameList), YailList.makeList(typeList), YailList.makeList(isPrimaryList), tableId);
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void GotColumn(YailList values, int counts, YailList rowIds, Object response) {
        EventDispatcher.dispatchEvent(this, "GotColumn", values, counts, rowIds, response);
        resetGetColumn();
        if (autoReset) {
            resetFilterAndSearches();
        }
    }

    @SimpleFunction
    public void GetColumn(final String columnName, int page, int size) {
        if (page != 0 && size != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            if (isFirstTimeForGetColumn) {
                stringBuilder.append(accessUrl).append("api/database/rows/table/").append(tableId).append("/?user_field_names=true&page=").append(page).append("&size=");
                stringBuilder.append(Math.min(size, 200));
                this.sizeForGetColumn = size;
                if (page > 1 && size > 200) {
                    OnError("Page greater than 1 can only accept size less than 200", "GetColumn");
                    return;
                } else {
                    whichPageIAmAtForGetColumn = page;
                }
            } else {
                stringBuilder.append(accessUrl).append("api/database/rows/table/").append(tableId).append("/?user_field_names=true&page=").append(whichPageIAmAtForGetColumn).append("&size=200");
            }
            if (!this.search.equals("")) {
                stringBuilder.append("&search=").append(this.search);
            }
            if (!this.order.equals("")) {
                stringBuilder.append("&order_by=").append(this.order);
            }
            for (int i = 0; i < this.filtersOfFilter.size(); i++) {
                stringBuilder.append("&filter__field_").append(fieldIdsOfFilter.get(i)).append("__").append(filtersOfFilter.get(i)).append("=").append(valuesOfFilter.get(i));
            }
            if (!this.filterType.equals("")) {
                stringBuilder.append("&filter_type=").append(this.filterType);
            }
            urlOfGetColumn = stringBuilder.toString();
            utility.DoHttpRequest(urlOfGetColumn, apiToken, new Callback() {
                @Override
                public void onError(final String error) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetGetColumn();
                            OnError(error, "GetColumn");
                        }
                    });
                }

                @Override
                public void onSuccess(final String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listOfResponsesForGetColumn.add(result);
                            try {
                                JSONObject responseObject = new JSONObject(result);
                                countForGetColumn = responseObject.getInt("count");
                                if (sizeForGetColumn > countForGetColumn) {
                                    sizeForGetColumn = countForGetColumn;
                                }
                                JSONArray resultsArray = responseObject.getJSONArray("results");

                                if (isFirstTimeForGetColumn) {
                                    JSONObject firstObject = resultsArray.getJSONObject(0);
                                    Iterator<String> iterator = firstObject.keys();
                                    while (iterator.hasNext()) {
                                        nameOfColumnsForGetColumn.add(iterator.next());
                                    }
                                    nameOfColumnsForGetColumn.remove("id");
                                    nameOfColumnsForGetColumn.remove("order");
                                }

                                ArrayList<JSONObject> listOfJSONObjectInArray = new ArrayList<>();
                                int remainingDataSizeWhatUserNeeds = sizeForGetColumn - listOfRowIdsForGetColumn.size();
                                for (int i = 0; i < (remainingDataSizeWhatUserNeeds > 200 ? resultsArray.length() : remainingDataSizeWhatUserNeeds); i++) {
                                    listOfRowIdsForGetColumn.add(resultsArray.getJSONObject(i).getString("id"));
                                    listOfJSONObjectInArray.add(resultsArray.getJSONObject(i));
                                }

                                for (JSONObject eachJSONObject : listOfJSONObjectInArray) {
                                    ArrayList<String> newArrayList = new ArrayList<>();
                                    for (String eachColumn : nameOfColumnsForGetColumn) {
                                        newArrayList.add(eachJSONObject.get(eachColumn).toString());
                                    }
                                    listOfValuesForGetColumn.add(YailList.makeList(newArrayList));
                                }
                                if (sizeForGetColumn == listOfRowIdsForGetColumn.size()) {
                                    ArrayList<String> columnsString = new ArrayList<>();
                                    if (nameOfColumnsForGetColumn.contains(columnName)) {
                                        int index = nameOfColumnsForGetColumn.indexOf(columnName);
                                        for (YailList items : listOfValuesForGetColumn) {
                                            columnsString.add(items.toStringArray()[index]);
                                        }
                                        GotColumn(YailList.makeList(columnsString), countForGetColumn, YailList.makeList(listOfRowIdsForGetColumn), YailList.makeList(listOfResponsesForGetColumn));
                                    } else {
                                        resetGetColumn();
                                        OnError("Column Not Found", "GetColumn");
                                    }
                                }
                                if (responseObject.getString("next").contains("api")) {
                                    if (sizeForGetColumn > listOfRowIdsForGetColumn.size()) {
                                        whichPageIAmAtForGetColumn = whichPageIAmAtForGetColumn + 1;
                                        isFirstTimeForGetColumn = false;
                                        GetColumn(columnName, 1, 1); // Any page or size more than 0 as it won't matter
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                resetGetColumn();
                                OnError(e.getMessage(), "GetColumn");
                            }
                        }
                    });
                }
            });
        }else{
            if (autoReset) {
                resetFilterAndSearches();
            }
            resetGetColumn();
            throw new YailRuntimeError("Page or Size should not be 0", "RuntimeError");
        }
    }

    @SimpleEvent
    public void GotAllRows(YailList values, int counts, YailList rowIds, Object response) {
        EventDispatcher.dispatchEvent(this, "GotAllRows", values, counts, rowIds, response);
        resetGetAllRows();
        if (autoReset) {
            resetFilterAndSearches();
        }
    }

    @SimpleFunction
    public void GetAllRows(int page, int size) {
        if (page != 0 && size != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            if (isFirstTimeForGetAllRows) {
                stringBuilder.append(accessUrl).append("api/database/rows/table/").append(tableId).append("/?user_field_names=true&page=").append(page).append("&size=");
                stringBuilder.append(Math.min(size, 200));
                this.sizeForGetAllRows = size;
                if (page > 1 && size > 200) {
                    OnError("Page greater than 1 can only accept size less than 200", "GetAllRows");
                    return;
                } else {
                    whichPageIAmAtForGetAllRows = page;
                }
            } else {
                stringBuilder.append(accessUrl).append("api/database/rows/table/").append(tableId).append("/?user_field_names=true&page=").append(whichPageIAmAtForGetAllRows).append("&size=200");
            }
            if (!this.search.equals("")) {
                stringBuilder.append("&search=").append(this.search);
            }
            if (!this.order.equals("")) {
                stringBuilder.append("&order_by=").append(this.order);
            }
            for (int i = 0; i < this.filtersOfFilter.size(); i++) {
                stringBuilder.append("&filter__field_").append(fieldIdsOfFilter.get(i)).append("__").append(filtersOfFilter.get(i)).append("=").append(valuesOfFilter.get(i));
            }
            if (!this.filterType.equals("")) {
                stringBuilder.append("&filter_type=").append(this.filterType);
            }
            String urlOfGetAllRows = stringBuilder.toString();
            utility.DoHttpRequest(urlOfGetAllRows, apiToken, new Callback() {
                @Override
                public void onError(final String error) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetGetAllRows();
                            OnError(error, "GetAllRows");
                        }
                    });
                }

                @Override
                public void onSuccess(final String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listOfResponsesForGetAllRows.add(result);
                            try {
                                JSONObject responseObject = new JSONObject(result); // responseObject is first object we get as response
                                countForGetAllRows = responseObject.getInt("count");
                                /**
                                 * User may have given the size more than the total rows since we have totalNo of rows in count
                                 * We will check if size > count then size will be equal to count
                                 */
                                if (sizeForGetAllRows > countForGetAllRows) {
                                    sizeForGetAllRows = countForGetAllRows;
                                }
                                JSONArray resultsArray = responseObject.getJSONArray("results");
                                /**
                                 * When we are at page 1, we get the name of columns in the table,
                                 * So, we get first JSONObject from resultsArray then get keys of that array in iterator
                                 * and add these keys at nameOfColumns arraylist. Later, we will remove id and order from
                                 * the arraylist.
                                 */
                                if (isFirstTimeForGetAllRows) {
                                    JSONObject firstObject = resultsArray.getJSONObject(0);
                                    Iterator<String> iterator = firstObject.keys();
                                    while (iterator.hasNext()) {
                                        nameOfColumnsForGetAllRows.add(iterator.next());
                                    }
                                    nameOfColumnsForGetAllRows.remove("id");
                                    nameOfColumnsForGetAllRows.remove("order");
                                }
                                /**
                                 * Here we get each object at resultsArray and then add id of each object
                                 * Since, when getting data from second or more than second time we may have
                                 * data more than what user has stated, we will only add those data which we need.
                                 */
                                ArrayList<JSONObject> listOfJSONObjectInArray = new ArrayList<>();
                                int remainingDataSizeWhatUserNeeds = sizeForGetAllRows - listOfRowIdsForGetAllRows.size();
                                for (int i = 0; i < (remainingDataSizeWhatUserNeeds > 200 ? resultsArray.length() : remainingDataSizeWhatUserNeeds); i++) {
                                    listOfRowIdsForGetAllRows.add(resultsArray.getJSONObject(i).getString("id"));
                                    listOfJSONObjectInArray.add(resultsArray.getJSONObject(i));
                                }
                                /**
                                 * Here, we are going through each JSONObject available on resultsArray then  we
                                 * create a new arraylist of strings and add all the columns values of each JSONObject
                                 */
                                for (JSONObject eachJSONObject : listOfJSONObjectInArray) {
                                    ArrayList<String> newArrayList = new ArrayList<>();
                                    for (String eachColumn : nameOfColumnsForGetAllRows) {
                                        newArrayList.add(eachJSONObject.get(eachColumn).toString());
                                    }
                                    listOfValuesForGetAllRows.add(YailList.makeList(newArrayList));
                                }
                                /**
                                 * Here, we are checking whether size what user needs is equal to length of rowIds we have now
                                 * If they are equal then Notify user with data
                                 */
                                if (sizeForGetAllRows == listOfRowIdsForGetAllRows.size()) {
                                    GotAllRows(YailList.makeList(listOfValuesForGetAllRows), countForGetAllRows, YailList.makeList(listOfRowIdsForGetAllRows), YailList.makeList(listOfResponsesForGetAllRows));
                                }
                                /**
                                 * If there are any more data available in next page then it will fetch those data if user needs
                                 */
                                if (responseObject.getString("next").contains("api")) {
                                    if (sizeForGetAllRows > listOfRowIdsForGetAllRows.size()) {
                                        whichPageIAmAtForGetAllRows = whichPageIAmAtForGetAllRows + 1;
                                        isFirstTimeForGetAllRows = false;
                                        GetAllRows(1, 1);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                resetGetAllRows();
                                OnError(e.getMessage(), "GetAllRows");
                            }
                        }
                    });
                }
            });
        } else {
            resetGetAllRows();
            if (autoReset) {
                resetFilterAndSearches();
            }
            throw new YailRuntimeError("Page or Size should not be 0", "RuntimeError");
        }
    }

    @SimpleEvent
    public void GotRow(YailList values, String response) {
        EventDispatcher.dispatchEvent(this, "GotRow", values, response);
    }

    @SimpleFunction
    public void GetRow(int rowId) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        utility.DoHttpRequest(url, apiToken, new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "GetRow");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            Iterator<String> iterator = jsonObject.keys();
                            ArrayList<String> arrayList = new ArrayList<>();
                            while (iterator.hasNext()) {
                                arrayList.add(jsonObject.getString(iterator.next()));
                            }
                            arrayList.remove(arrayList.get(0));
                            arrayList.remove(arrayList.get(0)); // Actually i did it two times so that first two word of origin arraylist removes
                            GotRow(YailList.makeList(arrayList), result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "GetRow");
                        }
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void RowCreated(String response) {
        EventDispatcher.dispatchEvent(this, "RowCreated", response);
    }

    @SimpleFunction
    public void CreateRow(YailList columns, YailList values) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/?user_field_names=true";
        String[] list = columns.toStringArray();
        String[] value = values.toStringArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (int i = 0; i < list.length; i++) {
            stringBuilder.append("\"").append(list[i]).append("\"").append(":").append("\"").append(value[i]).append("\"").append(",");
        }
        stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "}");
        utility.PostHttpRequest(url, apiToken, stringBuilder.toString(), "POST", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "CreateRow");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RowCreated(result);
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void RowUpdated(String response) {
        EventDispatcher.dispatchEvent(this, "RowUpdated", response);
    }

    @SimpleFunction
    public void UpdateRow(int rowId, YailList columns, YailList values) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/?user_field_names=true";
        String[] list = columns.toStringArray();
        String[] value = values.toStringArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (int i = 0; i < list.length; i++) {
            stringBuilder.append("\"").append(list[i]).append("\"").append(":").append("\"").append(value[i]).append("\"").append(",");
        }
        stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "}");
        utility.PostHttpRequest(url, apiToken, stringBuilder.toString(), "PATCH", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "UpdateRow");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RowUpdated(result);
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void RowMoved(String response) {
        EventDispatcher.dispatchEvent(this, "RowMoved", response);
    }

    @SimpleFunction
    public void MoveRow(int rowId, int beforeId) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/move/?user_field_names=true&before_id=" + beforeId;
        utility.PostHttpRequest(url, apiToken, "", "PATCH", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "MoveRow");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RowMoved(result);
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void RowDeleted(String response) {
        EventDispatcher.dispatchEvent(this, "RowDeleted", response);
    }

    @SimpleFunction
    public void DeleteRow(final int rowId) {
        String url = accessUrl + "api/database/rows/table/" + tableId + "/" + rowId + "/";
        utility.PostHttpRequest(url, apiToken, "", "DELETE", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "DeleteRow");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RowDeleted("Success Deletion of " + rowId);
                    }
                });
            }
        });
    }

    @SimpleProperty
    public void Search(String text) {
        this.search = text;
    }

    @SimpleProperty
    public void OrderAscendingTo(String columnName) {
        this.order = "+" + columnName;
    }

    @SimpleProperty
    public void OrderDescendingTo(String columnName) {
        this.order = "-" + columnName;
    }

    @SimpleProperty
    public void FilterType(String type) {
        this.filterType = type;
    }

    @SimpleFunction
    public void Filter(Object fieldIds, Object filters, Object values) {
        if (fieldIds instanceof YailList && filters instanceof YailList && values instanceof YailList) {
            for (int i = 0; i < (((YailList) fieldIds).toStringArray()).length; i++) {
                this.fieldIdsOfFilter.add(((YailList) fieldIds).toStringArray()[i]);
                this.filtersOfFilter.add(((YailList) filters).toStringArray()[i]);
                this.valuesOfFilter.add(((YailList) values).toStringArray()[i]);
            }
        } else if (fieldIds instanceof String && filters instanceof String && values instanceof String) {
            this.fieldIdsOfFilter.add(String.valueOf(fieldIds));
            this.filtersOfFilter.add(String.valueOf(filters));
            this.valuesOfFilter.add(String.valueOf(values));
        }
    }

    @SimpleEvent
    public void TokenGenerated(String token, String response) {
        EventDispatcher.dispatchEvent(this, "TokenGenerated", token, response);
    }

    @SimpleFunction
    public void GenerateToken(String username, String password) {
        String url = accessUrl + "api/user/token-auth/";
        String jsonWithUserAndPass = "{" + "\"username\"" + ":" + "\"" + username + "\"," + "\"password\": \"" + password + "\"}";
        utility.PostHttpRequest(url, apiToken, jsonWithUserAndPass, "POST", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "GenerateToken");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String awtToken = jsonObject.getString("token");
                            TokenGenerated(awtToken, result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "GenerateToken");
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void RefreshToken(String token) {
        String url = accessUrl + "api/user/token-refresh/";
        String jsonWithToken = "{" + "\"token\":" + "\"" + token + "\"" + "}";
        utility.PostHttpRequest(url, apiToken, jsonWithToken, "POST", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "RefreshToken");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String awtToken = jsonObject.getString("token");
                            TokenGenerated(awtToken, result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "RefreshToken");
                        }
                    }
                });

            }
        });
    }

    @SimpleEvent
    public void TokenVerified(boolean isVerified, String token) {
        EventDispatcher.dispatchEvent(this, "TokenVerified", isVerified, token);
    }

    @SimpleFunction
    public void VerifyToken(final String token) {
        String url = accessUrl + "api/user/token-verify/";
        String jsonWithToken = "{" + "\"token\":" + "\"" + token + "\"" + "}";
        utility.PostHttpRequest(url, apiToken, jsonWithToken, "POST", "", new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TokenVerified(false, token);
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String jwtToken = jsonObject.getString("token");
                            if (token.equals(jwtToken)) {
                                TokenVerified(true, jwtToken);
                            } else {
                                TokenVerified(false, token);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "VerifyToken");
                        }
                    }
                });
            }
        });
    }

    @SimpleEvent
    public void FileUploadedByUrl(int size, String url, String mimeType, boolean isImage, String name, String originalName, String response) {
        EventDispatcher.dispatchEvent(this, "FileUploadedByUrl", size, url, mimeType, isImage, name, originalName, response);
    }

    @SimpleFunction
    public void UploadFileByUrl(String token, String fileUrl) {
        String url = accessUrl + "api/user-files/upload-via-url/";
        String jsonFileUrl = "{\"url\":\"" + fileUrl + "\"}";
        utility.PostHttpRequest(url, "", jsonFileUrl, "POST", token, new Callback() {
            @Override
            public void onError(final String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        OnError(error, "UploadFileByUrl");
                    }
                });
            }

            @Override
            public void onSuccess(final String result) {
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
                            FileUploadedByUrl(sizeOfFile, uploadedFileUrl, mimeType, isImage, name, originalName, result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            OnError(e.getClass().getCanonicalName(), "UploadFileByUrl");
                        }
                    }
                });
            }
        });
    }

    private void resetGetAllRows() {
        sizeForGetAllRows = 0;
        whichPageIAmAtForGetAllRows = 1;
        countForGetAllRows = 0;
        listOfRowIdsForGetAllRows.clear();
        listOfValuesForGetAllRows.clear();
        listOfResponsesForGetAllRows.clear();
        nameOfColumnsForGetAllRows.clear();
        isFirstTimeForGetAllRows = true;
    }

    private void resetGetColumn() {
        sizeForGetColumn = 0;
        whichPageIAmAtForGetColumn = 1;
        countForGetColumn = 0;
        listOfRowIdsForGetColumn.clear();
        listOfValuesForGetColumn.clear();
        nameOfColumnsForGetColumn.clear();
        listOfResponsesForGetColumn.clear();
        isFirstTimeForGetColumn = true;
    }

    private void resetFilterAndSearches() {
        this.fieldIdsOfFilter.clear();
        this.filtersOfFilter.clear();
        this.valuesOfFilter.clear();
        this.search = "";
        this.order = "";
        this.filterType = "";
    }

}
