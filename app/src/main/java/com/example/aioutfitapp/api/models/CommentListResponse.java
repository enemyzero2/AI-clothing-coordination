package com.example.aioutfitapp.api.models;

import java.util.List;

/**
 * 评论列表响应模型
 */
public class CommentListResponse {
    private boolean success; // 是否成功
    private String message; // 响应消息
    private List<CommentResponse> comments; // 评论列表
    private int total; // 总数
    private int page; // 当前页码
    private int pageSize; // 每页数量
    private int totalPages; // 总页数
    
    /**
     * 构造函数
     */
    public CommentListResponse() {
    }
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 响应消息
     * @param comments 评论列表
     * @param total 总数
     * @param page 当前页码
     * @param pageSize 每页数量
     * @param totalPages 总页数
     */
    public CommentListResponse(boolean success, String message, List<CommentResponse> comments,
                              int total, int page, int pageSize, int totalPages) {
        this.success = success;
        this.message = message;
        this.comments = comments;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }
    
    // Getter 和 Setter 方法
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<CommentResponse> getComments() {
        return comments;
    }
    
    public void setComments(List<CommentResponse> comments) {
        this.comments = comments;
    }
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
} 