package org.example.common;


import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @description validator参数校验类
 */
public class ValidateUtil {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验实体类
     * @param t  参数
     * @return null为校验成功，否则校验失败。返回map，key为字段名称，value为错误消息
     */
    public static <T> Map<String,String> validate(T t) {
        if (t == null) {
            return null;
        }
        Set<ConstraintViolation<T>> constraintViolations = VALIDATOR.validate(t);
        if (constraintViolations.isEmpty()) {
            return null;
        }
        Map<String, String> map = buildResultMap(constraintViolations);
        if(map.isEmpty()){
            return null;
        }
        return map;
    }

    /**
     * 通过组来校验实体类
     * @param t 参数
     * @param groups 校验组
     * @return null为校验成功，否则校验失败。返回map，key为字段名称，value为错误消息
     */
    public static <T> Map<String,String> validate(T t, Class<?>... groups) {
        if (t == null) {
            return null;
        }
        Set<ConstraintViolation<T>> constraintViolations = VALIDATOR.validate(t, groups);
        if (constraintViolations.isEmpty()) {
            return null;
        }
        Map<String, String> map = buildResultMap(constraintViolations);
        if(map.isEmpty()){
            return null;
        }
        return map;
    }

    /**
     * 根据参数构建校验失败的map
     * @param constraintViolations
     * @return 返回map，key为错误的字段名称，value为错误的消息
     */
    private static <T> Map<String,String> buildResultMap(Set<ConstraintViolation<T>> constraintViolations){
        Map<String,String> map = new HashMap<>();
        //只获取第一个错误
        for (ConstraintViolation<T> constraintViolation : constraintViolations) {
            Path propertyPath = constraintViolation.getPropertyPath();
            String field = null;
            if(propertyPath!=null){
                Iterator<Path.Node> iterator = propertyPath.iterator();
                while (iterator.hasNext()) {
                    Path.Node next = iterator.next();
                    field = next.getName();
                }
            }
            map.put(field,constraintViolation.getMessage());
        }
        return map;
    }
}

