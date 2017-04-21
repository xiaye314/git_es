package com.hm;

import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lipeng
 * @date 2017/1/3
 */
public class Test {
    public static void main(String[] args) {
        Map<String,Object> params  = new HashMap<>();
        params.put("gender",0);
        params.put("symptomGroup", Arrays.asList("1","2","3"));
        params.put("userNoneSymptom",Arrays.asList("a","b","c"));
        ScoreParams scoreParams= new ScoreParams();
        try {
            BeanUtils.populate(scoreParams,params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        System.out.println(scoreParams);

        Object list=null;
        List<String> l= (List<String>) list;
        System.out.println(list);
    }
}
