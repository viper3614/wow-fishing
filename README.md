魔兽世界怀旧服巫妖王之怒版本的钓鱼脚本
=


使用Java语言实现，没有窗口界面，项目打包打包环境要求JDK17以上，打包完成后执行`java -jar wow-fishing-1.0-SNAPSHOT.jar` 启动项目


实现原理：
==
    1.通过opencv图片识别找到甩竿后鱼漂位置
    2.通过系统麦克风判断是否钓到鱼

注意：
==
		1.截图文件保存路径默认为E:\wowTempImg\ 
		2.每个钓鱼点的环境不同，影响找鱼漂的准确率，每次开始钓鱼时先截图鱼漂的图片，保存到resource目录，覆盖floatstart3.png,见下图
![](https://github.com/user-attachments/assets/eccb840d-a989-4a47-952a-321daf2e0680)

		3.判断是否钓到鱼，需要在游戏中设置音效，见下图
![](https://github.com/user-attachments/assets/0b2cd150-3c2f-4dba-9a35-34d2987ad173)
![](https://github.com/user-attachments/assets/e7fb6e02-f1ee-4d6b-9064-238b6893da8f)

		4.声音输入设备必须正常，否则通过声音判断是否钓到鱼无法工作，同时需要根据个人电脑情况选择麦克风输入设备
		5.保持环境安静，否则通过声音判断是否钓到鱼无法工作
		6.判断是否钓到鱼的图片识别有bug，不再修复了。原因有3
		1.图片识别需要在抛竿后大量截图，逐个和样本图片进行对比，性能慢
		2.OpenCV的图片对比存在误判
		3.声音识别比图片识别效率高，性能快
 
