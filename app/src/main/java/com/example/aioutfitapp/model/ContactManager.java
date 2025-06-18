package com.example.aioutfitapp.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 联系人管理类
 * 
 * 管理SIP联系人的增删改查，并持久化到本地存储
 */
public class ContactManager {
    
    private static final String TAG = "ContactManager";
    
    // 存储键值
    private static final String PREF_NAME = "contacts_preferences";
    private static final String KEY_CONTACTS = "key_contacts_list";
    
    // 单例实例
    private static ContactManager instance;
    
    // 上下文与数据
    private final Context context;
    private List<Contact> contacts;
    private final SharedPreferences preferences;
    private final Gson gson;
    
    /**
     * 获取单例实例
     */
    public static synchronized ContactManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContactManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     */
    private ContactManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.contacts = loadContacts();
        
        // 如果没有联系人，添加默认测试联系人
        if (contacts.isEmpty()) {
            addDefaultContacts();
        }
    }
    
    /**
     * 从本地存储加载联系人列表
     */
    private List<Contact> loadContacts() {
        String contactsJson = preferences.getString(KEY_CONTACTS, "");
        
        if (contactsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<Contact>>(){}.getType();
            return gson.fromJson(contactsJson, listType);
        } catch (Exception e) {
            Log.e(TAG, "加载联系人失败: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存联系人列表到本地存储
     */
    private void saveContacts() {
        try {
            String contactsJson = gson.toJson(contacts);
            preferences.edit().putString(KEY_CONTACTS, contactsJson).apply();
            Log.d(TAG, "联系人已保存，总数: " + contacts.size());
        } catch (Exception e) {
            Log.e(TAG, "保存联系人失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加默认测试联系人
     */
    private void addDefaultContacts() {
        String domain = "aioutfitapp.local";
        
        // 添加SIP账号联系人
        contacts.add(new Contact(generateId(), "1001", "用户1001", domain));
        contacts.add(new Contact(generateId(), "1002", "用户1002", domain));
        contacts.add(new Contact(generateId(), "1003", "用户1003", domain));
        contacts.add(new Contact(generateId(), "1004", "用户1004", domain));
        contacts.add(new Contact(generateId(), "1005", "用户1005", domain));
        contacts.add(new Contact(generateId(), "1006", "用户1006", domain));
        contacts.add(new Contact(generateId(), "1007", "用户1007", domain));
        contacts.add(new Contact(generateId(), "1008", "用户1008", domain));
        contacts.add(new Contact(generateId(), "1009", "用户1009", domain));
        contacts.add(new Contact(generateId(), "1010", "用户1010", domain));
        
        // 保存默认联系人
        saveContacts();
        Log.d(TAG, "已添加默认SIP账号联系人");
    }
    
    /**
     * 生成唯一ID
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 获取所有联系人列表
     */
    public List<Contact> getAllContacts() {
        return new ArrayList<>(contacts);
    }
    
    /**
     * 获取收藏的联系人列表
     */
    public List<Contact> getFavoriteContacts() {
        List<Contact> favoriteContacts = new ArrayList<>();
        
        for (Contact contact : contacts) {
            if (contact.isFavorite()) {
                favoriteContacts.add(contact);
            }
        }
        
        return favoriteContacts;
    }
    
    /**
     * 根据ID查找联系人
     */
    public Contact getContactById(String id) {
        for (Contact contact : contacts) {
            if (contact.getId().equals(id)) {
                return contact;
            }
        }
        return null;
    }
    
    /**
     * 根据用户名查找联系人
     */
    public Contact getContactByUsername(String username) {
        for (Contact contact : contacts) {
            if (contact.getUsername().equals(username)) {
                return contact;
            }
        }
        return null;
    }
    
    /**
     * 添加新联系人
     * 
     * @return 是否添加成功
     */
    public boolean addContact(Contact contact) {
        // 检查是否已存在
        for (Contact existingContact : contacts) {
            if (existingContact.getUsername().equals(contact.getUsername()) && 
                existingContact.getDomain().equals(contact.getDomain())) {
                Log.w(TAG, "联系人已存在: " + contact.getUsername());
                return false;
            }
        }
        
        // 确保有ID
        if (contact.getId() == null || contact.getId().isEmpty()) {
            contact.setId(generateId());
        }
        
        // 添加到列表
        contacts.add(contact);
        saveContacts();
        
        Log.d(TAG, "已添加联系人: " + contact.getUsername());
        return true;
    }
    
    /**
     * 更新联系人信息
     * 
     * @return 是否更新成功
     */
    public boolean updateContact(Contact contact) {
        // 查找联系人索引
        int index = -1;
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(contact.getId())) {
                index = i;
                break;
            }
        }
        
        // 不存在则返回失败
        if (index == -1) {
            Log.w(TAG, "更新失败，联系人不存在: " + contact.getId());
            return false;
        }
        
        // 更新联系人并保存
        contacts.set(index, contact);
        saveContacts();
        
        Log.d(TAG, "已更新联系人: " + contact.getUsername());
        return true;
    }
    
    /**
     * 删除联系人
     * 
     * @return 是否删除成功
     */
    public boolean deleteContact(String id) {
        // 查找联系人索引
        int index = -1;
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        
        // 不存在则返回失败
        if (index == -1) {
            Log.w(TAG, "删除失败，联系人不存在: " + id);
            return false;
        }
        
        // 删除联系人并保存
        contacts.remove(index);
        saveContacts();
        
        Log.d(TAG, "已删除联系人，ID: " + id);
        return true;
    }
    
    /**
     * 清空所有联系人
     */
    public void clearAllContacts() {
        contacts.clear();
        saveContacts();
        Log.d(TAG, "已清空所有联系人");
    }
    
    /**
     * 更新联系人通话时间
     */
    public void updateContactCallTime(String username, long timestamp) {
        Contact contact = getContactByUsername(username);
        if (contact != null) {
            contact.setLastCallTime(timestamp);
            updateContact(contact);
            Log.d(TAG, "已更新联系人通话时间: " + username);
        }
    }
    
    /**
     * 切换联系人收藏状态
     */
    public void toggleFavorite(String id) {
        Contact contact = getContactById(id);
        if (contact != null) {
            contact.setFavorite(!contact.isFavorite());
            updateContact(contact);
            Log.d(TAG, "已切换联系人收藏状态: " + contact.getUsername() + ", 收藏: " + contact.isFavorite());
        }
    }
} 