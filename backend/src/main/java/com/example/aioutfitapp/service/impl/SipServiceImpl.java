package com.example.aioutfitapp.service.impl;

import com.example.aioutfitapp.model.SipUser;
import com.example.aioutfitapp.model.User;
import com.example.aioutfitapp.repository.SipUserRepository;
import com.example.aioutfitapp.repository.UserRepository;
import com.example.aioutfitapp.service.SipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * SIP服务实现类
 */
@Service
@Slf4j
public class SipServiceImpl implements SipService {

    /**
     * SIP用户仓库
     */
    @Autowired
    private SipUserRepository sipUserRepository;
    
    /**
     * 用户仓库
     */
    @Autowired
    private UserRepository userRepository;
    
    /**
     * SIP域名
     */
    @Value("${sip.domain:localhost}")
    private String sipDomain;
    
    /**
     * SIP密码长度
     */
    private static final int SIP_PASSWORD_LENGTH = 12;
    
    /**
     * 为用户创建SIP账户
     * 
     * @param userId 用户ID
     * @return SIP用户
     */
    @Override
    @Transactional
    public SipUser createSipAccount(String userId) {
        // 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        // 检查是否已经有SIP账户
        List<SipUser> existingSipUsers = sipUserRepository.findByUserId(userId);
        if (!existingSipUsers.isEmpty()) {
            // 已存在SIP账户，返回第一个
            return existingSipUsers.get(0);
        }
        
        // 生成SIP用户名和密码
        String sipUsername = generateSipUsername(user.getUsername());
        String sipPassword = generateRandomPassword(SIP_PASSWORD_LENGTH);
        
        // 创建SIP用户
        SipUser sipUser = SipUser.builder()
                .userId(userId)
                .sipUsername(sipUsername)
                .sipPassword(sipPassword)
                .domain(sipDomain)
                .isActive(true)
                .createTime(System.currentTimeMillis())
                .build();
        
        // 保存SIP用户
        return sipUserRepository.save(sipUser);
    }
    
    /**
     * 根据SIP用户名查找SIP用户
     * 
     * @param sipUsername SIP用户名
     * @return SIP用户
     */
    @Override
    public Optional<SipUser> findBySipUsername(String sipUsername) {
        return sipUserRepository.findBySipUsername(sipUsername);
    }
    
    /**
     * 根据用户ID查找SIP用户列表
     * 
     * @param userId 用户ID
     * @return SIP用户列表
     */
    @Override
    public List<SipUser> findByUserId(String userId) {
        return sipUserRepository.findByUserId(userId);
    }
    
    /**
     * 处理FreeSWITCH directory请求
     * 这个方法用于响应FreeSWITCH的用户认证请求
     * 
     * @param section section参数
     * @param key_value key_value参数
     * @param key key参数
     * @param user 用户名
     * @param domain 域名
     * @return XML响应
     */
    @Override
    public String handleDirectoryRequest(String section, String key_value, String key, String user, String domain) {
        log.info("处理FreeSWITCH directory请求: section={}, key_value={}, key={}, user={}, domain={}", 
                section, key_value, key, user, domain);
        
        // 如果不是directory请求，返回空XML
        if (!"directory".equals(section)) {
            return getEmptyResponse();
        }
        
        // 查找SIP用户
        Optional<SipUser> sipUserOpt = sipUserRepository.findBySipUsername(user);
        if (sipUserOpt.isEmpty()) {
            log.warn("SIP用户不存在: {}", user);
            return getEmptyResponse();
        }
        
        SipUser sipUser = sipUserOpt.get();
        
        // 如果用户未激活，返回空XML
        if (!sipUser.getIsActive()) {
            log.warn("SIP用户未激活: {}", user);
            return getEmptyResponse();
        }
        
        // 构建用户XML配置
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"directory\">\n");
        xml.append("    <domain name=\"").append(domain).append("\">\n");
        xml.append("      <params>\n");
        xml.append("        <param name=\"dial-string\" value=\"{presence_id=${dialed_user}@${dialed_domain}}${sofia_contact(${dialed_user}@${dialed_domain})}\" />\n");
        xml.append("      </params>\n");
        xml.append("      <groups>\n");
        xml.append("        <group name=\"default\">\n");
        xml.append("          <users>\n");
        xml.append("            <user id=\"").append(sipUser.getSipUsername()).append("\">\n");
        xml.append("              <params>\n");
        xml.append("                <param name=\"password\" value=\"").append(sipUser.getSipPassword()).append("\" />\n");
        xml.append("                <param name=\"vm-password\" value=\"").append(sipUser.getSipPassword()).append("\" />\n");
        xml.append("              </params>\n");
        xml.append("              <variables>\n");
        xml.append("                <variable name=\"user_context\" value=\"default\" />\n");
        xml.append("                <variable name=\"effective_caller_id_name\" value=\"").append(sipUser.getSipUsername()).append("\" />\n");
        xml.append("                <variable name=\"effective_caller_id_number\" value=\"").append(sipUser.getSipUsername()).append("\" />\n");
        xml.append("              </variables>\n");
        xml.append("            </user>\n");
        xml.append("          </users>\n");
        xml.append("        </group>\n");
        xml.append("      </groups>\n");
        xml.append("    </domain>\n");
        xml.append("  </section>\n");
        xml.append("</document>");
        
