package org.example.common;

import lombok.Data;

@Data
// 权益平台通用请求返回接收类
public class Response {

    /**
     * 200成功，500错误
     */
    private int code = 500;

    private String msg;
    /**
     * 使用AES加密之后的业务数据
     * 业务数据请求参考具体接口
     * 加密源为String类型的JSON格式数据
     */
    private String data;

    public Response(){

    }

    public Response(int code, String msg, String data){
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public Response(int code, String data){
        this.code = code;
        this.data = data;
    }
}
