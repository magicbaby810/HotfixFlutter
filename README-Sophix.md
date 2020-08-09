## 混栈开发之Android端Flutter热更新-Sophix篇（二）

(混栈开发之Android端Flutter热更新-Tinker篇（一）)[https://juejin.im/post/6844904195665952776]

## 分析
之前写了利用Tinker来进行Flutter的热更新，分析了下原理以及实现步骤。  
后来一想，做为同样热更新框架的Sophix难道不可以吗？
按照sophix热修复的接入文档一步步接入完成，实验了一把，原生的改动修复，Flutter的改动并未修复。  
解开sophix-patch.jar包，里面有Flutter的补丁so包。Sophix和Tinker一样，也把Flutter的差分包给打出来了。  
那就简单了，我们只要找到这个补丁so包位置，检测下是否有so包，有的话也按照反射来处理，这样就可以使Sophix也连带把Flutter给修复了。  
操作流程跟Tinker一样，但是有一个问题，如何准确找到补丁so的位置？

## 探秘
Sophix的所有秘密都隐藏在`SophixManager`的`initialize()`里，所以跟进去逛逛。  
`queryAndLoadNewPatch`这个方法是拉取补丁的

```
public void queryAndLoadNewPatch() {
        com.taobao.sophix.c.c var1 = new com.taobao.sophix.c.c(1);
        int var2 = com.taobao.sophix.b.b.a == -1 ? 0 : com.taobao.sophix.b.b.a;
        var1.d = var2;
        this.a((String)null, var1, this.c);
    }
```
进this.a方法看看

```
public void a(final String var1, final com.taobao.sophix.c.c var2, final PatchLoadStatusListener var3) {
        if (com.taobao.sophix.b.b.b == null) {
            throw new RuntimeException("app is null");
        } else if (!com.taobao.sophix.e.f.a(com.taobao.sophix.b.b.b)) {
            com.taobao.sophix.e.d.d("SophixManager", "queryLoadPatch", new Object[]{"not in main progress, skip"});
        } else {
            k.a(new Runnable() {
                public void run() {
                    String var1x = e.this.e.a(var1, var2, var3);
                    if (!TextUtils.isEmpty(var1x)) {
                        e.this.f.a(var1x, var3, var2);
                    }

                }
            });
        }
    }
```
里面起了个子线程，`String var1x = e.this.e.a(var1, var2, var3);`可以看到下载补丁成功的日志

```
String var7 = (new File(this.g)).getParentFile().getAbsolutePath();
String var8 = this.a(var6, var7);
com.taobao.sophix.e.d.b("NetworkManager", "query", new Object[]{"local server decrypt patch", var8});
boolean var9 = com.taobao.sophix.e.a.a(var8, this.g, var6.c, this.d);
if (var9 && (new File(this.g)).exists()) {
    com.taobao.sophix.e.d.c("NetworkManager", "query", new Object[]{"download success"});
    var3.onLoad(0, 9, "download success", var6.b);
    com.taobao.sophix.e.i.b(com.taobao.sophix.b.b.b, "hpatch_clear", false);
    File var10 = new File(this.g);
    var2.c = "100";
    var2.g = var10.length();
    var2.e = System.currentTimeMillis() - var4;
    this.a(var2);
    return (new File(this.g)).exists() ? this.g : null;
} else {
    throw new com.taobao.sophix.a.b(11, "server decrypt fail");
}
```
下载的肯定是我们前面打好的补丁包`sophix-patch.jar`，所以需要解压，然后把里面的so包找到，存放到文件夹里。进`e.this.f.a(var1x, var3, var2);`里看看

```
Attributes var25 = this.a(var5);
String var10 = var25.getValue("Modified-So");
var4 = new ZipFile(var5);
var11 = false;
if (!TextUtils.isEmpty(var10)) {
    var11 = true;
    long var12 = System.currentTimeMillis();
    com.taobao.sophix.e.d.a("PatchManager", "addPatch", new Object[]{"start unzip lib file"});
    File var14 = var7 ? this.e : this.c;
    com.taobao.sophix.b.c var15 = new com.taobao.sophix.b.c();
    var15.a(var10, var4, var14);
    com.taobao.sophix.e.d.a("PatchManager", "addPatch", new Object[]{"finish unzip lib file(ms)", System.currentTimeMillis() - var12});
}
```
这里显示的是解压和存放so文件过程，最终通过`var15.a(var10, var4, var14);`这个方法把so文件存放在本地。  
进这个方法搂一眼，通过阅读这个方法不难看出，`File var13 = new File(var3, var8);`里var8就是libapp.so，而var3就是我们要找的路径，根据传参往回找，传入的形参var3是外部实参var14。  
`boolean var7 = this.a() != null;`显示，`this.a()`判断的是是否有下载好的`sophix-patch.jar`，所以var7这时候是true。  
`File var14 = var7 ? this.e : this.c;`，所以this.e就是我们要的路径。  
而this.e又对应着方法a

```
public void a(String var1) {
        File var2 = new File(var1);
        this.a = com.taobao.sophix.e.i.a(com.taobao.sophix.b.b.b, "SP_SOPHIX_DIR_STATE", 0);
        boolean var3 = com.taobao.sophix.e.f.a(com.taobao.sophix.b.b.b);
        if (var3 && this.a(2)) {
            this.a = 3 & ~this.a;
            com.taobao.sophix.e.i.b(com.taobao.sophix.b.b.b, "SP_SOPHIX_DIR_STATE", this.a);
        }

        if (this.a(1)) {
            this.b = com.taobao.sophix.e.b.a(var2, "patch_");
            this.c = com.taobao.sophix.e.b.a(var2, "libs_");
            this.d = com.taobao.sophix.e.b.a(var2, "patch");
            this.e = com.taobao.sophix.e.b.a(var2, "libs");
        } else {
            this.b = com.taobao.sophix.e.b.a(var2, "patch");
            this.c = com.taobao.sophix.e.b.a(var2, "libs");
            this.d = com.taobao.sophix.e.b.a(var2, "patch_");
            this.e = com.taobao.sophix.e.b.a(var2, "libs_");
        }

        com.taobao.sophix.e.d.b("PatchManager", "initPatchDir", new Object[]{"patchDir", this.b.getName(), "nativeLibDir", this.c.getName()});
        com.taobao.sophix.b.b.d = this.b;
        com.taobao.sophix.b.b.e = this.c;
        if (var3) {
            com.taobao.sophix.e.b.a(this.d);
            com.taobao.sophix.e.b.a(this.e);
        }

    }
```
可以看到这里创建了不同的补丁路径，至于我们要找的Flutter补丁so包具体在libs还是libs_呢？懒得猜了，直看ˆ往这个方法里插桩干它  
狠狠的插入我的代码，把这个方法的形参var1打印出来，可以看到一个完整的手机存储路径
`/data/user/0/com.sk.hotfixflutter/files/sophix`  
然后在hook方法里遍历这个文件夹，最终，我们要找的补丁so包完整路径就是
`/data/user/0/com.sk.hotfixflutter/files/sophix/libs/libapp.so`

不对，阿里的代码太明显了，日志完全没隐藏啊  
我在Log控制台，输入`NativeLibManager`

```
22687-22961/? D/Sophix.NativeLibManager:  init primaryCpuAbis: [arm64-v8a]
22687-22961/? D/Sophix.NativeLibManager:  unZipLibFile libPath: {"app":["armeabi-v7a","arm64-v8a"]} libDir: /data/user/0/com.sk.hotfixflutter/files/sophix/libs
22687-22961/? V/Sophix.NativeLibManager:  getLibPatchMap libPatchMap: {app=arm64-v8a}
22687-22961/? V/Sophix.NativeLibManager:  unZipLibFile entryName: lib/arm64-v8a/libapp.so soName: libapp.so
```
卧槽，浪费我插桩时间

## 实现
现在相当于我有了Sophix生成的补丁so包，那剩下来的流程跟Tinker要做的基本一样了。
但是怕Sophix路径以后有变，所以还是要在上面的a方法里插桩拿Sophix定义的路径，再补上libs/libapp.so，就可以了。  

在插桩库里，需要提前知道当前项目使用了哪个框架做热修复。因为如果其中一个没有初始化，调用的方法就会崩溃。所以要在它们初始化的方法里插桩，区分当前项目使用了哪个框架做热修复，就拿那个框架的so包路径去执行反射方法。分别测试两个框架，生效。







