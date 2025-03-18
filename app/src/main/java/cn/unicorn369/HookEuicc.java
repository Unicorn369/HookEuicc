package cn.unicorn369;

import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class HookEuicc implements IXposedHookLoadPackage {
    private static final String TAG = "HookEUICC";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
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
     }

}
