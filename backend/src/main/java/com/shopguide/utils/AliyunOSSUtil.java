package com.shopguide.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ServiceException;
import com.shopguide.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传工具类（仅 OSS，不做本地磁盘存储）
 *
 * 配置来源：
 *   application.yml 的 storage.oss.* → {@link StorageProperties} →
 *   {@link com.shopguide.config.StorageConfig} 通过有参构造函数注入到本类。
 *
 * 其中 access-key-id / access-key-secret 在 yml 里写成环境变量占位符
 * （${OSS_ACCESS_KEY_ID:} / ${OSS_ACCESS_KEY_SECRET:}），由本类解析后使用：
 *   1. 若配置值本身就是环境变量名（全大写+下划线，如 OSS_ACCESS_KEY_ID），则按该名字去读环境变量；
 *   2. 若配置值是空串，则直接按默认的环境变量名 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET 读取；
 *   3. 否则视为直接写入的真实密钥。
 * 这样无论 yml 写占位符还是写环境变量名，都能正确取到密钥。
 */
public class AliyunOSSUtil {

    private static final Logger log = LoggerFactory.getLogger(AliyunOSSUtil.class);

    /** AK 对应的环境变量名 */
    private static final String ENV_ACCESS_KEY_ID = "OSS_ACCESS_KEY_ID";
    /** SK 对应的环境变量名 */
    private static final String ENV_ACCESS_KEY_SECRET = "OSS_ACCESS_KEY_SECRET";

    /** 环境变量名特征：全大写字母/数字/下划线 */
    private static final String ENV_NAME_PATTERN = "^[A-Z][A-Z0-9_]*$";

    /** OSS 配置（endpoint / accessKeyId / accessKeySecret / bucketName） */
    private final StorageProperties.Oss oss;

    /** 实际使用的 AccessKey ID（可能来自环境变量） */
    private final String accessKeyId;

    /** 实际使用的 AccessKey Secret（可能来自环境变量） */
    private final String accessKeySecret;

    /** OSS 是否可用：endpoint / bucket 有值且 AK、SK 都能取到 */
    private final boolean ossEnabled;

    /**
     * 有参构造函数：由 StorageConfig#aliyunOSSUtil(StorageProperties) 调用，
     * 在这里把 yml 配置引入本类，并解析环境变量形式的密钥。
     */
    public AliyunOSSUtil(StorageProperties properties) {
        this.oss = properties.getOss();
        this.accessKeyId = resolveSecret(this.oss.getAccessKeyId(), ENV_ACCESS_KEY_ID);
        this.accessKeySecret = resolveSecret(this.oss.getAccessKeySecret(), ENV_ACCESS_KEY_SECRET);
        this.ossEnabled = StringUtils.hasText(this.oss.getEndpoint())
                && StringUtils.hasText(this.oss.getBucketName())
                && StringUtils.hasText(this.accessKeyId)
                && StringUtils.hasText(this.accessKeySecret);

        if (ossEnabled) {
            log.info("阿里云 OSS 已启用：endpoint={}，bucket={}，accessKeyId={}",
                    this.oss.getEndpoint(), this.oss.getBucketName(), mask(this.accessKeyId));
        } else {
            log.error("阿里云 OSS 配置不完整，图片上传功能不可用。请设置环境变量 {} 与 {}，"
                            + "并确认 yml 中 storage.oss.endpoint / bucket-name 已配置",
                    ENV_ACCESS_KEY_ID, ENV_ACCESS_KEY_SECRET);
        }
    }

    /**
     * 上传图片到阿里云 OSS，返回公网访问 URL
     *
     * @param file 前端上传的文件
     * @return https://{bucket}.{endpoint}/{objectName}
     * @throws IllegalStateException OSS 未配置或上传过程失败
     */
    public String upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (!ossEnabled) {
            throw new IllegalStateException("未配置阿里云 OSS，无法上传图片。"
                    + "请设置环境变量 " + ENV_ACCESS_KEY_ID + " / " + ENV_ACCESS_KEY_SECRET);
        }

        // 对象名：product/时间戳_uuid.ext，避免同名覆盖
        String objectName = "product/" + System.currentTimeMillis() + "_" + uuid()
                + resolveExtension(file.getOriginalFilename());

        OSS ossClient = new OSSClientBuilder()
                .build(oss.getEndpoint(), accessKeyId, accessKeySecret);
        try (InputStream is = file.getInputStream()) {
            ossClient.putObject(oss.getBucketName(), objectName, is);
        } catch (ServiceException e) {
            // OSS 业务异常（AK 错误、bucket 不存在、跨地域等），只透出关键信息
            log.error("上传到 OSS 失败：{} - {}", e.getErrorCode(), e.getErrorMessage());
            throw new IllegalStateException("上传到 OSS 失败：" + e.getErrorCode() + " - " + e.getErrorMessage());
        } catch (Exception e) {
            log.error("上传到 OSS 异常：{}", e.getMessage());
            throw new IllegalStateException("上传到 OSS 失败：" + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
        log.info("图片已上传到 OSS：{}", objectName);
        return buildOssUrl(objectName);
    }

    /**
     * 当前 OSS 是否可用（供 Controller / 健康检查判断）
     */
    public boolean isOssEnabled() {
        return ossEnabled;
    }

    // ------------------------------------------------------------------ 私有方法

    /**
     * 解析密钥：支持「真实密钥」「环境变量名」「空值取默认环境变量」三种写法
     */
    private String resolveSecret(String configured, String defaultEnvName) {
        // 1. 未配置或占位符未解析出值 → 按默认环境变量名读取
        if (!StringUtils.hasText(configured)) {
            return env(defaultEnvName);
        }
        // 2. 配置值本身就是环境变量名（如 OSS_ACCESS_KEY_ID）→ 按该名字读取
        if (configured.matches(ENV_NAME_PATTERN)) {
            String value = env(configured);
            if (!StringUtils.hasText(value)) {
                // 变量名存在但环境变量没设置：视为未配置，避免把变量名当成密钥使用
                log.warn("配置项 {} 看起来是环境变量名，但系统未设置该环境变量", configured);
                return null;
            }
            return value;
        }
        // 3. 直接写入的真实密钥
        return configured;
    }

    private String env(String name) {
        String value = System.getenv(name);
        if (!StringUtils.hasText(value)) {
            value = System.getProperty(name);   // 兼容 -DOSS_ACCESS_KEY_ID=xxx 的启动方式
        }
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 拼装 OSS 公网访问地址（虚拟主机风格：https://{bucket}.{endpoint}/{object}）
     */
    private String buildOssUrl(String objectName) {
        String endpoint = oss.getEndpoint().trim();
        String scheme = endpoint.startsWith("http://") ? "http://" : "https://";
        String host = endpoint.replaceFirst("^https?://", "").replaceAll("/+$", "");
        return scheme + oss.getBucketName() + "." + host + "/" + objectName;
    }

    /**
     * 取文件扩展名（含点），无扩展名返回空串
     */
    private String resolveExtension(String originalName) {
        if (originalName != null && originalName.lastIndexOf(".") > 0) {
            return originalName.substring(originalName.lastIndexOf("."));
        }
        return "";
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 日志脱敏：只显示前 4 位和后 2 位
     */
    private String mask(String secret) {
        if (secret == null || secret.length() <= 6) {
            return "******";
        }
        return secret.substring(0, 4) + "******" + secret.substring(secret.length() - 2);
    }
}
