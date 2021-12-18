package com.huawei.bigdata.https.kerberos.demo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginContext;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;

public class HttpsKerberosTest {

    private static Logger log = LoggerFactory.getLogger(HttpsKerberosTest.class);

    public static void main(String[] args) throws Exception {

        if(args.length <3){
            System.out.println("输入有误，请依次输入 用户名、用户密码，GET请求的URL");
            System.exit(0);
        }

        String username = args[0];
        String password = args[1];
        // Oozie IP 取安装有Oozie的任一节点IP，端口固定21003,示例https://172.16.4.132:21003/oozie/v1/jobs
        // Yarn取主ResourceManager实例的IP，端口26001 示例 https://172-16-4-173:26001/ws/v1/cluster/apps
        String url = args[2];
        String type = "get";
        String json = "{}";

        String KRB5_CONF = HttpsKerberosTest.class.getClassLoader().getResource("krb5.conf").getPath();
        String JAAS_PATH = HttpsKerberosTest.class.getClassLoader().getResource("jaas.conf").getPath();

        System.setProperty("java.security.auth.login.config", JAAS_PATH);
        System.setProperty("java.security.krb5.conf", KRB5_CONF);

        HttpResponse response = trustAllHttpsCertificates(url, username, password, type, json);
        HttpEntity entity = response.getEntity();
        String entityString = EntityUtils.toString(entity);
        System.out.println(entityString);
    }


    /**
     * 设置 https 请求,参考 https://www.cnblogs.com/kiko2014551511/p/11609853.html
     *
     * @throws Exception
     */
    private static HttpResponse trustAllHttpsCertificates(final String url, String username, String pass, final String type,
                                                          final String jsonParam) throws Exception {
        // TestClient需要与jaas.conf文件中的第一行对应
        LoginContext lc = new LoginContext("TestClient", new KerberosCallBackHandler(username, pass));
        lc.login();
        Subject sb = lc.getSubject();
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(new AuthScope(null, -1, null), new Credentials() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getPassword() {
                return null;
            }
        });

        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build();

        TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        TrustManager tm = new HttpsKerberosTest.miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE);
        final HttpClient httpClient = HttpClients.custom().setDefaultAuthSchemeRegistry(authSchemeRegistry)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLSocketFactory(sslSocketFactory).build();
        HttpResponse response = Subject.doAs(sb, new PrivilegedExceptionAction<HttpResponse>() {
            @Override
            public HttpResponse run() throws Exception {
                if ("get".equals(type)) {
                    HttpUriRequest request = new HttpGet(url);
                    return httpClient.execute(request);
                } else if ("post".equals(type)) {
                    // 设置请求头和报文
                    HttpPost httpPost = new HttpPost();
                    httpPost.setHeader("Connection", "Keep-Alive");
                    // 设置报文和通讯格式
                    StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
                    stringEntity.setContentEncoding("UTF-8");
                    stringEntity.setContentType("application/json");
                    httpPost.setEntity(stringEntity);
                    log.info("请求{}接口的参数为{}", url, jsonParam);
                    //执行发送，获取相应结果
                    return httpClient.execute(httpPost);
                } else {
                    log.info("输入类型有误");
                    return null;
                }
            }
        });
        return response;
    }

    //设置 https 请求证书
    static class miTM implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
        }

        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
        }
    }
}

class KerberosCallBackHandler implements CallbackHandler {

    private final String user;
    private final String password;

    public KerberosCallBackHandler(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {

        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nc = (NameCallback) callback;
                nc.setName(user);
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callback;
                pc.setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callback, "Unknown Callback");
            }
        }
    }
}
