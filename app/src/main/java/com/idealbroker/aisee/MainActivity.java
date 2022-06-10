package com.idealbroker.aisee;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.ResponseListener;
import com.kongzue.baseokhttp.util.BaseOkHttp;
import com.kongzue.baseokhttp.util.Parameter;

import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.BaseDialog;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
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
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAppCachePath(getApplication().getCacheDir().getAbsolutePath());
        webSettings.setDatabaseEnabled(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " TooMuchAI/" + ToolUtils.getLocalVersion(getApplicationContext()));
        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void logout() {
                MyApplication.tts.speek("确定要退出登录吗?", true, true);
                MessageDialog.build()
                        .setTitle("确定要退出登录吗?")
                        .setMessage(null)
                        .setCancelButton("取消", new OnDialogButtonClickListener() {
                            @Override
                            public boolean onClick(BaseDialog baseDialog, View v) {
                                MyApplication.tts.speek("取消", true, true);
                                return false;
                            }
                        })
                        .setOkButton("确定", new OnDialogButtonClickListener() {
                            @Override
                            public boolean onClick(BaseDialog baseDialog, View v) {
                                MyApplication.tts.speek("退出登录成功", true, true);
                                MyApplication.user.logout();
                                return false;
                            }
                        })
                        .show();
            }

            @JavascriptInterface
            public void qqlogin() {
                MyApplication.tts.speek("开始QQ登录", true, true);
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

            @JavascriptInterface
            public void settings() {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }

            @JavascriptInterface
            public void toggle_micService() {
                Intent intent = new Intent(MainActivity.this, MicService.class);
                if (!MicService.isRunning()) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }

            @JavascriptInterface
            public boolean get_micServiceState() {
                return MicService.isRunning();
            }

            @JavascriptInterface
            public String get_httpBaseUrl() {
                return BaseOkHttp.serviceUrl;
            }

            @JavascriptInterface
            public void speek(String text, boolean preemptive, boolean flush_queue) {
                MyApplication.tts.speek(text, preemptive, flush_queue);
            }

            @JavascriptInterface
            public void camera(){
                Intent intent = new Intent(MainActivity.this,OCRActivity.class);
                startActivity(intent);
            }

        }, "JS");
        webView.loadUrl("file:///android_asset/html/index.html");
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
                webView.evaluateJavascript(ToolUtils.genVuexStoreActionStr("update_loginState"), new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                    }
                });
            }
        });
    }

    public static void refreshMicServiceState() {
        MainActivity.base.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(ToolUtils.genVuexStoreActionStr("update_micServiceState"), new ValueCallback<String>() {
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

                HttpRequest.GET(MainActivity.this, "/oauth/qq",
                        new Parameter().add("accessToken", accessToken), new ResponseListener() {
                            @Override
                            public void onResponse(String response, Exception error) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    ToolUtils.showToastMessage(MainActivity.this, jsonObject.getString("description"), 2000);
                                    if (jsonObject.getBoolean("success")) {
                                        MyApplication.user.set("token", jsonObject.getString("token"));
                                        MyApplication.user.login();
                                    }
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
            ToolUtils.showToastMessage(MainActivity.this, "用户取消登录", 1000);
        }

        @Override
        public void onWarning(int i) {
            ToolUtils.showToastMessage(MainActivity.this, "警告", 1000);

        }

    };

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }


    long exitTime;

    @Override
    public void onBackPressed() {

        if ((System.currentTimeMillis() - exitTime) > 2000) {
            ToolUtils.showToastMessage(MyApplication.context,"再按一次退出程序",2000);
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }


    }

}