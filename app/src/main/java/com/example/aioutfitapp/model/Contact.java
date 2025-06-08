package com.example.aioutfitapp.model;

import java.io.Serializable;

/**
 * 联系人模型类
 * 
 * 用于管理SIP通话联系人信息
 */
public class Contact implements Serializable {
    
    // 序列化ID
    private static final long serialVersionUID = 1L;
    
    // 联系人属性
    private String id;           // 唯一标识符
    private String username;     // SIP用户名
    private String displayName;  // 显示名称
    private String domain;       // SIP域名
    private String sipUri;       // 完整SIP地址URI
    private String avatar;       // 头像资源
    private boolean isFavorite;  // 是否为收藏联系人
    private long lastCallTime;   // 最后通话时间
    
    /**
     * 默认构造函数
     */
    public Contact() {
    }
    
    /**
     * 带参数构造函数
     */
    public Contact(String id, String username, String displayName, String domain) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.domain = domain;
        updateSipUri();
    }
    
    /**
     * 根据用户名和域名更新SIP URI
     */
    private void updateSipUri() {
        this.sipUri = "sip:" + username + "@" + domain;
    }
    
    // Getter和Setter方法
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
        updateSipUri();
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
        updateSipUri();
    }
    
    public String getSipUri() {
        return sipUri;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public boolean isFavorite() {
        return isFavorite;
    }
    
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
    
    public long getLastCallTime() {
        return lastCallTime;
    }
    
    public void setLastCallTime(long lastCallTime) {
        this.lastCallTime = lastCallTime;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Contact contact = (Contact) o;
        
        return sipUri != null ? sipUri.equals(contact.sipUri) : contact.sipUri == null;
    }
    
    @Override
    public int hashCode() {
        return sipUri != null ? sipUri.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return displayName != null && !displayName.isEmpty() ? displayName : username;
    }
} 