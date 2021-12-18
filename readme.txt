基于用户密码的kerberos认证，访问HTTP的GET请求示例

1、将执行节点的 hosts 中添加对应FusionInsight集群的主机名IP映射信息；
2、将resources目录下的krb5.conf替换为要访问集群的krb5.conf
3、windows环境源码执行 com.huawei.bigdata.https.kerberos.demo.HttpsKerberosTest
4、linux环境执行参考：
（1）创建目录/opt/test、/opt/test/conf、/opt/test/lib
（2）将resources目录下的内容上传到/opt/test/conf
（3）将打包后的target目录下的lib目录内容上传到/opt/test/lib
（4）将maven package后的包https-kerberos-test-1.0-SNAPSHOT.jar上传到/opt/test/lib
（5）修改conf中的krb5.conf为对应的集群的krb5.conf文件
（5）在/opt/test目录中执行java -cp conf/:lib/* com.huawei.bigdata.https.kerberos.demo.HttpsKerberosTest developuser Huawei@123 https://172-16-4-173:26001/ws/v1/cluster/apps

参考：
访问 Yarn 地址 https://172-16-4-173:26001/ws/v1/cluster/apps/application_1610259978742_0067
访问 Oozie地址 https://172.16.4.132:21003/oozie/v1/jobs