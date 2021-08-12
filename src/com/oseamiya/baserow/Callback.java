package com.oseamiya.baserow;

public interface Callback {
    void onError(final String error);

    void onSuccess(final String result);
}
