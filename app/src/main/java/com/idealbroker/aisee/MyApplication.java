package com.idealbroker.aisee;

import static com.kongzue.dialog.util.DialogSettings.STYLE.STYLE_IOS;

import android.app.Application;
import android.os.Build;

import com.kongzue.baseokhttp.util.BaseOkHttp;
import com.kongzue.dialog.util.DialogSettings;
import com.tencent.tauth.Tencent;


public class MyApplication extends Application {
    public static MyApplication context;
    public static Tencent mTencent;
    public static User user;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Tencent.setIsPermissionGranted(true, Build.MODEL);
        mTencent=Tencent.createInstance("102007045", this,"com.tencent.login.fileprovider");
        DialogSettings.style = STYLE_IOS;
        BaseOkHttp.serviceUrl = "https://aisee.idealbroker.cn/api/";
        user = new User();

    }


}
