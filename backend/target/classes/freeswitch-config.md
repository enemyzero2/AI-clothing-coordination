# FreeSWITCH与Spring Boot集成配置

本文档介绍如何配置FreeSWITCH使用Spring Boot后端的mod_xml_curl服务。

## 1. 安装mod_xml_curl模块

确保FreeSWITCH已安装并启用mod_xml_curl模块：

```
# 在FreeSWITCH配置目录下编辑modules.conf.xml
vi /etc/freeswitch/autoload_configs/modules.conf.xml

# 添加或取消注释以下行
<load module="mod_xml_curl"/>
```

## 2. 配置mod_xml_curl

配置mod_xml_curl指向我们的Spring Boot后端：

```
# 编辑xml_curl.conf.xml
vi /etc/freeswitch/autoload_configs/xml_curl.conf.xml
```

配置内容示例：

```xml
<configuration name="xml_curl.conf" description="XML Curl Configuration">
  <bindings>
    <!-- 用户目录查询绑定 -->
    <binding name="directory">
      <param name="gateway-url" value="http://your-server-ip:8082/api/freeswitch/xml" bindings="directory"/>
      <param name="method" value="post"/>
      <param name="timeout" value="5"/>
    </binding>

    <!-- 拨号计划查询绑定 -->
    <binding name="dialplan">
      <param name="gateway-url" value="http://your-server-ip:8082/api/freeswitch/xml" bindings="dialplan"/>
      <param name="method" value="post"/>
      <param name="timeout" value="5"/>
    </binding>

    <!-- 配置查询绑定 -->
    <binding name="configuration">
      <param name="gateway-url" value="http://your-server-ip:8082/api/freeswitch/xml" bindings="configuration"/>
      <param name="method" value="post"/>
      <param name="timeout" value="5"/>
    </binding>
  </bindings>
</configuration>
```

注意：替换`your-server-ip`为你的Spring Boot服务器IP地址。

## 3. 重启FreeSWITCH

配置完成后，重启FreeSWITCH以应用更改：

```bash
systemctl restart freeswitch
# 或
service freeswitch restart
# 或
/etc/init.d/freeswitch restart
```

## 4. 验证配置

可以通过FreeSWITCH CLI检查mod_xml_curl是否正常工作：

```
fs_cli -x "xml_locate user username domain.com"
```

如果配置正确，FreeSWITCH将发送请求到我们的API并收到响应。

## 5. 调试

在调试过程中，可以通过以下方式查看FreeSWITCH日志：

```
tail -f /var/log/freeswitch/freeswitch.log
```

同时检查Spring Boot服务的日志以确认是否收到请求并返回了正确的XML响应。

## 6. SIP用户测试

登录APP后，可以使用SIP客户端（如Linphone、Zoiper等）测试注册：

1. 服务器：FreeSWITCH服务器地址
2. 用户名：APP中显示的SIP用户名
3. 密码：APP中显示的SIP密码
4. 域：APP中显示的SIP域名

如果一切配置正确，SIP客户端应该能够成功注册并进行通话。 