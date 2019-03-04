package com.example.demo.common.utils;

/**
 * @author monkey_lwy@163.com
 * @date 2019-02-28 17:43
 * @desc
 */

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConstantProperties implements InitializingBean {



    @Value("${oss.endpoint}")
    private String spring_file_endpoint;

    @Value("${oss.accessKey}")
    private String spring_file_keyid;

    @Value("${oss.secretKey}")
    private String spring_file_keysecret;

    @Value("${oss.bucketName}")
    private String spring_file_bucketname1;

    @Value("${oss.domain}")
    private String spring_file_filehost;

    public  static  String SPRING_FILE_ENDPOINT;
    public  static  String SPRING_FILE_ACCESS_KEY_ID;
    public  static  String SPRING_FILE_ACCESS_KEY_SECRET;
    public  static  String SPRING_FILE_BUCKET_NAME1;
    public  static  String SPRING_FILE_FILE_HOST;

    @Override
    public void afterPropertiesSet() throws Exception {
        SPRING_FILE_ENDPOINT = spring_file_endpoint;
        SPRING_FILE_ACCESS_KEY_ID = spring_file_keyid;
        SPRING_FILE_ACCESS_KEY_SECRET = spring_file_keysecret;
        SPRING_FILE_BUCKET_NAME1 = spring_file_bucketname1;
        SPRING_FILE_FILE_HOST = spring_file_filehost;
    }
}
