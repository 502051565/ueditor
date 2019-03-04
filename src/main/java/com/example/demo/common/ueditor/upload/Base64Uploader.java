package com.example.demo.common.ueditor.upload;

import com.alibaba.fastjson.util.Base64;
import com.example.demo.common.ueditor.PathFormat;
import com.example.demo.common.ueditor.UeditorConfigKit;
import com.example.demo.common.ueditor.define.AppInfo;
import com.example.demo.common.ueditor.define.BaseState;
import com.example.demo.common.ueditor.define.FileType;
import com.example.demo.common.ueditor.define.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Base64Uploader {
	private static List<String> post = new ArrayList<String>();
	static {
		post.add(".mp4");
		post.add(".avi");
		post.add(".wmv");
	}

	public static State save(String content, Map<String, Object> conf) {
		byte[] data = decode(content);

		long maxSize = ((Long) conf.get("maxSize")).longValue();

		if (!validSize(data, maxSize)) {
			return new BaseState(false, AppInfo.MAX_SIZE);
		}

		String suffix = FileType.getSuffix("JPG");

		String savePath = PathFormat.parse((String) conf.get("savePath"),
				(String) conf.get("filename"));

		savePath = savePath + suffix;
		String rootPath = (String) conf.get("rootPath");

		State storageState = UeditorConfigKit.getFileManager().saveFile(data, rootPath, savePath);

		//copy到共享目录，供nginx代理做图片服务器
//		if(Globals.getBooleanProperty("file.store")){
//			File floder = new File(Globals.getProperty("file.floder"));
//			if(!floder.exists()){
//				floder.mkdir();
//			}
//			File file = new File(rootPath+savePath);
//			try {
//
//				FileUtils.copyFile(file, new File(Globals.getProperty("file.floder") + savePath));
//			} catch (IOException e) {
//				e.printStackTrace();
//				return new BaseState(false, AppInfo.IO_ERROR);
//			}
//		}

		String filePath  = rootPath+savePath;

		//七牛上传
		/*try {
			Response response = QiniuKit.simpleUpload(filePath, null);
			String result = response.bodyString();
			JSONObject jsonObject = JSON.parseObject(result);
			savePath = PropKit.use("qiniu.properties").get("space_url")+jsonObject.getString("key");

		} catch (QiniuException e) {
			return new BaseState(false, AppInfo.IO_ERROR);
		}*/

		if (storageState.isSuccess()) {
			storageState.putInfo("url", PathFormat.format(savePath));
			storageState.putInfo("type", suffix);
			storageState.putInfo("original", "");
		}

		return storageState;
	}

	private static byte[] decode(String content) {
		return Base64.decodeFast(content);
	}

	private static boolean validSize(byte[] data, long length) {
		return data.length <= length;
	}

}