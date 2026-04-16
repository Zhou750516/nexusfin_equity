package org.example.pojo;

import lombok.Data;
import org.example.common.ValidateUtil;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

@Data
public class DemoParams {

    @NotEmpty(message = "用户ID不能为空")
    private String uniqueId;

    @NotNull(message = "贷款金额不能为空")
    private Integer payAmount;

    @NotEmpty(message = "产品编码不能为空")
    private String productCode;


    @NotEmpty(message = "客户手机号不能为空")
    private String phone;


    public String checkParams(){
        Map<String, String> validate = ValidateUtil.validate(this);
        if(validate!=null && !validate.isEmpty()){
            StringBuilder sb = new StringBuilder();
            Set<String> keySet = validate.keySet();
            int size = keySet.size();
            int i =0;
            for (String s : keySet) {
                String s1 = validate.get(s);
                sb.append(s1);
                if(i + 1 < size){
                    sb.append(",");
                }
                i++;
            }
            return sb.toString();
        }
        return null;
    }
}
