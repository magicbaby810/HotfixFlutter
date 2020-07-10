## 混栈开发之Android端Flutter热更新

#### 背景
[Flutter暂时放弃热更新的官方解释](https://github.com/flutter/flutter/issues/14330#issuecomment-485565194)

Google从性能和安全两方面考虑，暂时不会推出热更新，把这个功能交给用户和第三方去处理。结合公司不愿提供任何资源，只能走捷径的方式使用Tinker去实现Flutter的热更新。

#### 分析
Native项目可以接入Tinker进行热更新，而且有Bugly做为补丁版本控制台，来上传下发补丁，统计数量，不需要再去实现，省了不少事。

接入Flutter模块，修改Dart代码后，执行buildTinkerPatchRelease，生成patch\_signed\_7zip.apk补丁包，解开patch\_signed\_7zip.apk，里面也生成了Flutter模块的补丁so包。测试直接使用Tinker进行热更新，Dart代码的修改并未生效。

由于Flutter有自己的一套so加载流程，Tinker无法加载到Flutter的补丁so包。分析下Flutter的so加载流程，在FlutterLoader类里可以通过反射字段aotSharedLibraryName把它set进去，这样就可以实现加载补丁so文件，测试Dart代码的修改生效。

#### FlutterBoost
但是在集成FlutterBoost后，这样就行不通了。因为Flutter的初始化封装在FlutterBoost类里，FlutterBoost里又new了一个FlutterEngine引擎类，传入一个空字符数组的FlutterShellArgs。需要我们把反射方法放在里面的初始化方法后执行

```FlutterMain.startInitialization(mPlatform.getApplication());```

这就需要利用插桩，找到初始化方法，把反射方法插到初始化方法后面。测试生效。

#### 结论
整个修复过程，都是利用Flutter自身加载so文件去实现，所以不会出现兼容性和安全性的问题，而且也不会对系统性能有任何大的损耗。同时，Tinker开源，可以方便的查阅Tinker的源码。  

<br/>

> Flutter版本1.17.3，Dart版本2.8.4。Flutter低于1.12以下的请抓紧升级。  
> Gradle版本5.4.1，Gradle Plugin版本3.4.1。项目中Tinker版本不支持高版本的Gradle，请注意。  
> 纯Flutter项目也可以在android下配置Tinker，但是遇到[tinker id问题](https://github.com/Tencent/tinker/issues/1422)
 
## 快速接入你的项目

1. 根配置添加，repositories下

	```
	maven { url 'https://dl.bintray.com/magicbaby/maven' }
	```

   dependencies下

	```
	classpath 'com.sk.hannibal:hannibal:1.0.5.1'
	```

2. 在app gradle里配置

	```
	apply plugin: 'hannibal'	
	```
   dependencies下

	```
	implementation 'com.sk.flutterpatch:flutterpatch:0.0.3'
	```


<br/>

## Demo运行步骤            

<font color=#ff0000>第一次运行请先按步骤走下</font>


1. down下来后，先打开flutterhotfixmodule项目，open->HotFixFlutter->flutterhotfixmodule，再打开pubspec.yaml，点击Pub get，执行完成。

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
	implementation project(':flutter_boost')
	implementation 'com.sk.flutterpatch:flutterpatch:0.0.4'
   ```
   如果需要运行flutterpatch模块，在flutterpatch模块下gradle配置如下。

   ```
	implementation project(':flutter')
	implementation project(':flutter_boost')
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
<br/>

## 更新
#### FlutterPatch 0.0.4
- 优化FlutterPatch类，在hannibal中固定路径，防止出错
 
<br/>
#### Hannibal 1.0.5.1
- 不需再配置hannibal扩展项
- 不再区分是否集成FlutterBoost

#### Hannibal 1.0.3
- 移除insertClassFullName，改为依赖flutterpatch来实现，减少出错

#### Hannibal 1.0.2
- 修复Windows下扫描不到FlutterBoost类


<br/>
<br/>
<br/>

### 鸣谢
[带你不到80行代码搞定Flutter热更新](https://cloud.tencent.com/developer/article/1531498)


