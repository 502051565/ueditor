package com.example.demo.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author monkey_lwy@163.com
 * @date 2019-02-28 15:51
 * @desc
 */
public class AliOSSUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliOSSUtil.class);


    public static String upload(File file){
        //logger.info("=========>OSS文件上传开始："+file.getName());
        String endpoint=ConstantProperties.SPRING_FILE_ENDPOINT;
        String accessKeyId=ConstantProperties.SPRING_FILE_ACCESS_KEY_ID;
        String accessKeySecret=ConstantProperties.SPRING_FILE_ACCESS_KEY_SECRET;
        String bucketName=ConstantProperties.SPRING_FILE_BUCKET_NAME1;
        String fileHost=ConstantProperties.SPRING_FILE_FILE_HOST;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = format.format(new Date());

        if(null == file){
            return null;
        }

        OSSClient ossClient = new OSSClient(endpoint,accessKeyId,accessKeySecret);
        try {
            //容器不存在，就创建
            if(! ossClient.doesBucketExist(bucketName)){
                ossClient.createBucket(bucketName);
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
                createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
                ossClient.createBucket(createBucketRequest);
            }
            //创建文件路径
            String fileUrl = dateStr + "/" + UUID.randomUUID().toString().replace("-","")+"-"+file.getName();
            //上传文件
            PutObjectResult result = ossClient.putObject(new PutObjectRequest(bucketName, fileUrl, file));
            //设置权限 这里是公开读
            ossClient.setBucketAcl(bucketName,CannedAccessControlList.PublicRead);
            if(null != result){
                LOGGER.info("==========>OSS文件上传成功,OSS地址："+fileUrl);
                return fileHost+"/"+fileUrl;
            }
        }catch (OSSException oe){
            LOGGER.error(oe.getMessage());
        }catch (ClientException ce){
            LOGGER.error(ce.getMessage());
        }finally {
            //关闭
            ossClient.shutdown();
        }
        return null;
    }
}
