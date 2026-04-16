package org.example;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.example.common.AESHelper;
import org.example.common.Response;
import org.example.pojo.ReqData;
import org.example.pojo.ReqHead;
import org.example.pojo.DemoParams;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    //接口地址(需要配置)
    public static  final String API_URL = "http://t-api.test.qweimobile.com/api/partner/demo/method";
    public static void main(String[] args) {
        try {
            //MD5密钥
            String MD5_KEY = "WwZTwihkdJHDLh8kiSCFsJrF6hfMS5iE";
            //AES密钥
            String AES_KEY = "FCiA8fNy6zjH4cHt";

            DemoParams params = new DemoParams();
            params.setUniqueId("UniqueId3344556677");
            params.setPayAmount(1000);
            params.setProductCode("yfk001");
            params.setPhone("13500000000");

            ReqHead reqHead = new ReqHead();
            reqHead.setMethod("jdm.active.notify");
            reqHead.setPartnerNo("jdm");
            reqHead.setVersion("v1.0");
            reqHead.setTimestamp(System.currentTimeMillis());
            String sign = DigestUtil.md5Hex(String.format("%s%s%s%s%s",
                    reqHead.getMethod(),
                    reqHead.getPartnerNo(),
                    reqHead.getTimestamp(),
                    reqHead.getVersion(),
                    MD5_KEY), StandardCharsets.UTF_8);
            reqHead.setSign(sign);
            ReqData reqData = new ReqData();
            reqData.setRequestHead(reqHead);
            reqData.setRequestBody(AESHelper.EncryptByKey(AES_KEY, JSON.toJSONString(params)));
            System.out.println(JSON.toJSONString(reqData));

            HttpRequest httpRequest = HttpUtil.createPost(API_URL);
            Map<String,String> map = new HashMap<>();
            map.put("Content-Type","application/json");
            httpRequest.addHeaders(map);
            httpRequest.body(JSON.toJSONString(reqData));

            HttpResponse execute = httpRequest.execute(false);
            String resBody = execute.body();
            System.out.println(resBody);
            Response res = JSON.parseObject(resBody, Response.class);
            if(res != null && res.getCode() == 200){
                String data = res.getData();
                if(StringUtils.isNotEmpty(data)){
                    String resData = AESHelper.DecryptByKey(AES_KEY, data);
                    System.out.println(resData);
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}