package cn.dv4.weeximagecroppicker;

import android.support.annotation.NonNull;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.bridge.JSCallback;

public class ResponseHelper {

    private JSCallback callback;
    private JSONObject response = new JSONObject();

    ResponseHelper(@NonNull final JSCallback callback) {
        this.callback = callback;
    }

    public void cleanResponse() {
        response = new JSONObject();
    }

    public void invoke(@NonNull final String code, @NonNull String message) {
        cleanResponse();
        response.put("code", code);
        response.put("message", message);
        this.callback.invoke(response);
    }

    public void invoke(@NonNull final String code) {
        cleanResponse();
        response.put("code", code);
        this.callback.invoke(response);
    }

    public void invoke(@NonNull final String code, @NonNull JSONObject data) {
        cleanResponse();
        response.put("code", code);
        response.put("data", data);
        this.callback.invoke(response);
    }

    public void invoke(@NonNull final String code, @NonNull JSONArray data) {
        cleanResponse();
        response.put("code", code);
        response.put("data", data);
        this.callback.invoke(response);
    }
}