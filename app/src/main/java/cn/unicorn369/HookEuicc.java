package cn.unicorn369;

import android.app.Application;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;

import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEuicc implements IXposedHookLoadPackage {
    private static final String TAG = "HookEUICC";

    private static Application application;
    private static Context context;
    private static ClipboardManager clipboardManager;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedHelpers.findAndHookMethod(
            Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    application = (Application) param.thisObject;
                    context = application.getApplicationContext();
                    clipboardManager = (ClipboardManager) application.getSystemService(Context.CLIPBOARD_SERVICE);
                }
            }
        );

        //伪装支持eSIM
        XposedHelpers.findAndHookMethod(
            EuiccManager.class,
            "isEnabled",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            }
        );

        //获取eSIM激活码
        XposedHelpers.findAndHookMethod(
            DownloadableSubscription.class,
            "forActivationCode",
            String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String activationCode = (String) param.args[0];
                    //复制到剪切板
                    if (activationCode != null) {
                        ClipData clipdata = ClipData.newPlainText("eSIM激活码", activationCode);
                        clipboardManager.setPrimaryClip(clipdata);
                        Toast.makeText(context, "已复制到剪切板\neSIM激活码：" + activationCode, Toast.LENGTH_LONG).show();
                        shareCode(activationCode);
                    }
                }
            }
        );

    }

    //打开分享 (用于查看)
    public void shareCode(String activationCode) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, activationCode);
        Intent chooserIntent = Intent.createChooser(shareIntent, "eSIM激活码");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
