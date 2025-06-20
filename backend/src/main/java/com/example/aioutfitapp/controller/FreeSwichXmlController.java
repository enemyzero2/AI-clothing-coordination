package com.example.aioutfitapp.controller;

import com.example.aioutfitapp.service.SipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FreeSWITCH XML控制器
 * 
 * 处理来自FreeSWITCH的XML请求
 */
@RestController
@Slf4j
public class FreeSwichXmlController {

    /**
     * SIP服务
     */
    @Autowired
    private SipService sipService;

    /**
     * 处理FreeSWITCH的XML请求 - 通过/freeswitch/xml路径
     * 
     * @param section 请求的section
     * @param keyValue key_value参数
     * @param key key参数
     * @param user 用户名
     * @param domain 域名
     * @param context context参数
     * @param destinationNumber 目标号码参数（dest_number或destination_number）
     * @param destNumber 目标号码参数（dest-number或dest_number变体）
     * @param callerIdNumber 主叫号码参数
     * @param callerIdName 主叫名称参数
     * @param purpose 请求目的参数
     * @return XML响应
     */
    @PostMapping(
            value = "/freeswitch/xml", 
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.TEXT_XML_VALUE
    )
    public ResponseEntity<String> handleXmlRequest(
            @RequestParam("section") String section,
            @RequestParam(value = "key_value", required = false) String keyValue,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "context", required = false) String context,
            @RequestParam(value = "destination_number", required = false) String destinationNumber,
            @RequestParam(value = "dest_number", required = false) String destNumber,
            @RequestParam(value = "caller_id_number", required = false) String callerIdNumber,
            @RequestParam(value = "caller_id_name", required = false) String callerIdName,
            @RequestParam(value = "purpose", required = false) String purpose) {

        // 如果destination_number为空但dest_number不为空，使用dest_number
        if ((destinationNumber == null || destinationNumber.isEmpty()) && destNumber != null && !destNumber.isEmpty()) {
            destinationNumber = destNumber;
        }

        return processXmlRequest(section, keyValue, key, user, domain, context, 
                destinationNumber, callerIdNumber, callerIdName, purpose);
    }
    
    /**
     * 处理FreeSWITCH的XML请求 - 通过/api/freeswitch/xml路径
     * 
     * @param section 请求的section
     * @param keyValue key_value参数
     * @param key key参数
     * @param user 用户名
     * @param domain 域名
     * @param context context参数
     * @param destinationNumber 目标号码参数（dest_number或destination_number）
     * @param destNumber 目标号码参数（dest-number或dest_number变体）
     * @param callerIdNumber 主叫号码参数
     * @param callerIdName 主叫名称参数
     * @param purpose 请求目的参数
     * @return XML响应
     */
    @PostMapping(
            value = "/api/freeswitch/xml",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.TEXT_XML_VALUE
    )
    public ResponseEntity<String> handleApiXmlRequest(
            @RequestParam("section") String section,
            @RequestParam(value = "key_value", required = false) String keyValue,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "context", required = false) String context,
            @RequestParam(value = "destination_number", required = false) String destinationNumber,
            @RequestParam(value = "dest_number", required = false) String destNumber,
            @RequestParam(value = "caller_id_number", required = false) String callerIdNumber,
            @RequestParam(value = "caller_id_name", required = false) String callerIdName,
            @RequestParam(value = "purpose", required = false) String purpose) {

        // 如果destination_number为空但dest_number不为空，使用dest_number
        if ((destinationNumber == null || destinationNumber.isEmpty()) && destNumber != null && !destNumber.isEmpty()) {
            destinationNumber = destNumber;
        }

        return processXmlRequest(section, keyValue, key, user, domain, context, 
                destinationNumber, callerIdNumber, callerIdName, purpose);
    }
    
    /**
     * 处理XML请求逻辑
     */
    private ResponseEntity<String> processXmlRequest(
            String section, String keyValue, String key, 
            String user, String domain, String context, String destinationNumber,
            String callerIdNumber, String callerIdName, String purpose) {
            
        log.info("收到FreeSWITCH XML请求: section={}, key_value={}, key={}, user={}, domain={}, context={}, " +
                "destination_number={}, caller_id_number={}, caller_id_name={}, purpose={}", 
                section, keyValue, key, user, domain, context, destinationNumber, 
                callerIdNumber, callerIdName, purpose);
        
        String xmlResponse;
        
        // 根据section类型调用不同的处理方法
        switch (section) {
            case "directory":
                xmlResponse = sipService.handleDirectoryRequest(section, keyValue, key, user, domain);
                break;
            case "configuration":
                xmlResponse = sipService.handleConfigurationRequest(section, keyValue, key);
                break;
            case "dialplan":
                xmlResponse = sipService.handleDialplanRequest(section, context, destinationNumber);
                break;
            default:
                // 返回空XML响应
                xmlResponse = "<?xml version=\"1.0\"?>\n<document type=\"freeswitch/xml\"></document>";
                break;
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(xmlResponse);
    }

    /**
     * 测试端点，用于验证FreeSWITCH XML控制器是否可访问
     * 
     * @return 测试响应
     */
    @GetMapping(value = {"/freeswitch/test", "/api/freeswitch/test"})
    public ResponseEntity<String> testEndpoint() {
        log.info("测试端点被访问");
        return ResponseEntity.ok("FreeSWITCH XML控制器测试端点正常工作");
    }
} 