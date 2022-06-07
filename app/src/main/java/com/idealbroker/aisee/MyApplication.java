package com.idealbroker.aisee;


import android.app.Application;
import android.os.Build;

import com.kongzue.baseokhttp.util.BaseOkHttp;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.style.IOSStyle;
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
        DialogX.init(this);
        DialogX.globalStyle = new IOSStyle();
        BaseOkHttp.serviceUrl = "http://192.168.31.114:4000/";
        user = new User();


    }


}
