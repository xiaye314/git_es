package com.hm;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangjialin on 17/3/22.
 */
public class DiseaseSymptomMapping {
    private String symptomId;
    private String group;
    private String relevance = "1";
    private String symptomSpecificity = "1";
    private String symptomType = "0";
    private String requirement = "0";
    private String crowdRelevance = "";

    public DiseaseSymptomMapping(){}

    public DiseaseSymptomMapping(String symptomMapping){
        if (StringUtils.isBlank(symptomMapping)){
            return;
        }
        //String[] strings = StringUtils.split(symptomMapping, "\\|");
        String[] strings = symptomMapping.split( "\\|");
        if (strings.length == 0) return;
        if (strings.length > 0) {
            this.setSymptomId(strings[0]);
        }
        if (strings.length > 1) {
            this.setGroup(strings[1]);
        }
        if (strings.length > 2) {
            this.setRelevance(strings[2]);
        }
        if (strings.length > 3) {
            this.setSymptomSpecificity(strings[3]);
        }
        if (strings.length > 4) {
            this.setSymptomType(strings[4]);
        }
        if (strings.length > 5) {
            this.setRequirement(strings[5]);
        }
        if (strings.length > 6){
            this.setCrowdRelevance(strings[6]);
        }
//        if (strings.length > 4) {
//            this.setRelevance(strings[4]);
//        }
//        if (strings.length > 5) {
//            this.setSymptomSpecificity(strings[5]);
//        }
//        if (strings.length > 6) {
//            this.setSymptomType(strings[6]);
//        }
//        if (strings.length > 7) {
//            this.setRequirement(strings[7]);
//        }
//        if (strings.length > 8){
//            this.setCrowdRelevance(strings[8]);
//        }
        if (StringUtils.isBlank(this.getGroup())){
            this.setGroup(this.getSymptomId());
        }
    }

    public String getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(String symptomId) {
        this.symptomId = symptomId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getRelevance() {
        return relevance;
    }

    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }

    public String getSymptomSpecificity() {
        return symptomSpecificity;
    }

    public void setSymptomSpecificity(String symptomSpecificity) {
        this.symptomSpecificity = symptomSpecificity;
    }

    public String getSymptomType() {
        return symptomType;
    }

    public void setSymptomType(String symptomType) {
        this.symptomType = symptomType;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public String getCrowdRelevance() {
        return crowdRelevance;
    }

    public void setCrowdRelevance(String crowdRelevance) {
        this.crowdRelevance = crowdRelevance;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("symptomId:").append(this.getSymptomId()).append(";");
        sb.append("group:").append(this.getGroup()).append(";");
        sb.append("relevance:").append(this.getRelevance()).append(";");
        sb.append("symptomSpecificity:").append(this.getSymptomSpecificity()).append(";");
        sb.append("symptomType:").append(this.getSymptomType()).append(";");
        sb.append("requirement:").append(this.getRequirement()).append(";");
        sb.append("crowdRelevance:").append(this.getCrowdRelevance()).append(";");
        return sb.toString();
    }
}
