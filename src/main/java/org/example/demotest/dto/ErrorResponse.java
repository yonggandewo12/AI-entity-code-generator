package org.example.demotest.dto;

/**
 * 标准JSON错误响应载荷。
 *
 * @author Liang.Xu
 */
public class ErrorResponse {

    private String error;

    private String message;

    private String path;

    private long timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message, String path, long timestamp) {
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
