package com.hm;

import java.util.List;

/**
 * @author lipeng
 * @date 2017/1/3
 */
public class ScoreParams {
    Integer gender;
    List<String> symptomGroup;
    List<Integer> userNoneSymptom;
    List<String> userSymptom;
    Double age;
    boolean debug;

    String callbackWords;

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public List<String> getSymptomGroup() {
        return symptomGroup;
    }

    public void setSymptomGroup(List<String> symptomGroup) {
        this.symptomGroup = symptomGroup;
    }

    public List<Integer> getUserNoneSymptom() {
        return userNoneSymptom;
    }

    public void setUserNoneSymptom(List<Integer> userNoneSymptom) {
        this.userNoneSymptom = userNoneSymptom;
    }

    public List<String> getUserSymptom() {
        return userSymptom;
    }

    public void setUserSymptom(List<String> userSymptom) {
        this.userSymptom = userSymptom;
    }

    public Double getAge() {
        return age;
    }

    public void setAge(Double age) {
        this.age = age;
    }

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getCallbackWords() {
        return callbackWords;
    }

    public void setCallbackWords(String callbackWords) {
        this.callbackWords = callbackWords;
    }
}
