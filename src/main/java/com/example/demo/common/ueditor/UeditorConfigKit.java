package com.example.demo.common.ueditor;


import com.example.demo.common.ueditor.manager.AbstractFileManager;
import com.example.demo.common.ueditor.manager.DefaultFileManager;

/**
 * Ueditor 配置工具类，非线程安全，请全局配置
 * @author L.cm
 * email: 596392912@qq.com
 * site:http://www.dreamlu.net
 * date: 2016年1月20日 下午10:57:22
 */
public class UeditorConfigKit {
	
	protected static AbstractFileManager fileManager = new DefaultFileManager();
	
	public static void setFileManager(AbstractFileManager fileManager) {
		UeditorConfigKit.fileManager = fileManager;
	}
	
	public static AbstractFileManager getFileManager() {
		return UeditorConfigKit.fileManager;
	}
	
}
