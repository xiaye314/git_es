package com.hm;
import java.util.*;

/**
 * @author lipeng
 * @date 2016/11/7
 */
public class DiseaseDoc {
    private String id;
    private String diseaseName;
    private String diseaseCode;
    private String diseaseAlias;
    private String department;
    private String symptomListStr;//疾病关联的诊断因子id，用’|’将所有的诊断因子拼接成字符串；需要在该字段上建倒排索引
//    private List<SymptomPojo> symptomList;
//
//    private List<DiseaseDistributionPojo> distGroup = new ArrayList<>();
//    private List<DiseaseDistributionPojo> distGroupByGenderSeason = new ArrayList<>();
//    private List<DiseaseDistributionPojo> distGroupByAgeGroupSeason = new ArrayList<>();
//    private List<DiseaseDistributionPojo> distGroupBySeason = new ArrayList<>();

    // [symptomId | symptomGroupId | relevance | inDiseaseRate ]
    private List<String> symptomList;

    private List<String> distGroup;
    private List<String> distGroupByGenderSeason;
    private List<String> distGroupByAgeGroupSeason;
    private List<String> distGroupBySeason;

    private Integer groupTotalCount;
    private Integer freq;
    // 性别
    private Integer gender;
    // 是否危急重症
    private Integer isCritical;
    private String pinyin;
    private String pinyinFirst;
    // 是否上线
    private int online;//0 1
    private String modifyDate;
    private Integer prevalence;
    // 召回词集合
    private  List<String> callbackWords;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getDiseaseCode() {
        return diseaseCode;
    }

    public void setDiseaseCode(String diseaseCode) {
        this.diseaseCode = diseaseCode;
    }

    public String getDiseaseAlias() {
        return diseaseAlias;
    }

    public void setDiseaseAlias(String diseaseAlias) {
        this.diseaseAlias = diseaseAlias;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSymptomListStr() {
        return symptomListStr;
    }

    public void setSymptomListStr(String symptomListStr) {
        this.symptomListStr = symptomListStr;
    }

    public List<String> getSymptomList() {
        return symptomList;
    }

    public void setSymptomList(List<String> symptomList) {
        this.symptomList = symptomList;
    }

    public List<String> getDistGroup() {
        return distGroup;
    }

    public void setDistGroup(List<String> distGroup) {
        this.distGroup = distGroup;
    }

    public List<String> getDistGroupByGenderSeason() {
        return distGroupByGenderSeason;
    }

    public void setDistGroupByGenderSeason(List<String> distGroupByGenderSeason) {
        this.distGroupByGenderSeason = distGroupByGenderSeason;
    }

    public List<String> getDistGroupByAgeGroupSeason() {
        return distGroupByAgeGroupSeason;
    }

    public void setDistGroupByAgeGroupSeason(List<String> distGroupByAgeGroupSeason) {
        this.distGroupByAgeGroupSeason = distGroupByAgeGroupSeason;
    }

    public List<String> getDistGroupBySeason() {
        return distGroupBySeason;
    }

    public void setDistGroupBySeason(List<String> distGroupBySeason) {
        this.distGroupBySeason = distGroupBySeason;
    }

    public Integer getGroupTotalCount() {
        return groupTotalCount;
    }

    public void setGroupTotalCount(Integer groupTotalCount) {
        this.groupTotalCount = groupTotalCount;
    }

    public Integer getFreq() {
        return freq;
    }

    public void setFreq(Integer freq) {
        this.freq = freq;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getIsCritical() {
        return isCritical;
    }

    public void setIsCritical(Integer isCritical) {
        this.isCritical = isCritical;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getPinyinFirst() {
        return pinyinFirst;
    }

    public void setPinyinFirst(String pinyinFirst) {
        this.pinyinFirst = pinyinFirst;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public String getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(String modifyDate) {
        this.modifyDate = modifyDate;
    }

    public Integer getPrevalence() {
        return prevalence;
    }

    public void setPrevalence(Integer prevalence) {
        this.prevalence = prevalence;
    }

    public List<String> getCallbackWords() {
        return callbackWords;
    }

    public void setCallbackWords(List<String> callbackWords) {
        this.callbackWords = callbackWords;
    }
}
