package com.example.aioutfitapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.aioutfitapp.network.CallManager;
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
                    // 显示SIP账号选择器
                    showSipAccountSelector();
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
        // 显示SIP账号选择器
        showSipAccountSelector();
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
        try {
            // 构建SIP地址
            String sipAddress;
            if (contact.getUsername().contains("@")) {
                // 已经是完整SIP地址
                sipAddress = contact.getUsername();
            } else {
                // 使用FreeSwitch服务器格式
                sipAddress = contact.getUsername() + "@10.29.206.148";
            }
            
            // 创建通话Intent
            Intent intent = CallActivity.createOutgoingCallIntent(
                    this,
                    CallManager.CALL_TYPE_AUDIO,
                    sipAddress,
                    "default-room"
            );
            
            // 启动通话活动
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "发起通话失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_freesip_accounts) {
            showFreeSwitchAccounts();
            return true;
        } else if (id == R.id.action_sip_status) {
            showSipStatus();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 显示FreeSwitch账号信息
     */
    private void showFreeSwitchAccounts() {
        CallManager callManager = CallManager.getInstance(this);
        String accountsInfo = callManager.getAllAccountsInfo();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("FreeSwitch可用账号");
        builder.setMessage(accountsInfo);
        builder.setPositiveButton("确定", null);
        builder.show();
    }
    
    /**
     * 显示SIP状态信息
     */
    private void showSipStatus() {
        CallManager callManager = CallManager.getInstance(this);
        String statusInfo = callManager.getSipAccountStatus();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SIP账号状态");
        builder.setMessage(statusInfo);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("切换账号", (dialog, which) -> {
            showSipAccountSelector();
        });
        builder.show();
    }
    
    /**
     * 显示SIP账号选择器
     */
    private void showSipAccountSelector() {
        CallManager callManager = CallManager.getInstance(this);
        List<String> accounts = callManager.getAvailableSipAccounts();
        String[] accountsArray = accounts.toArray(new String[0]);
        
        // 显示账号信息
        String[] displayArray = new String[accountsArray.length];
        for (int i = 0; i < accountsArray.length; i++) {
            displayArray[i] = accountsArray[i] + " (密码: 1234)";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要使用的SIP账号");
        builder.setSingleChoiceItems(displayArray, 0, null);
        builder.setPositiveButton("登录", (dialog, which) -> {
            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < accounts.size()) {
                String selectedAccount = accounts.get(selectedPosition);
                loginToSipWithAccount(selectedAccount, selectedPosition);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 使用指定账号登录SIP
     */
    private void loginToSipWithAccount(String username, int accountIndex) {
        // 显示登录中提示
        Toast.makeText(this, "正在连接SIP服务器使用账号 " + username + "...", Toast.LENGTH_SHORT).show();
        
        CallManager callManager = CallManager.getInstance(this);
        callManager.setCurrentAccountIndex(accountIndex);
        
        if (callManager.initializeSipWithAccount(username)) {
            Toast.makeText(this, "正在登录SIP账号: " + username, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SIP账号初始化失败: " + username, Toast.LENGTH_SHORT).show();
        }
    }
} 