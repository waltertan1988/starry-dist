# 一、HTTPS服务端
## 1. 生成密钥库(包含证书)
```shell
keytool -genkeypair -alias starry -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore D:/work/keystore.p12 -validity 3650 -storepass serverchangeit -keypass serverchangeit -dname "CN=localhost, OU=Dev, O=Demo, L=GZ, ST=GD, C=CN" -ext "SAN=dns:localhost,dns:starry.com"
```

## 2. 查看密钥库
```shell
keytool -list -v -keystore D:/work/keystore.p12 -storetype PKCS12 -storepass serverchangeit
```

## 3. 从密钥库导出证书
```shell
keytool -exportcert -rfc -alias starry -keystore D:/work/keystore.p12 -storetype PKCS12 -storepass serverchangeit -file D:/work/starry.crt
```

# 二、HTTPS客户端
## 1. 证书导入到客户端的信任库(信任库不存在会自动创建)
```shell
keytool -importcert -alias starry -file D:/work/starry.crt -keystore D:/work/truststore.p12 -storetype PKCS12 -storepass clientchangeit -noprompt
```

## 2. 查看信任库
```shell
keytool -list -v -keystore D:/work/truststore.p12 -storetype PKCS12 -storepass clientchangeit
```

# 三、代码示例：
## 1. SpringBoot配置服务端启用HTTPS
把密钥库`keystore.p12`添加到`src/main/resources`下，并在`application.yml`中配置https：
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: serverchangeit
    key-store-type: PKCS12
    key-alias: starry
```

## 2. SpringBoot配置客户端调用HTTPS
把密钥库`truststore.p12`添加到`src/main/resources`下

### 示例1. 发起https请求（正常发起）
```java
package com.walter.starry.ai.mcp.server;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

@Slf4j
@SpringBootTest(classes = McpServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class MyTest {
    @Test
    void requestNormal() throws Exception {
        // 加载客户端信任库
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try(InputStream is = this.getClass().getClassLoader().getResourceAsStream("truststore.p12")){
            if (is == null){
                throw new IllegalStateException("truststore.p12 not found");
            }
            trustStore.load(is, "clientchangeit".toCharArray()); //客户端信任库的密码
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        // 客户端的域名校验器
        HostnameVerifier verifier = (host, session) -> {
            log.info("HostnameVerifier is verifying host: {}", host);
            return "localhost".equalsIgnoreCase(host);
        };

        // 证书里的CN设置了只能用localhost访问，若用127.0.0.1访问则会报错
        // URL url = URI.create("https://127.0.0.1:8443/api/ping").toURL();
        URL url = URI.create("https://localhost:8443/api/ping").toURL();
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(verifier);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        try(InputStream in = connection.getInputStream()){
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(body);
        }
    }
}
```

### 示例2. 发起https请求（绕过证书校验和域名校验）
```java
package com.walter.starry.ai.mcp.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@SpringBootTest(classes = McpServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class MyTest {

    @Test
    void requestBypassCertAndHost() throws Exception {
        TrustManager[] trustAllManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllManagers, new SecureRandom());

        HostnameVerifier verifier = (host, session) -> true;

        URL url = URI.create("https://localhost:8443/api/ping").toURL();
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(verifier);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        try(InputStream in = connection.getInputStream()){
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(body);
        }
    }
}
```