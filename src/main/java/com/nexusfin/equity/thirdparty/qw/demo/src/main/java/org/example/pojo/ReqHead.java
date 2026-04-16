package org.example.pojo;

import lombok.Data;

@Data
// "合作商平台请求接口头
public class ReqHead {

    // "商户编号
    private String partnerNo;

    // "时间戳
    private Long timestamp;

    // 方法名
    private String method;

    // 版本号 固定值：v1.0
    private String version;

    // 签名，Md5(String.format(\"%s%s%s%s%s\", method ,partnerNo,timestamp,version,key))
    private String sign;

}
