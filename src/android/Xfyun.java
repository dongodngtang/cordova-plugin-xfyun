package org.cordova.plugin.xfyun;

import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.iflytek.cloud.SpeechUtility;

/**
 * cordova科大讯飞插件 create by wilhan.tian
 */
public class Xfyun extends CordovaPlugin {
    private SpeechRecognizer mAsr;
    private String TAG = "cordova-plguin-xfyun";
    private CallbackContext mBuildGrammarCallbackContext;

    private CallbackContext mNoPerGrammarListeningCallbackContext;
    private JSONArray mNoPerGrammarArgs;

    private String[] permissions = { Manifest.permission.RECORD_AUDIO,
            // Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("init")) {
            this.init(args, callbackContext);
            return true;
        }
        if (action.equals("startListeningGrammar")) {
            this.startListeningGrammar(args, callbackContext);
            return true;
        }
        if (action.equals("stopListeningGrammar")) {
            this.stopListeningGrammar(args, callbackContext);
            return true;
        }
        if (action.equals("clear")) {
            this.cancelGrammar(args, callbackContext);
            return true;
        }

        return false;
    }

    // 初始化
    private void init(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String appid = args.getString(0);

        Context context = cordova.getActivity().getApplicationContext();
        SpeechUtility.createUtility(context, "appid=" + appid);
        callbackContext.success();
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.d(TAG, "初始化失败，错误码：" + code);
            }
        }
    };

    /// 监听命令
    private void startListeningGrammar(JSONArray args, CallbackContext callbackContext) throws JSONException {

        String language = args.getString(0);

        // 获取相关权限
        if (!hasPermisssion()) {
            this.cordova.requestPermissions(this, 0, permissions);
            mNoPerGrammarArgs = args;
            mNoPerGrammarListeningCallbackContext = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return;
        }
        Context context = cordova.getActivity().getApplicationContext();
        if (mAsr == null) {
            mAsr = SpeechRecognizer.createRecognizer(context, mInitListener);
        }
        if (language.equals("zh_cn")) {
            mAsr.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mAsr.setParameter(SpeechConstant.ACCENT, "mandarin");
        } else {
            mAsr.setParameter(SpeechConstant.LANGUAGE, language);
        }
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mAsr.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mAsr.setParameter(SpeechConstant.VAD_EOS, "1000");
        mAsr.setParameter(SpeechConstant.ASR_PTT, "0");

        Log.e(TAG, "language:" + language);// 设置语言

        int ret = mAsr.startListening(new CustomRecognizerListener(callbackContext));
        if (ret != ErrorCode.SUCCESS) {
            Log.d(TAG, "启动监听器失败, 错误码: " + ret);
            callbackContext.error(ret);
        } else {
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        }
    }

    // 停止命令监听
    private void stopListeningGrammar(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (mAsr == null) {
            Log.d(TAG, "mAsr为空，停止失败");
            callbackContext.error("当前无命令监听, 无法停止");
            return;
        } else {
            mAsr.stopListening();
            Log.d(TAG, "停止成功");
            callbackContext.success();
        }
    }

    // 取消命令
    private void cancelGrammar(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (mAsr == null) {
            callbackContext.error("当前无命令监听, 无法取消");
            return;
        } else {
            mAsr.cancel();
            mAsr = null;
            callbackContext.success();
        }
    }

    @Override
    public boolean hasPermisssion() {
        for (String p : permissions) {
            if (!this.cordova.hasPermission(p))
                return false;
        }
        return true;
    }

    @Override
    public void requestPermissions(int requestCode) {
        this.cordova.requestPermissions(this, requestCode, permissions);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        if (mNoPerGrammarListeningCallbackContext != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.e(TAG, "相关权限被拒绝!");
                    mNoPerGrammarListeningCallbackContext.error(-2);
                    return;
                }
            }
            this.startListeningGrammar(mNoPerGrammarArgs, mNoPerGrammarListeningCallbackContext);
        }
    }

    // 自定义监听器
    class CustomRecognizerListener implements RecognizerListener {
        private CallbackContext callbackContext;

        CustomRecognizerListener(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        // 音量变化
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "onVolumeChanged" + volume);
            // try{
            // JSONObject main = new JSONObject();
            // main.put("action", "onVolumeChanged");

            // JSONObject json = new JSONObject();
            // json.put("volume", volume);
            // json.put("data", data);
            // main.put("data", json);

            // PluginResult pResult = new PluginResult(PluginResult.Status.OK, main);
            // pResult.setKeepCallback(true);
            // callbackContext.sendPluginResult(pResult);
            // }catch(JSONException e){}
        }

        // 返回结果
        public void onResult(final RecognizerResult result, boolean isLast) {
            Log.d(TAG, "onResult");
            try {
                JSONObject main = new JSONObject();
                main.put("action", "onResult");

                JSONObject json = new JSONObject();
                json.put("result", result == null ? "" : result.getResultString());
                json.put("isLast", isLast);
                main.put("data", json);

                PluginResult pResult = new PluginResult(PluginResult.Status.OK, main);
                pResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pResult);
            } catch (JSONException e) {
            }
        }

        // 开始说话
        public void onBeginOfSpeech() {
            Log.d(TAG, "onBeginOfSpeech");
            try {
                JSONObject main = new JSONObject();
                main.put("action", "onBeginOfSpeech");

                PluginResult pResult = new PluginResult(PluginResult.Status.OK, main);
                pResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pResult);
            } catch (JSONException e) {
            }
        }

        // 结束说话
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
            try {
                JSONObject main = new JSONObject();
                main.put("action", "onEndOfSpeech");

                PluginResult pResult = new PluginResult(PluginResult.Status.OK, main);
                pResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pResult);
            } catch (JSONException e) {
            }
        }

        // 错误回调
        public void onError(SpeechError error) {
            Log.d(TAG, "发生错误，错误码:" + error.getErrorCode());
            callbackContext.error(error.getErrorCode());
        }

        // 件回调
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    }
}
