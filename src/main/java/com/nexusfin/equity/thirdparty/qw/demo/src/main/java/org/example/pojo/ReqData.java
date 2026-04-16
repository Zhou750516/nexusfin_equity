package org.example.pojo;

import lombok.Data;

@Data
// 合作商平台请求接口
public class ReqData {

    // 使用AES加密之后的业务数据, 业务数据请求参考具体接口，加密源为String类型的JSON格式数据
    private String requestBody;

    // 请求头
    private ReqHead requestHead;

}
