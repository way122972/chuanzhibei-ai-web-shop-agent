package com.shopguide.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性类
 *
 * 作用：把 application.yml 中 storage.oss.* 的配置项读取成 Java 对象，
 *      再由 {@link StorageConfig} 通过有参构造函数注入给 AliyunOSSUtil。
 *
 * 对应 yml：
 * storage:
 *   oss:
 *     endpoint: oss-cn-beijing.aliyuncs.com
 *     access-key-id: ${OSS_ACCESS_KEY_ID:}
 *     access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
 *     bucket-name: projectww
 *
 * 其中 AK/SK 由环境变量 OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET 提供，
 * 未设置时为空，由 AliyunOSSUtil 判定为「未配置」，上传接口直接返回失败提示。
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 阿里云 OSS 配置 */
    private Oss oss = new Oss();

    /**
     * 阿里云 OSS 配置项
     * 注意：yml 中写成 access-key-id 这类中划线形式，Spring 会自动绑定到 accessKeyId 驼峰字段
     */
    @Data
    public static class Oss {
        /** 地域节点，例如 oss-cn-beijing.aliyuncs.com（可带 https:// 前缀） */
        private String endpoint;
        /** 访问密钥 ID */
        private String accessKeyId;
        /** 访问密钥 Secret */
        private String accessKeySecret;
        /** Bucket 名称 */
        private String bucketName;
    }
}
