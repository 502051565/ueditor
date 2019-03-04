package com.example.demo.common.ueditor.upload;


import com.example.demo.common.ueditor.PathFormat;
import com.example.demo.common.ueditor.UeditorConfigKit;
import com.example.demo.common.ueditor.define.AppInfo;
import com.example.demo.common.ueditor.define.BaseState;
import com.example.demo.common.ueditor.define.FileType;
import com.example.demo.common.ueditor.define.State;
import com.example.demo.common.utils.AliOSSUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BinaryUploader {

	private static List<String> post = new ArrayList<String>();

	static {
		post.add(".mp4");
		post.add(".avi");
		post.add(".wmv");
	}

	public static final State save(HttpServletRequest request,
								   Map<String, Object> conf) {
		FileItemStream fileStream = null;
		boolean isAjaxUpload = request.getHeader("X_Requested_With") != null;

		if (!ServletFileUpload.isMultipartContent(request)) {
			return new BaseState(false, AppInfo.NOT_MULTIPART_CONTENT);
		}
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
//		if (isMultipart) {
//			return getMultipartState(request, conf, fileStream, isAjaxUpload);
//		} else {
			return getState(request, conf, fileStream, isAjaxUpload);
//		}
	}

	private static State getState(HttpServletRequest request, Map<String, Object> conf, FileItemStream fileStream, boolean isAjaxUpload) {
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

		if (isAjaxUpload) {
			upload.setHeaderEncoding("UTF-8");
		}

		InputStream is = null;
		try {

			/*if (commonsMultipartResolver.isMultipart(request)) {//有文件上传
				//将request变成多部分request
				MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
				//获取multiRequest 中所有的文件名
				Iterator<String> iter = multiRequest.getFileNames();
				while (iter.hasNext()) {
					MultipartFile imageFile = multiRequest.getFile(iter.next());//(String) iter.next()
					File f=File.createTempFile("temp",null);
					imageFile.transferTo(f);
					f.deleteOnExit();
					is= new FileInputStream(f);
				}
			}
*/




			FileItemIterator iterator = upload.getItemIterator(request);

			while (iterator.hasNext()) {
				fileStream = iterator.next();

				if (!fileStream.isFormField())
					break;
				fileStream = null;
			}

			if (fileStream == null) {
				return new BaseState(false, 7);
			}

			String savePath = (String) conf.get("savePath");
			String originFileName = fileStream.getName();
			String suffix = FileType.getSuffixByFilename(originFileName);

			originFileName = originFileName.substring(0, originFileName.length() - suffix.length());
			savePath = savePath + suffix;

			long maxSize = ((Long) conf.get("maxSize")).longValue();

			if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
			}

			savePath = PathFormat.parse(savePath, originFileName);

			String rootPath = (String) conf.get("rootPath");

			is = fileStream.openStream();
			State storageState = UeditorConfigKit.getFileManager().saveFile(is, rootPath, savePath, maxSize);

			//copy到共享目录，供nginx代理做图片服务器
//			if(Globals.getBooleanProperty("file.store")){
//				File floder = new File(Globals.getProperty("file.floder"));
//				if(!floder.exists()){
//					floder.mkdir();
//				}
//				File file = new File(rootPath+savePath);
//				FileUtils.copyFile(file, new File(Globals.getProperty("file.floder") + savePath));
//			}
			String filePath = rootPath + savePath;

			//七牛上传
			/*try {

				Response response = QiniuKit.simpleUpload(filePath, null);
				String result = response.bodyString();
				JSONObject jsonObject = JSON.parseObject(result);
				savePath = PropKit.use("qiniu.properties").get("space_url") + jsonObject.getString("key");
				// 如果是视频，截图

			} catch (QiniuException e) {
				return new BaseState(false, AppInfo.IO_ERROR);
			}*/

			//阿里云上传
			try {
				// 上传到OSS
				File file = new File(filePath);
				savePath = AliOSSUtil.upload(file);
				file.delete();
			} catch (Exception e){
				throw new Exception("上传文件失败，请检查配置信息", e);
			}


			if (storageState.isSuccess()) {
				storageState.putInfo("url", PathFormat.format(savePath));
				storageState.putInfo("type", suffix);
				storageState.putInfo("original", originFileName + suffix);
			}

			return storageState;
		} catch (IOException e) {
			return new BaseState(false, 6);
		} catch (Exception e) {
			return new BaseState(false, AppInfo.IO_ERROR);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	private static State getMultipartState(HttpServletRequest request, Map<String, Object> conf, FileItemStream fileStream, boolean isAjaxUpload) {
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

		if (isAjaxUpload) {
			upload.setHeaderEncoding("UTF-8");
		}

		InputStream is = null;
		try {
			String savePath = (String) conf.get("savePath");
			// 得到所有的表单域，它们目前都被当作FileItem
			List<FileItem> fileItems = upload.parseRequest(request);
			String id = "";
			String fileName = "";
			// 如果大于1说明是分片处理
			int chunks = 1;
			int chunk = 0;
			FileItem tempFileItem = null;

			for (FileItem fileItem : fileItems) {
				if (fileItem.getFieldName().equals("id")) {
					id = fileItem.getString();
				} else if (fileItem.getFieldName().equals("name")) {
					fileName = new String(fileItem.getString().getBytes(
							"ISO-8859-1"), "UTF-8");
				} else if (fileItem.getFieldName().equals("chunks")) {
					chunks = Integer.parseInt(fileItem.getString());
				} else if (fileItem.getFieldName().equals("chunk")) {
					chunk = Integer.parseInt(fileItem.getString());
				} else if (fileItem.getFieldName().equals("file")) {
					tempFileItem = fileItem;
				}
			}

			// 临时目录用来存放所有分片文件
			String tempFileDir = savePath
					+ File.separator + id;
			File parentFileDir = new File(tempFileDir);
			if (!parentFileDir.exists()) {
				parentFileDir.mkdirs();
			}
			// 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台(默认每片为5M)
			File tempPartFile = new File(parentFileDir, fileName + "_" + chunk
					+ ".part");
			FileUtils.copyInputStreamToFile(tempFileItem.getInputStream(),
					tempPartFile);

			// 是否全部上传完成
			// 所有分片都存在才说明整个文件上传完成
			boolean uploadDone = true;
			for (int i = 0; i < chunks; i++) {
				File partFile = new File(parentFileDir, fileName + "_" + i
						+ ".part G:\\ideawork\\daxing21chu\\src\\main\\webapp");
				if (!partFile.exists()) {
					uploadDone = false;
				}
			}
			// 所有分片文件都上传完成
			// 将所有分片文件合并到一个文件中
			if (uploadDone) {
				File destTempFile = new File(savePath, fileName);
				for (int i = 0; i < chunks; i++) {
					File partFile = new File(parentFileDir, fileName + "_"
							+ i + ".part");

					FileOutputStream destTempfos = new FileOutputStream(
							destTempFile, true);

					FileUtils.copyFile(partFile, destTempfos);

					destTempfos.close();
				}
				// 得到 destTempFile 就是最终的文件
				// 添加到文件系统或者存储中

				// 删除临时目录中的分片文件
				FileUtils.deleteDirectory(parentFileDir);
				// 删除临时文件
				destTempFile.delete();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

//            while (iterator.hasNext()) {
//                fileStream = iterator.next();
//
//                if (!fileStream.isFormField())
//                    break;
//                fileStream = null;
//            }
//
//            if (fileStream == null) {
//                return new BaseState(false, 7);
//            }
//
//            String savePath = (String) conf.get("savePath");
//            String originFileName = fileStream.getName();
//            String suffix = FileType.getSuffixByFilename(originFileName);
//
//            originFileName = originFileName.substring(0, originFileName.length() - suffix.length());
//            savePath = savePath + suffix;
//
//            long maxSize = ((Long) conf.get("maxSize")).longValue();
//
//            if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
//                return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
//            }
//
//            savePath = PathFormat.parse(savePath, originFileName);
//
//            String rootPath = (String) conf.get("rootPath");
//
//            is = fileStream.openStream();
//            State storageState = UeditorConfigKit.getFileManager().saveFile(is, rootPath, savePath, maxSize);
//
//            //copy到共享目录，供nginx代理做图片服务器
////			if(Globals.getBooleanProperty("file.store")){
////				File floder = new File(Globals.getProperty("file.floder"));
////				if(!floder.exists()){
////					floder.mkdir();
////				}
////				File file = new File(rootPath+savePath);
////				FileUtils.copyFile(file, new File(Globals.getProperty("file.floder") + savePath));
////			}
//            String filePath  = rootPath+savePath;
//            String imgUrl = null;
//            if (post.contains(suffix)) {
//                String str1 = savePath.substring(0, savePath.lastIndexOf(suffix));
//                String img = str1 + ".png";
//                //System.out.println(img);
//                String imgPath = PathKit.getWebRootPath() + img;
//                CutOutImgUtils.cutImg(PathKit.getWebRootPath() + savePath, imgPath, PropKit.get("ffmpeg"));
//                Response response1 = QiniuKit.simpleUpload(imgPath, null);
//                String result1 = response1.bodyString();
//                JSONObject jsonObject1 = JSON.parseObject(result1);
//                imgUrl = PropKit.use("qiniu.properties").get("space_url")+jsonObject1.getString("key");
//            }
//
//            //七牛上传
//            try {
//
//                Response response = QiniuKit.simpleUpload(filePath,null);
//                String result = response.bodyString();
//                JSONObject jsonObject = JSON.parseObject(result);
//                savePath = PropKit.use("qiniu.properties").get("space_url")+jsonObject.getString("key");
//                // 如果是视频，截图
//
//            } catch (QiniuException e) {
//                return new BaseState(false, AppInfo.IO_ERROR);
//            }
//
//            if (storageState.isSuccess()) {
//                if(imgUrl != null){
//                    storageState.putInfo("url", PathFormat.format(savePath+";"+imgUrl+";"+suffix));
//                }else {
//                    storageState.putInfo("url", PathFormat.format(savePath));
//                }
////				storageState.putInfo("url", PathFormat.format(savePath));
//                storageState.putInfo("type", suffix);
//                storageState.putInfo("original", originFileName + suffix);
//            }
//
//            return storageState;
//        } catch (FileUploadException e) {
//            return new BaseState(false, 6);
//        } catch (IOException e) {
//            return new BaseState(false, AppInfo.IO_ERROR);
//        } finally {
//            IOUtils.closeQuietly(is);
//        }


//	public void fileUpload(HttpServletRequest request,
//						   HttpServletResponse response) throws ServletException {
//		try {
//			String path = request.getParameter("path");
//			path = path != null ? java.net.URLDecoder.decode(path, "utf-8")
//					: "";
//			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
//
//			if (isMultipart) {
//				FileItemFactory factory = new DiskFileItemFactory();
//				ServletFileUpload upload = new ServletFileUpload(factory);
//
//				// 得到所有的表单域，它们目前都被当作FileItem
//				List<FileItem> fileItems = upload.parseRequest(request);
//
//				String id = "";
//				String fileName = "";
//				// 如果大于1说明是分片处理
//				int chunks = 1;
//				int chunk = 0;
//				FileItem tempFileItem = null;
//
//				for (FileItem fileItem : fileItems) {
//					if (fileItem.getFieldName().equals("id")) {
//						id = fileItem.getString();
//					} else if (fileItem.getFieldName().equals("name")) {
//						fileName = new String(fileItem.getString().getBytes(
//								"ISO-8859-1"), "UTF-8");
//					} else if (fileItem.getFieldName().equals("chunks")) {
//						chunks = NumberUtils.toInt(fileItem.getString());
//					} else if (fileItem.getFieldName().equals("chunk")) {
//						chunk = NumberUtils.toInt(fileItem.getString());
//					} else if (fileItem.getFieldName().equals("file")) {
//						tempFileItem = fileItem;
//					}
//				}
//
//				// 临时目录用来存放所有分片文件
//				String tempFileDir = getTempFilePath()
//						+ File.separator + id;
//				File parentFileDir = new File(tempFileDir);
//				if (!parentFileDir.exists()) {
//					parentFileDir.mkdirs();
//				}
//				// 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台(默认每片为5M)
//				File tempPartFile = new File(parentFileDir, fileName + "_" + chunk
//						+ ".part");
//				FileUtils.copyInputStreamToFile(tempFileItem.getInputStream(),
//						tempPartFile);
//
//				// 是否全部上传完成
//				// 所有分片都存在才说明整个文件上传完成
//				boolean uploadDone = true;
//				for (int i = 0; i < chunks; i++) {
//					File partFile = new File(parentFileDir, fileName + "_" + i
//							+ ".part");
//					if (!partFile.exists()) {
//						uploadDone = false;
//					}
//				}
//				// 所有分片文件都上传完成
//				// 将所有分片文件合并到一个文件中
//				if (uploadDone) {
//					File destTempFile = new File(getTempFilePath(), fileName);
//					for (int i = 0; i < chunks; i++) {
//						File partFile = new File(parentFileDir, fileName + "_"
//								+ i + ".part");
//
//						FileOutputStream destTempfos = new FileOutputStream(
//								destTempFile, true);
//
//						FileUtils.copyFile(partFile, destTempfos);
//
//						destTempfos.close();
//					}
//					// 得到 destTempFile 就是最终的文件
//					// 添加到文件系统或者存储中
//
//					// 删除临时目录中的分片文件
//					FileUtils.deleteDirectory(parentFileDir);
//					// 删除临时文件
//					destTempFile.delete();
//
//					ResponseUtil.responseSuccess(response, null);
//				} else {
//					// 临时文件创建失败
//					if (chunk == chunks -1) {
//						FileUtils.deleteDirectory(parentFileDir);
//						ResponseUtil.responseFail(response, "500", "内部错误");
//					}
//				}
//			}
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//			ResponseUtil.responseFail(response, "500", "内部错误");
//		}
//	}


	private static boolean validType(String type, String[] allowTypes) {
		List<String> list = Arrays.asList(allowTypes);

		return list.contains(type);
	}
}
