package cn.unicorn369;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class HookEuicc extends XposedModule {

    private static final String TAG = "HookEUICC";
    private static final String TITLE = "eSIM激活码";

    private static String initActivationCode = "";
    private static Activity activity;

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        final String packageName = param.getPackageName();
        final ClassLoader classLoader = param.getDefaultClassLoader();

        try {
            HookInit(classLoader);
            // OMAPI bypass
            if (packageName.equals("com.android.se")) {
                HookAndroidSE(classLoader);
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "发生错误: " + packageName, t);
        }
    }

    private void HookInit(ClassLoader classLoader) throws Throwable {
        // Activity.onCreate(Bundle)
        Method activityOnCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
        hook(activityOnCreate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    activity = (Activity) chain.getThisObject();
                    return chain.proceed();
                });

        Class<?> packageManagerClass = Class.forName(
                "android.app.ApplicationPackageManager", false, classLoader);

        // 伪装支持 eSIM
        Method hasSystemFeature1 =
                packageManagerClass.getDeclaredMethod("hasSystemFeature", String.class);
        hook(hasSystemFeature1)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String feature = (String) chain.getArg(0);
                    if (PackageManager.FEATURE_TELEPHONY_EUICC.equals(feature)) {
                        return true;
                    }
                    return chain.proceed();
                });

        Method hasSystemFeature2 =
                packageManagerClass.getDeclaredMethod("hasSystemFeature", String.class, int.class);
        hook(hasSystemFeature2)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String feature = (String) chain.getArg(0);
                    if (PackageManager.FEATURE_TELEPHONY_EUICC.equals(feature)) {
                        return true;
                    }
                    return chain.proceed();
                });

        // EuiccManager.isEnabled() -> true
        Method isEnabled = EuiccManager.class.getDeclaredMethod("isEnabled");
        hook(isEnabled)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> true);

        // 获取 eSIM 激活码
        Method forActivationCode =
                DownloadableSubscription.class.getDeclaredMethod("forActivationCode", String.class);
        hook(forActivationCode)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String activationCode = (String) chain.getArg(0);
                    if (activationCode != null) {
                        shareCode(activationCode);
                    }
                    return chain.proceed();
                });

        // Hook LPA
        Method getEncodedActivationCode =
                DownloadableSubscription.class.getDeclaredMethod("getEncodedActivationCode");
        hook(getEncodedActivationCode)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof String) {
                        String activationCode = (String) result;
                        if (activationCode != null) {
                            shareCode(activationCode);
                        }
                    }
                    return result;
                });

        // 其他检测
        Method queryIntentServices =
                packageManagerClass.getDeclaredMethod(
                        "queryIntentServices", Intent.class, int.class);
        hook(queryIntentServices)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Intent intent = (Intent) chain.getArg(0);

                    if (intent != null
                            && "android.service.euicc.EuiccService".equals(intent.getAction())) {

                        Object result = chain.proceed();

                        if (result instanceof List && !((List<?>) result).isEmpty()) {
                            return result;
                        }

                        List<ResolveInfo> fakeList = new ArrayList<>();
                        fakeList.add(createFakeResolveInfo());
                        return fakeList;
                    }

                    return chain.proceed();
                });

        // TelephonyManager.getCardIdForDefaultEuicc() -> 0
        Method getCardIdForDefaultEuicc =
                TelephonyManager.class.getDeclaredMethod("getCardIdForDefaultEuicc");
        hook(getCardIdForDefaultEuicc)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> 0);
    }

    // OMAPI bypass
    private void HookAndroidSE(ClassLoader classLoader) throws Throwable {
        Class<?> enforcerClass = Class.forName(
                "com.android.se.security.AccessControlEnforcer", false, classLoader);

        Method readSecurityProfile =
                enforcerClass.getDeclaredMethod("readSecurityProfile");

        hook(readSecurityProfile)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object thisObject = chain.getThisObject();

                    setBooleanField(thisObject, "mUseArf", false);
                    setBooleanField(thisObject, "mUseAra", false);
                    setBooleanField(thisObject, "mFullAccess", true);

                    return null;
                });
    }

    private static void setBooleanField(Object object, String fieldName, boolean value)
            throws IllegalAccessException, NoSuchFieldException {
        Class<?> clazz = object.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.setBoolean(object, value);
                return;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    private void shareCode(String activationCode) {
        Context context = getCurrentApplicationContext();

        ClipboardManager clipboardManager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        // 复制到剪切板
        if (clipboardManager != null) {
            ClipData clipData = ClipData.newPlainText(TITLE, activationCode);
            clipboardManager.setPrimaryClip(clipData);
        }

        // 避免重复发送相同激活码
        if (!activationCode.equals(initActivationCode)) {
            initActivationCode = activationCode;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, activationCode);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent = Intent.createChooser(shareIntent, TITLE);

            try {
                context.startActivity(shareIntent);
            } catch (Exception e) {
                if (activity != null) {
                    activity.startActivity(shareIntent);
                } else {
                    log(Log.WARN, TAG, "无法启动分享", e);
                }
            }

            try {
                Toast.makeText(
                        context,
                        "已复制到剪切板\neSIM激活码：" + activationCode,
                        Toast.LENGTH_LONG
                ).show();
            } catch (Exception e) {
                if (activity != null) {
                    Toast.makeText(
                            activity,
                            "已复制到剪切板\neSIM激活码：" + activationCode,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    // 获取当前进程 Application
    private Context getCurrentApplicationContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication =
                    activityThread.getDeclaredMethod("currentApplication");
            currentApplication.setAccessible(true);

            Object application = currentApplication.invoke(null);
            if (application instanceof Context) {
                return ((Context) application).getApplicationContext();
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "无法获取Application", t);
        }

        if (activity != null) {
            return activity.getApplicationContext();
        }

        throw new IllegalStateException("当前Application不可用");
    }

    // 构造伪造的 ResolveInfo。
    private ResolveInfo createFakeResolveInfo() {
        ResolveInfo fakeInfo = new ResolveInfo();
        fakeInfo.serviceInfo = new ServiceInfo();
        fakeInfo.serviceInfo.packageName = "cn.unicorn369.HookEuicc";
        fakeInfo.serviceInfo.name = "HookEuiccService";
        fakeInfo.serviceInfo.permission = "android.permission.BIND_EUICC_SERVICE";
        return fakeInfo;
    }
}
