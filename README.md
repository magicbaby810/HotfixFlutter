### 混栈开发之Android端Flutter热更新



#### 背景
[Flutter暂时放弃热更新的官方解释](https://github.com/flutter/flutter/issues/14330#issuecomment-485565194)

Google从性能和安全两方面考虑，暂时不会推出热更新，把这个功能交给用户和第三方去处理。结合公司不愿提供任何资源，只能走捷径的方式使用Tinker去实现Flutter的热更新。

#### 分析
Native项目可以接入Tinker进行热更新，而且有Bugly做为补丁版本控制台，来上传下发补丁，统计数量，不需要再去实现，省了不少事。

接入Flutter模块，修改Dart代码后，执行buildTinkerPatchRelease，生成patch\_signed\_7zip.apk补丁包，解开patch\_signed\_7zip.apk，里面也生成了Flutter模块的补丁so包。测试直接使用Tinker进行热更新，Dart代码的修改并未生效。

由于Flutter有自己的一套so加载流程，Tinker无法加载到Flutter的补丁so包。分析下Flutter的so加载流程，在FlutterLoader类里可以通过反射字段aotSharedLibraryName把它set进去，这样就可以实现加载补丁so文件，测试Dart代码的修改生效。

#### FlutterBoost
但是在集成FlutterBoost后，这样就行不通了。因为Flutter的初始化封装在FlutterBoost类里，FlutterBoost里又new了一个FlutterEngine引擎类，传入一个空字符数组的FlutterShellArgs。需要我们把反射方法放在里面的```FlutterMain.startInitialization(mPlatform.getApplication());```方法后执行，或者把补丁so包路径传进来，add到flutterShellArgs里。前一种方法需要反射，性能会有损耗，后一种虽然没什么损耗，只是需要把路径层层传进来，改动有点大。不过FlutterBoost做为模块依赖进来，改动一下也是可以的，测试生效。

#### 结论
整个修复过程，都是利用Flutter自身加载so文件去实现，所以不会出现兼容性和安全性的问题，而且也不会对系统性能有任何大的损耗。同时，Tinker开源，可以方便的查阅Tinker的源码。



#### demo运行
down下来后，先打开flutterhotfixmodule项目，open->HotFixFlutter->flutterhotfixmodule，打开pubspec.yaml，点击Pub get，执行完成。再打开HotFixFlutter，切换到Project下，等待Gradle Sync完成。再把```FlutterBoost.instance().init(platform, FlutterPatch.getLibPath(this));```的参数给传进```createEngine(String libPath)```方法内，在里面把
 
```
if (!TextUtils.isEmpty(libPath)) {
    flutterShellArgs.add("--aot-shared-library-name=" + libPath);
    flutterShellArgs.add("--aot-shared-library-name="
            + getApplicationInfo(mPlatform.getApplication().getApplicationContext()).nativeLibraryDir
            + File.separator + libPath);
}
```
### 更新
未考虑到多人协同开发，下载FlutterBoost都要手动把路径传进去，不太方便。所以改为插桩到
```FlutterMain.startInitialization(mPlatform.getApplication());```方法后实现，加入hannibal插桩库，根配置添加  

```
repositories下
maven { url 'https://dl.bintray.com/magicbaby/maven' }

```
```
dependencies下
classpath 'com.sk.hannibal:hannibal:1.0.1.2'
```
  
在app gradle里配置  
  
```  
apply plugin: 'hannibal'
	
hannibal {
 adjustFlutterBoost = true
 insertClassFullName = 'com.xxx.xxxxx.FlutterPatch'
}
```
记得把AppApplication的Bugly id改成你申请的id，或者你的项目可以照着这个配置来，有什么问题可以提issue

### 致谢
[带你不到80行代码搞定Flutter热更新](https://cloud.tencent.com/developer/article/1531498)
	