        return xml.toString();
    }
    
    /**
     * 处理FreeSWITCH configuration请求
     * 
     * @param section section参数
     * @param key_value key_value参数
     * @param key key参数
     * @return XML响应
     */
    @Override
    public String handleConfigurationRequest(String section, String key_value, String key) {
        log.info("处理FreeSWITCH configuration请求: section={}, key_value={}, key={}", section, key_value, key);
        
        // 简单返回空响应，因为我们当前不处理配置请求
        return getEmptyResponse();
    }
    
    /**
     * 处理FreeSWITCH dialplan请求
     * 
     * @param section section参数
     * @param context context参数
     * @param destinationNumber 目标号码
     * @return XML响应
     */
    @Override
    public String handleDialplanRequest(String section, String context, String destinationNumber) {
        log.info("处理FreeSWITCH dialplan请求: section={}, context={}, destination_number={}", 
                section, context, destinationNumber);
        
        // 如果不是dialplan请求，返回空XML
        if (!"dialplan".equals(section)) {
            return getEmptyResponse();
        }
        
        // 如果目标号码为空，记录警告但仍然返回有效的拨号计划
        if (destinationNumber == null || destinationNumber.isEmpty()) {
            log.warn("目标号码为空，但仍将返回有效的拨号计划以处理所有呼叫请求");
            // 不再直接返回默认拨号计划，而是继续执行并返回完整的拨号计划
        } else {
            // 如果目标号码存在，检查是否是有效的SIP用户
            boolean isValidSipUser = false;
            if (destinationNumber.matches("^[a-zA-Z0-9]+$")) {
                Optional<SipUser> sipUserOpt = sipUserRepository.findBySipUsername(destinationNumber);
                isValidSipUser = sipUserOpt.isPresent() && sipUserOpt.get().getIsActive();
                log.info("目标号码 {} 是否为有效SIP用户: {}", destinationNumber, isValidSipUser);
            }
        }
        
        // 构建拨号计划XML配置
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"dialplan\" description=\"Dynamic Dialplan\">\n");
        xml.append("    <context name=\"default\">\n");
        
        // 处理SIP动态账号呼叫
        xml.append("      <extension name=\"dynamic_sip_users\">\n");
        // 匹配任意字母数字组合（动态SIP账号通常有字母和数字）
        xml.append("        <condition field=\"destination_number\" expression=\"^([a-zA-Z0-9]+)$\">\n");
        xml.append("          <action application=\"log\" data=\"INFO Processing call to SIP account: ${destination_number}\" />\n");
        xml.append("          <action application=\"set\" data=\"hangup_after_bridge=true\" />\n");
        xml.append("          <action application=\"set\" data=\"continue_on_fail=true\" />\n");
        xml.append("          <action application=\"set\" data=\"call_timeout=30\" />\n");
        // 关键修改：使用user/前缀而非sofia/internal/
        xml.append("          <action application=\"bridge\" data=\"user/${destination_number}\" />\n");
        xml.append("        </condition>\n");
        xml.append("      </extension>\n");
        
        // 处理数字呼叫
        xml.append("      <extension name=\"numeric_extensions\">\n");
        // 匹配至少3位数字
        xml.append("        <condition field=\"destination_number\" expression=\"^(\\d{3,})$\">\n");
        xml.append("          <action application=\"log\" data=\"INFO Processing call to numeric extension: ${destination_number}\" />\n");
        xml.append("          <action application=\"set\" data=\"hangup_after_bridge=true\" />\n");
        xml.append("          <action application=\"set\" data=\"continue_on_fail=true\" />\n");
        xml.append("          <action application=\"set\" data=\"call_timeout=30\" />\n");
        // 关键修改：使用user/前缀
        xml.append("          <action application=\"bridge\" data=\"user/${destination_number}\" />\n");
        xml.append("        </condition>\n");
        xml.append("      </extension>\n");
        
        // 处理外部SIP URI呼叫
        xml.append("      <extension name=\"external_sip_uri\">\n");
        // 匹配包含@的SIP URI
        xml.append("        <condition field=\"destination_number\" expression=\"^(.+)@(.+)$\">\n");
        xml.append("          <action application=\"log\" data=\"INFO Processing call to external SIP URI: ${destination_number}\" />\n");
        xml.append("          <action application=\"set\" data=\"hangup_after_bridge=true\" />\n");
        xml.append("          <action application=\"set\" data=\"continue_on_fail=true\" />\n");
        xml.append("          <action application=\"set\" data=\"call_timeout=30\" />\n");
        // 关键修改：使用user/前缀（但这里可能需要特殊处理外部URI）
        xml.append("          <action application=\"bridge\" data=\"user/${destination_number}\" />\n");
        xml.append("        </condition>\n");
        xml.append("      </extension>\n");
        
        // 添加未匹配时的处理
        xml.append("      <extension name=\"unmatched_destination\">\n");
        xml.append("        <condition field=\"destination_number\" expression=\"^(.*)$\">\n");
        xml.append("          <action application=\"log\" data=\"WARNING Unmatched destination number, trying default bridge: ${destination_number}\" />\n");
        xml.append("          <action application=\"set\" data=\"hangup_after_bridge=true\" />\n");
        xml.append("          <action application=\"set\" data=\"continue_on_fail=true\" />\n");
        xml.append("          <action application=\"set\" data=\"call_timeout=30\" />\n");
        // 关键修改：使用user/前缀
        xml.append("          <action application=\"bridge\" data=\"user/${destination_number}\" />\n");
        xml.append("        </condition>\n");
        xml.append("      </extension>\n");
        
        xml.append("    </context>\n");
        xml.append("  </section>\n");
        xml.append("</document>");
        
        return xml.toString();
    }
    
    /**
     * 返回空响应
     * 当请求无法处理或数据不存在时返回
     * 
     * @return 空XML响应
     */
    private String getEmptyResponse() {
        return "<?xml version=\"1.0\"?>\n<document type=\"freeswitch/xml\"></document>";
    }
    
    /**
     * 返回默认拨号计划
     * 当请求中没有指定目标号码时返回
     * 
     * @return 默认拨号计划XML响应
     */
    private String getDefaultDialplan() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<document type=\"freeswitch/xml\">\n");
        xml.append("  <section name=\"dialplan\" description=\"Default Dialplan\">\n");
        xml.append("    <context name=\"default\">\n");
        xml.append("      <extension name=\"default_dialplan\">\n");
        xml.append("        <condition field=\"destination_number\" expression=\"^(.*)$\">\n");
        xml.append("          <action application=\"log\" data=\"INFO Processing call with default dialplan\" />\n");
        xml.append("          <action application=\"set\" data=\"hangup_after_bridge=true\" />\n");
        xml.append("          <action application=\"set\" data=\"continue_on_fail=true\" />\n");
        xml.append("          <action application=\"set\" data=\"call_timeout=30\" />\n");
        // 关键修改：使用user/前缀
        xml.append("          <action application=\"bridge\" data=\"user/${destination_number}\" />\n");
        xml.append("        </condition>\n");
        xml.append("      </extension>\n");
        xml.append("    </context>\n");
        xml.append("  </section>\n");
        xml.append("</document>");
        
        return xml.toString();
    }
    
    /**
     * 生成SIP用户名
     * 
     * @param username 用户名
     * @return SIP用户名
     */
    private String generateSipUsername(String username) {
        // 基于用户名和随机数生成SIP用户名
        String baseSipUsername = username.replaceAll("[^a-zA-Z0-9]", "");
        String sipUsername = baseSipUsername + new Random().nextInt(10000);
        
        // 确保SIP用户名唯一
        int attempts = 0;
        while (sipUserRepository.existsBySipUsername(sipUsername) && attempts < 10) {
            sipUsername = baseSipUsername + new Random().nextInt(10000);
            attempts++;
        }
        
        return sipUsername;
    }
    
    /**
     * 生成随机密码
     * 
     * @param length 密码长度
     * @return 随机密码
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
} 