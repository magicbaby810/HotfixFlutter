## 混栈开发之Android端Flutter热更新-Tinker篇（一）

### Sophix 也可支持Flutter热更新了！！！ 
Flutter里的Dart代码和资源文件都可修复

[混栈开发之Android端Flutter热更新-Sophix篇（二）](https://github.com/magicbaby810/HotfixFlutter/blob/master/README-Sophix.md)

#### 背景
[Flutter暂时放弃热更新的官方解释](https://github.com/flutter/flutter/issues/14330#issuecomment-485565194)

Google从性能和安全两方面考虑，暂时不会推出热更新，把这个功能交给用户和第三方去处理。结合公司不愿提供任何资源，只能走捷径的方式使用Tinker去实现Flutter的热更新。

#### 分析
Native项目可以接入Tinker进行热更新，而且有Bugly做为补丁版本控制台，来上传下发补丁，统计数量，不需要再去实现，省了不少事。

接入Flutter模块，修改Dart代码后，执行buildTinkerPatchRelease，生成patch\_signed\_7zip.apk补丁包，解开patch\_signed\_7zip.apk，里面也生成了Flutter模块的补丁so包。测试直接使用Tinker进行热更新，Dart代码的修改并未生效。

由于Flutter有自己的一套so加载流程，Tinker无法加载到Flutter的补丁so包。分析下Flutter的so加载流程，在FlutterLoader类里可以通过反射字段aotSharedLibraryName把它set进去，这样就可以实现加载补丁so文件，测试Dart代码的修改生效。Flutter新版本增加了FlutterApplicationInfo，但是不影响反射的使用，反射FlutterApplicationInfo里的aotSharedLibraryName，把路径set进去，再塞给FlutterLoader（感谢@heroghost的提醒）。


#### FlutterBoost (flutter1.22.5版本有问题，暂时不集成)
但是在集成FlutterBoost后，这样就行不通了。因为Flutter的初始化封装在FlutterBoost类里，FlutterBoost里又new了一个FlutterEngine引擎类，传入一个空字符数组的FlutterShellArgs。需要我们把反射方法放在里面的初始化方法后执行

```FlutterMain.startInitialization(mPlatform.getApplication());```

这就需要利用插桩，找到初始化方法，把反射方法插到初始化方法后面。测试生效。

#### 结论
整个修复过程，都是利用Flutter自身加载so文件去实现，所以不会出现兼容性和安全性的问题，而且也不会对系统性能有任何大的损耗。同时，Tinker开源，可以方便的查阅Tinker的源码。  

<br/>


> Flutter版本1.22.5，Dart版本2.10.4。Flutter低版本抓紧升级，不要用命令去升级Flutter，最好直接去官网下载zip解压，能省去好多问题。  
> Gradle版本5.4.1，Gradle Plugin版本3.4.1。

 
## 快速接入
> 无需关心是否集成FlutterBoost   

> 无需关心FlutterPatch类

> 无需在Flutter初始化后编写 `FlutterPatch.flutterPatchInit(this);`  

> 新增Sophix支持Flutter热更新 

1. 根配置添加，repositories下

	```
	maven { url 'https://dl.bintray.com/magicbaby/maven' }
	```

   dependencies下

	```
	classpath 'com.sk.hannibal:hannibal:1.0.7'
	```

2. 在app gradle里配置

	```
	apply plugin: 'hannibal'	
	```
   dependencies下

	```
	implementation 'com.sk.flutterpatch:flutterpatch:0.0.7'
	```


<br/>

## Demo运行步骤            

<font color=#ff0000>第一次运行请先按步骤走下</font>


1. down下来后，先打开flutterhotfixmodule项目，open->HotFixFlutter->flutterhotfixmodule，别管报错信息，直接打开pubspec.yaml，点击Pub get，执行完成。

2. 打开HotFixFlutter，切换到Project下，打开根目录的settings.gradle，把下面的配置copy进去。  
	注意一定要填对路径，这个是我demo的路径，如果你用自己的项目跑的话，就需要把你的路径给放进来，比如'/xxx/.android/include_flutter.groovy'

	> Native项目和Flutter项目在同一个目录下，如下配置
	
	
	```
	setBinding(new Binding([gradle: this]))
	evaluate(new File(settingsDir.parentFile, '/HotFixFlutter/flutterhotfixmodule/.android/include_flutter.groovy'))
	include ':flutterhotfixmodule'
	```
	> Native项目和Flutter项目不在同一个目录下，如下配置
		
	```
	setBinding(new Binding([gradle: this]))
	evaluate(new File(settingsDir.parentFile, flutterhotfixmodule/.android/include_flutter.groovy'))
	include ':flutterhotfixmodule'
	project(':flutterhotfixmodule').projectDir = new File('../flutterhotfixmodule')
	```
	点击Sync Now，执行完成，会看到项目结构变成田格样式
	
	![image](https://github.com/magicbaby810/HotfixFlutter/blob/master/screenshot/QQ20200624-180051@2x.png)
	
	
3. 在app的gradle里，配置下面flutter、flutterboost，以及flutterpatch的依赖，再次Sync Now。

	```
	implementation project(':flutter')
	implementation 'com.sk.flutterpatch:flutterpatch:0.0.7'
   ```
   如果要测试flutterpatch，在flutterpatch模块的gradle里配置如下

   ```
	compileOnly project(':flutter')
   ```
<br/>


#### Tinker操作
> 如果是老手，已接过Tinker，无需再看下面步骤。新手接入，可以跟着我这个步骤走下，腾讯的官方文档乱七八糟的

4.  把bugly id复制到bugly初始化里面

	```
	Bugly.init(this, "你的bugly id", true);
	```
	运行gradle下面的assembleRelease任务。如果有error，请先clean project再试。


	![image](https://github.com/magicbaby810/HotfixFlutter/blob/master/screenshot/QQ20200624-183519@2x.png)

	执行完成，安装build->bakApk->带有日期文件夹->app-release.apk。

5. 去flutterhotfixmodule项目下修改dart代码，以及添加加载图片资源。修改完后回到HotfixFlutter项目下，把build->bakApk下生成目录上的安装日期抄写到tinker-support.gradle里的baseApkDir里。执行

	![image](https://github.com/magicbaby810/HotfixFlutter/blob/master/screenshot/QQ20200624-184708@2x.png)

6. 进入	bugly官网，打开热更新页面，点击发布新补丁，找到build->outputs->patch->patch_signed_7zip.apk，上传完成，点击全量设备（只限测试，别整个生产的bugly id进来啊），立即下发。稍微等待那么一小会，杀掉应用，再重新打开，会出现

	![image](https://github.com/magicbaby810/HotfixFlutter/blob/master/screenshot/QQ20200624-191212@2x.png)

	代表补丁已经打上去了，杀掉应用，再次打开进去flutter页面，修复成功！

	![image](https://github.com/magicbaby810/HotfixFlutter/blob/master/screenshot/WX20200629-103028.png)
<br/>
<br/>

#### Sophix操作  

这个不需要细说了吧，阿里的接入文档，还是很清晰的，配置完就ok了。  

- 比Tinker多个`SophixStubApplication`  
- 打补丁需要用到`SophixPatchTool`，一定要区分好新旧包
- 还可以利用`Sophix调试工具V3` app快速加载补丁
- 在demo里操作Sophix的时候，记得在`AppApplication`里注掉`attachBaseContext`这个方法


<br/>
<br/>


### 鸣谢
[带你不到80行代码搞定Flutter热更新](https://cloud.tencent.com/developer/article/1531498)


