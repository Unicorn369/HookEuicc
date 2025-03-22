package cn.unicorn369;

import android.app.Activity;
import android.app.AndroidAppHelper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;

import android.widget.Toast;

//import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEuicc implements IXposedHookLoadPackage {
    private static final String TAG = "HookEUICC";
    private static final String Title = "eSIM激活码";

    private static String initActivationCode = "";
    private static Activity activity;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedHelpers.findAndHookMethod(
            Activity.class.getName(), lpparam.classLoader,
            "onCreate", "android.os.Bundle",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    activity = (Activity) param.thisObject;
                }
            }
        );

        //android.hardware.telephony.euicc
        Class<?> packageManagerClass = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(
            packageManagerClass, "hasSystemFeature", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String feature = (String) param.args[0];
                    if (feature.equals(PackageManager.FEATURE_TELEPHONY_EUICC)) {
                        param.setResult(true);
                    }
                }
            }
        );
        XposedHelpers.findAndHookMethod(
            packageManagerClass, "hasSystemFeature", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String feature = (String) param.args[0];
                    if (feature.equals(PackageManager.FEATURE_TELEPHONY_EUICC)) {
                        param.setResult(true);
                    }
                }
            }
        );

        //伪装支持eSIM
        XposedHelpers.findAndHookMethod(
            EuiccManager.class, "isEnabled",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            }
        );

        //获取eSIM激活码
        XposedHelpers.findAndHookMethod(
            DownloadableSubscription.class, "forActivationCode", String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String activationCode = (String) param.args[0];
                    if (activationCode != null) {
                        shareCode(activationCode);
                    }
                }
            }
        );

        //Hook LPA
        XposedHelpers.findAndHookMethod(
            DownloadableSubscription.class, "getEncodedActivationCode",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String activationCode = (String) param.getResult();
                    if (activationCode != null) {
                        shareCode(activationCode);
                    }
                }
            }
        );
    }

    private void shareCode(String activationCode) {
        Context context = AndroidAppHelper.currentApplication();
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        //复制到剪切板
        if (clipboardManager != null) {
            ClipData clipdata = ClipData.newPlainText(Title, activationCode);
            clipboardManager.setPrimaryClip(clipdata);
        }
        //发送激活码
        if (initActivationCode != activationCode) {
            initActivationCode = activationCode;
            new Share.Builder(activity).setText(activationCode).setTitle(Title).build().toShare();
            Toast.makeText(context, "已复制到剪切板\neSIM激活码：" + activationCode, Toast.LENGTH_LONG).show();
        }
    }
}
