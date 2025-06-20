package com.example.aioutfitapp.api.models;

/**
 * SIP账户信息模型
 */
public class SipAccount {
    private String sipUsername; // SIP用户名
    private String sipPassword; // SIP密码
    private String sipDomain; // SIP域名
    private String sipServerAddress; // SIP服务器地址
    private String sipServerPort; // SIP服务器端口
    
    /**
     * 构造函数
     */
    public SipAccount() {
    }
    
    /**
     * 构造函数
     * 
     * @param sipUsername SIP用户名
     * @param sipPassword SIP密码
     * @param sipDomain SIP域名
     * @param sipServerAddress SIP服务器地址
     * @param sipServerPort SIP服务器端口
     */
    public SipAccount(String sipUsername, String sipPassword, String sipDomain, 
                     String sipServerAddress, String sipServerPort) {
        this.sipUsername = sipUsername;
        this.sipPassword = sipPassword;
        this.sipDomain = sipDomain;
        this.sipServerAddress = sipServerAddress;
        this.sipServerPort = sipServerPort;
    }
    
    // Getter 和 Setter 方法
    
    public String getSipUsername() {
        return sipUsername;
    }
    
    public void setSipUsername(String sipUsername) {
        this.sipUsername = sipUsername;
    }
    
    public String getSipPassword() {
        return sipPassword;
    }
    
    public void setSipPassword(String sipPassword) {
        this.sipPassword = sipPassword;
    }
    
    public String getSipDomain() {
        return sipDomain;
    }
    
    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }
    
    public String getSipServerAddress() {
        return sipServerAddress;
    }
    
    public void setSipServerAddress(String sipServerAddress) {
        this.sipServerAddress = sipServerAddress;
    }
    
    public String getSipServerPort() {
        return sipServerPort;
    }
    
    public void setSipServerPort(String sipServerPort) {
        this.sipServerPort = sipServerPort;
    }
} 