package com.idealbroker.aisee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;
import com.kongzue.baseokhttp.util.Parameter;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.v3.WaitDialog;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static MainActivity base;
    public static WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        base = this;

        webView = findViewById(R.id.webView);
        webView.setVerticalScrollBarEnabled(false);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webSettings.setSupportZoom(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getApplication().getCacheDir().getAbsolutePath());
        webSettings.setDatabaseEnabled(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " TooMuchAI/" + ToolUtils.getLocalVersion(getApplicationContext()));
        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void qqlogin() {
                MyApplication.mTencent.login(MainActivity.this, "all", QQLoginListener);
            }
            @JavascriptInterface
            public String get_loginState() {
                return MyApplication.user.getLoginState().toString();
            }

            @JavascriptInterface
            public String get_token() {
                return MyApplication.user.getToken();
            }

        }, "JS");
        webView.loadUrl("file:///android_asset/html/home/index.html");
        webView.setWebContentsDebuggingEnabled(true);


        XXPermissions.with(this)
                .permission(Permission.RECORD_AUDIO)
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            System.out.println("获取权限成功");
                        } else {
                            System.out.println("获取部分权限成功，但部分权限未正常授予");
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            System.out.println("被永久拒绝授权");
                            finish();
                        } else {
                            System.out.println("获取权限失败");
                        }
                    }
                });


    }

    public static void refreshLoginState() {
        MainActivity.base.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToolUtils.syncCookie(webView);
                webView.evaluateJavascript("javascript:app.loginState=eval('(' + JS.get_loginState() + ')');postchild('refreshLoginState')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {

                    }
                });
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Tencent.onActivityResultData(requestCode, resultCode, data, QQLoginListener);

    }

    IUiListener QQLoginListener = new IUiListener() {
        @Override
        public void onComplete(Object o) {
            try {
                WaitDialog.show(MainActivity.this, "请稍候...");
                JSONObject jsonObject = (JSONObject) o;
                Log.e("QQLOGIN", jsonObject.toString());
                String accessToken = jsonObject.getString("access_token");

                HttpRequest.GET(MainActivity.this, "/qqlogin",
                        new Parameter().add("accessToken", accessToken), new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    MyApplication.user.set("token", jsonObject.getString("token"));
                                    ToolUtils.showToastMessage(MainActivity.this, jsonObject.getString("description"), 2000);
                                    MyApplication.user.login();
                                    WaitDialog.dismiss();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    ToolUtils.showToastMessage(MainActivity.this, "请求失败！", 2000);
                                    WaitDialog.dismiss();
                                }

                            }
                        });


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(UiError uiError) {
            ToolUtils.showToastMessage(MainActivity.this, uiError.errorMessage, 1000);
        }

        @Override
        public void onCancel() {
            ToolUtils.showToastMessage(MainActivity.this, "取消", 1000);
        }

        @Override
        public void onWarning(int i) {
            ToolUtils.showToastMessage(MainActivity.this, "警告", 1000);

        }

    };


}