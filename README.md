# HookEuicc
用于伪装应用支持eSIM功能，并获取eSIM激活码

在某些不直接提供eSIM激活码的应用，将复制激活码到剪切板

## 先决条件
root设备已安装 [LSPosed](https://github.com/LSPosed/LSPosed/releases)

无root设备安装 [LSPatch](https://github.com/JingMatrix/LSPatch/releases)


## 食用方法
通常只需勾选你需要伪装的应用即可享受薅羊毛的乐趣

对于某些应用，如`com.dentwireless.dentapp`、`com.giffgaffmobile.controller`在点击安装时将会复制eSIM激活码到剪切板

如果某些应用无法获取eSIM激活码，或者检测Hook，但是你的系统有安装LPA应用，如`com.miui.euicc`，那你可尝试勾选LPA应用来获取eSIM激活码

## 已知问题
通常在某些不直接提供eSIM激活码的应用上，获取到eSIM激活码后，会先复制到剪切板，然后发送消息并打开分享界面(用于查看)，但某些应用可能无法发送消息或打开分享界面。如果你没看到任何消息，可先查看剪切板是否存在eSIM激活码。始终没有eSIM激活码的话请在这里 -->> [反馈问题](https://github.com/Unicorn369/HookEuicc/issues) <<--
