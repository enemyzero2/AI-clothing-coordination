package com.example.aioutfitapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aioutfitapp.model.Contact;
import com.example.aioutfitapp.model.ContactManager;
import com.example.aioutfitapp.network.LinphoneManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * 联系人列表界面
 * 
 * 用于显示SIP联系人列表，并提供添加、编辑、删除和呼叫功能
 */
public class ContactsActivity extends AppCompatActivity {

    private static final String TAG = "ContactsActivity";
    
    private ListView contactsListView;
    private FloatingActionButton fabAdd;
    private ArrayAdapter<Contact> adapter;
    private ContactManager contactManager;
    private List<Contact> contacts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("联系人");
        }
        
        // 初始化组件
        contactsListView = findViewById(R.id.contacts_list_view);
        fabAdd = findViewById(R.id.fab_add_contact);
        
        // 初始化联系人管理器
        contactManager = ContactManager.getInstance(this);
        
        // 加载联系人列表
        loadContacts();
        
        // 设置添加按钮点击事件
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddContactDialog();
            }
        });
        
        // 设置列表项点击事件
        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contacts.get(position);
                showContactOptionsDialog(contact);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 刷新联系人列表
        loadContacts();
    }
    
    /**
     * 加载联系人列表
     */
    private void loadContacts() {
        contacts = contactManager.getAllContacts();
        
        // 设置适配器
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);
        contactsListView.setAdapter(adapter);
        
        // 显示空列表提示
        if (contacts.isEmpty()) {
            Toast.makeText(this, "暂无联系人", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示添加联系人对话框
     */
    private void showAddContactDialog() {
        // 创建简单的添加联系人对话框
        // 在实际应用中，这应该是一个完整的表单界面
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加新联系人");
        builder.setMessage("这里将实现添加联系人表单");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 在实际应用中，从表单获取数据添加联系人
                // 简单示例：添加一个测试联系人
                Contact newContact = new Contact(
                        "",
                        "user" + System.currentTimeMillis(),
                        "新联系人",
                        "aioutfitapp.local"
                );
                
                if (contactManager.addContact(newContact)) {
                    Toast.makeText(ContactsActivity.this, "联系人已添加", Toast.LENGTH_SHORT).show();
                    loadContacts();
                } else {
                    Toast.makeText(ContactsActivity.this, "添加联系人失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示联系人操作选项对话框
     */
    private void showContactOptionsDialog(final Contact contact) {
        String[] options = {"呼叫", "编辑", "删除"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contact.getDisplayName());
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        callContact(contact);
                        break;
                    case 1:
                        editContact(contact);
                        break;
                    case 2:
                        deleteContact(contact);
                        break;
                }
            }
        });
        builder.show();
    }
    
    /**
     * 呼叫联系人
     */
    private void callContact(Contact contact) {
        // 检查SIP是否已注册
        LinphoneManager linphoneManager = LinphoneManager.getInstance();
        if (!linphoneManager.isRegistered()) {
            // 提示用户需要先登录SIP
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("SIP未注册");
            builder.setMessage("您需要先登录SIP服务器才能进行通话。是否立即登录？");
            builder.setPositiveButton("登录", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 这里进行SIP登录(示例使用默认账号)
                    loginToSip(contact);
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
            return;
        }

        // SIP已注册，直接拨打电话
        startCall(contact);
    }
    
    /**
     * 登录到SIP服务器
     */
    private void loginToSip(Contact contact) {
        // 显示登录中提示
        Toast.makeText(this, "正在连接SIP服务器...", Toast.LENGTH_SHORT).show();
        
        // 初始化Linphone(如果尚未初始化)
        LinphoneManager linphoneManager = LinphoneManager.getInstance();
        
        // 检查网络连接
        if (!isNetworkConnected()) {
            Toast.makeText(this, "网络连接异常，请检查网络设置", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 检查Linphone初始化状态并输出详细日志
        if (linphoneManager.getCore() == null) {
            try {
                linphoneManager.init(this);
                android.util.Log.d(TAG, "Linphone核心引擎初始化成功");
            } catch (Exception e) {
                String errorMsg = "Linphone初始化失败: " + e.getMessage();
                android.util.Log.e(TAG, errorMsg, e);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        android.util.Log.d(TAG, "尝试SIP登录 - 用户名: admin, 域名: aioutfitapp.local, 端口: 5062");
        
        // 开始登录前先监听回调
        linphoneManager.setListener(new LinphoneManager.LinphoneManagerListener() {
            @Override
            public void onCallStateChanged(int state) {
                android.util.Log.d(TAG, "通话状态变更: " + state);
            }
            
            @Override
            public void onIncomingCall(String callerId, int callType) {
                android.util.Log.d(TAG, "收到来电: " + callerId + ", 类型: " + callType);
            }
            
            @Override
            public void onRegistered() {
                // 注册成功后拨打电话
                android.util.Log.i(TAG, "SIP注册成功");
                runOnUiThread(() -> {
                    Toast.makeText(ContactsActivity.this, "SIP注册成功，开始通话", Toast.LENGTH_SHORT).show();
                    startCall(contact);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e(TAG, "SIP注册错误: " + errorMessage);
                runOnUiThread(() -> {
                    // 显示更详细的错误提示
                    showSipErrorDialog("SIP登录失败", errorMessage);
                    
                    // 域名解析失败时尝试使用IP地址
                    if (errorMessage.contains("域名解析失败") || errorMessage.contains("UnknownHostException")) {
                        showIpConnectionDialog(contact);
                    }
                });
            }
        });
        
        // 使用默认配置登录并添加超时处理
        try {
            linphoneManager.login("admin", "admin123", "aioutfitapp.local", "5062", "udp", new LinphoneManager.SipCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.i(TAG, "SIP登录成功");
                    runOnUiThread(() -> {
                        Toast.makeText(ContactsActivity.this, "SIP注册成功，开始通话", Toast.LENGTH_SHORT).show();
                        startCall(contact);
                    });
                }
                
                @Override
                public void onLoginStarted() {
                    android.util.Log.d(TAG, "SIP登录开始");
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e(TAG, "SIP登录错误: " + errorMessage);
                    runOnUiThread(() -> {
                        showSipErrorDialog("SIP登录失败", errorMessage);
                    });
                }
            });
            
            // 添加注册超时检查
            new android.os.Handler().postDelayed(() -> {
                if (!linphoneManager.isRegistered()) {
                    android.util.Log.e(TAG, "SIP注册超时");
                    runOnUiThread(() -> {
                        showSipErrorDialog("SIP注册超时", 
                            "连接服务器超时，请检查:\n" +
                            "1. 网络连接状态\n" +
                            "2. SIP服务器地址和端口是否正确\n" +
                            "3. SIP服务器是否在线\n" +
                            "4. 账号密码是否正确");
                        
                        // 尝试使用本机IP连接
                        showIpConnectionDialog(contact);
                    });
                }
            }, 15000); // 15秒超时
        } catch (Exception e) {
            String errorMsg = "SIP登录异常: " + e.getMessage();
            android.util.Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 显示IP地址连接对话框
     */
    private void showIpConnectionDialog(Contact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SIP服务器连接问题");
        builder.setMessage("尝试使用IP地址直接连接SIP服务器？");
        
        // 添加输入框
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText("10.29.206.148"); // 使用主机IP地址
        builder.setView(input);
        
        builder.setPositiveButton("连接", (dialog, which) -> {
            String ipAddress = input.getText().toString().trim();
            if (!ipAddress.isEmpty()) {
                android.util.Log.d(TAG, "尝试使用IP地址连接SIP服务器: " + ipAddress);
                Toast.makeText(this, "正在连接IP: " + ipAddress, Toast.LENGTH_SHORT).show();
                
                // 使用IP地址登录
                LinphoneManager linphoneManager = LinphoneManager.getInstance();
                linphoneManager.login("admin", "admin123", ipAddress, "5062", "udp", new LinphoneManager.SipCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.i(TAG, "使用IP地址SIP登录成功");
                        runOnUiThread(() -> {
                            Toast.makeText(ContactsActivity.this, "SIP注册成功，开始通话", Toast.LENGTH_SHORT).show();
                            startCall(contact);
                        });
                    }
                    
                    @Override
                    public void onLoginStarted() {
                        android.util.Log.d(TAG, "SIP登录开始");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.e(TAG, "SIP登录错误: " + errorMessage);
                        runOnUiThread(() -> {
                            showSipErrorDialog("SIP登录失败", errorMessage);
                        });
                    }
                });
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示SIP错误对话框
     */
    private void showSipErrorDialog(String title, String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(errorMessage);
        builder.setPositiveButton("查看日志", (dialog, which) -> {
            // 显示日志信息对话框
            showLogDialog();
        });
        builder.setNegativeButton("确定", null);
        builder.show();
    }
    
    /**
     * 显示日志对话框
     */
    private void showLogDialog() {
        // 收集SIP连接相关日志
        String logs = collectSipLogs();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SIP连接日志");
        builder.setMessage(logs);
        builder.setPositiveButton("复制", (dialog, which) -> {
            // 复制日志到剪贴板
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = 
                android.content.ClipData.newPlainText("SIP日志", logs);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }
    
    /**
     * 收集SIP连接日志
     */
    private String collectSipLogs() {
        LinphoneManager linphoneManager = LinphoneManager.getInstance();
        StringBuilder sb = new StringBuilder();
        
        sb.append("设备信息:\n");
        sb.append("Android版本: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        sb.append("设备型号: ").append(android.os.Build.MODEL).append("\n\n");
        
        sb.append("SIP连接信息:\n");
        sb.append("服务器: aioutfitapp.local:5062\n");
        sb.append("用户名: admin\n");
        sb.append("注册状态: ").append(linphoneManager.isRegistered() ? "已注册" : "未注册").append("\n\n");
        
        sb.append("连接诊断:\n");
        sb.append("网络状态: ").append(isNetworkConnected() ? "已连接" : "未连接").append("\n");
        if (linphoneManager.getCore() != null) {
            sb.append("Linphone状态: 已初始化\n");
        } else {
            sb.append("Linphone状态: 未初始化\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 检查网络连接状态
     */
    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm = 
            (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * 开始通话
     */
    private void startCall(Contact contact) {
        // 启动通话活动
        String roomId = "room" + System.currentTimeMillis();
        Intent intent = CallActivity.createOutgoingCallIntent(
                this, 
                0, // 默认音频通话
                contact.getUsername(),
                roomId
        );
        startActivity(intent);
        
        // 更新最后通话时间
        contactManager.updateContactCallTime(contact.getUsername(), System.currentTimeMillis());
    }
    
    /**
     * 编辑联系人
     */
    private void editContact(final Contact contact) {
        // 此处应弹出编辑表单
        // 简化为示例修改
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑联系人");
        builder.setMessage("这里将实现联系人编辑表单");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 示例修改
                contact.setDisplayName(contact.getDisplayName() + " (已更新)");
                if (contactManager.updateContact(contact)) {
                    Toast.makeText(ContactsActivity.this, "联系人已更新", Toast.LENGTH_SHORT).show();
                    loadContacts();
                } else {
                    Toast.makeText(ContactsActivity.this, "更新联系人失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 删除联系人
     */
    private void deleteContact(final Contact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除联系人");
        builder.setMessage("确定要删除 " + contact.getDisplayName() + " 吗？");
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (contactManager.deleteContact(contact.getId())) {
                    Toast.makeText(ContactsActivity.this, "联系人已删除", Toast.LENGTH_SHORT).show();
                    loadContacts();
                } else {
                    Toast.makeText(ContactsActivity.this, "删除联系人失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 