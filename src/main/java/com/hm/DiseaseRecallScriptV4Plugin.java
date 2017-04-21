package com.hm;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import com.alibaba.fastjson.JSON;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by wangjialin on 17/3/13.
 */
public class DiseaseRecallScriptV4Plugin extends Plugin implements ScriptPlugin {
    public List<NativeScriptFactory> getNativeScripts() {
        return Collections.singletonList(new MyNativeScriptFactory());
    }

    public static class MyNativeScriptFactory implements NativeScriptFactory {
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            return new MyNativeScript(params);
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        @Override
        public String getName() {
            return "diseaseRecallV4";
        }
    }


    public static class MyNativeScript extends AbstractSearchScript {
        Map<String, Object> params;

        public MyNativeScript(Map<String, Object> params) {
            this.params =  params;
        }

        @Override
        public Object run() {
            ScoreParams scoreParams = new ScoreParams();
            try {
                BeanUtils.populate(scoreParams, params);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            DiseaseDoc source = new DiseaseDoc();
            try {
                if (!source().isEmpty()) {

                    Map<String, Object> sourceMap = source().source();
                    // 将source 的map对象转为bean
                    BeanUtils.populate(source, source().source());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            //性别限制 疾病性别-1女 1男. 用户性别 0女 1男
            //source表示的是召回的疾病信息，scoreParams表示医生输入的患者病例信息
            Integer diseaseGender = source.getGender();//召回疾病中的疾病性别
            if (diseaseGender == null) diseaseGender = 0;
            Integer userGender = scoreParams.getGender();//医生输入病人病例中的病人性别
            if (userGender == null) userGender = -2;
            if ((diseaseGender == -1 && userGender == 1) || (diseaseGender == 1 && userGender == 0)){
                return 0.0;//如果疾病性别和患者性别不匹配就排除该疾病
            }

            //疾病诊断因子列表
            List<String> symptomList = source.getSymptomList();
            if (symptomList == null) symptomList = new ArrayList<>();
            Map<String, DiseaseSymptomMapping> diseaseSymptomMap = new HashMap();

            //必要条件诊断因子集合
            //symptomList中的字段：symptomId，group，relevance，symptomSpecificity，symptomType，requirement，crowdRelevance "1|1|2|1|0|0|"
            Set<Integer> requirementSymptom = new HashSet<>();
            for (String symptomStr : symptomList) {
                DiseaseSymptomMapping diseaseSymptomMapping = new DiseaseSymptomMapping(symptomStr);
                diseaseSymptomMap.put(diseaseSymptomMapping.getSymptomId(), diseaseSymptomMapping);
                if ("1".equals(diseaseSymptomMapping.getRequirement())){
                    requirementSymptom.add(Integer.parseInt(diseaseSymptomMapping.getSymptomId()));
                }
            }

            //用户症状
            List<String> userSymptomList = scoreParams.getUserSymptom();// "userSymptom" : ["14905:9.165902841429881E-4",]
            if (userSymptomList == null) userSymptomList = new ArrayList();
            List<String> userSymptomIdList = new ArrayList<>();
            for (String userSymptom : userSymptomList) {
                String[] symptom = userSymptom.split(":");
                if (symptom.length < 2) continue;
                userSymptomIdList.add(symptom[0]);
            }
            //阴性症状中包含必要条件的不推出
            List<Integer> userNoneSymptom = scoreParams.getUserNoneSymptom();
            if (userNoneSymptom != null && userNoneSymptom.size() > 0 && requirementSymptom.size() > 0){//存在阴性症状和必要条件
                for (Integer symptomId : requirementSymptom){
                    if (userNoneSymptom.contains(symptomId)){
                        return 0;
                    }
                }
            }

            //识别症状同义词组
            List<String> synonymGroupList = scoreParams.getSymptomGroup();// "symptomGroup" : ["126:11512","162:9324","6707:","40:14905,4050,6993"],
            if (synonymGroupList == null) synonymGroupList = new ArrayList<>();
            Map<String, Set<String>> recognitionSynonymMap = new HashMap();
            Set synonymSymptomSet = new HashSet();
            List<Set<String>> noRecognitionSynonymList = new ArrayList<>();
            for (String synonymGroup : synonymGroupList) {
                String[] symptomPair = synonymGroup.split(":");
                if (symptomPair.length < 2) continue;
                String symptomId = symptomPair[0];
                String[] list = symptomPair[1].split(",");
                if ("".equals(symptomId)) {
                    Set<String> set = new HashSet<>();
                    set.addAll(Arrays.asList(list));
                    noRecognitionSynonymList.add(set);
                } else {
                    recognitionSynonymMap.put(symptomId, new HashSet(Arrays.asList(list)));
                    synonymSymptomSet.add(symptomId);
                }
                for (String str : list) {
                    synonymSymptomSet.add(str);
                }
            }

            for (String str : userSymptomIdList) {
                if (!synonymSymptomSet.contains(str)) {
                    recognitionSynonymMap.put(str, new HashSet());
                }
            }

            //各诊断因子得分
            Map<String, Double> symptomScore = new HashMap<>();
            Map<String,StringBuilder> diseaseCalculationFormula = new HashMap<>();
            for(String symptomId : userSymptomIdList){
                StringBuilder sb = new StringBuilder();
                DiseaseSymptomMapping diseaseSymptomMapping = diseaseSymptomMap.get(symptomId);
                if(diseaseSymptomMapping != null){
                    double matchScore = getSymptomSpecificityScore(diseaseSymptomMapping.getSymptomSpecificity()) *
                            getDiseaseSymptomRelevanceScore(diseaseSymptomMapping.getRelevance(), diseaseSymptomMapping.getCrowdRelevance(), scoreParams.getAge(), scoreParams.getGender());
                    sb.append(getSymptomSpecificityScore(diseaseSymptomMapping.getSymptomSpecificity()));
                    sb.append("*").append(getDiseaseSymptomRelevanceScore(diseaseSymptomMapping.getRelevance(), diseaseSymptomMapping.getCrowdRelevance(), scoreParams.getAge(), scoreParams.getGender()));
                    if (!recognitionSynonymMap.keySet().contains(symptomId)) {
                        if(diseaseSymptomMapping.getRelevance().equals("5")){
                            matchScore = matchScore * 0.19;
                            sb.append(",bySynonym:").append("*0.19");
                        } else if (diseaseSymptomMapping.getRelevance().equals("4")){
                            matchScore = matchScore * 0.88;
                            sb.append(",bySynonym:").append("*0.88");
                        } else {
                            matchScore = matchScore * 0.9;
                            sb.append(",bySynonym:").append("*0.9");
                        }
                    }
                    symptomScore.put(symptomId, matchScore);
                    diseaseCalculationFormula.put(symptomId, sb);
                }
            }

            Set<String> matchSymptom = new HashSet<>();
            //同义词组匹配项计算
            for (String symptomId : recognitionSynonymMap.keySet()){
                if (symptomScore.containsKey(symptomId)){
                    matchSymptom.add(symptomId);
                } else {
                    double maxScore = 0.0;
                    String maxScoreSymptom = "";
                    for (String symptomId2 : recognitionSynonymMap.get(symptomId)){
                        if (!symptomScore.containsKey(symptomId2)){
                            continue;
                        }
                        if (symptomScore.get(symptomId2) > maxScore){
                            maxScore = symptomScore.get(symptomId2);
                            maxScoreSymptom = symptomId2;
                        }
                    }
                    if (!StringUtils.isBlank(maxScoreSymptom)) {
                        matchSymptom.add(maxScoreSymptom);
                    }
                }
            }
            for (Set<String> noRecognitionSynonym : noRecognitionSynonymList){
                double maxScore = 0.0;
                String maxScoreSymptom = "";
                for (String symptomId : noRecognitionSynonym){
                    if (!symptomScore.containsKey(symptomId)){
                        continue;
                    }
                    if (symptomScore.get(symptomId) > maxScore){
                        maxScore = symptomScore.get(symptomId);
                        maxScoreSymptom = symptomId;
                    }
                }
                if (!StringUtils.isBlank(maxScoreSymptom)){
                    matchSymptom.add(maxScoreSymptom);
                }
            }

            //同组疾病中取得分点取最 大分数
            Map<String, String> groupMap = new HashMap<>();
            for (String symptomId : matchSymptom){
                String group  ="";
                if(diseaseSymptomMap.get(symptomId)!=null)
                    group  = diseaseSymptomMap.get(symptomId).getGroup();
                if (groupMap.containsKey(group)){
                    double score1 = symptomScore.get(groupMap.get(group));
                    double score2 = symptomScore.get(symptomId);
                    if (score2 > score1) {
                        groupMap.put(group, symptomId);
                    }
                } else {
                    groupMap.put(group, symptomId);
                }
            }
            matchSymptom = new HashSet<>(groupMap.values());

            double score = 1.0;

            StringBuilder resultCalculationFormual = new StringBuilder();
            for (String symptom : matchSymptom){
                Double temScore=symptomScore.get(symptom);
                if(temScore!=null && temScore != 0.0){
                    score *= temScore;
                    resultCalculationFormual.append(symptom).append(":").append(diseaseCalculationFormula.get(symptom)).append(";");
                }
            }

            //只有召回词匹配的病历算分
            Set<String> callbackWords = new HashSet<>();
            if (!StringUtils.isEmpty(scoreParams.getCallbackWords())){
                String[] temps = StringUtils.split(scoreParams.getCallbackWords(), ',');
                callbackWords.addAll(Arrays.asList(temps));
            }
            if (score == 1.0 && source.getCallbackWords() != null){
                Integer matchNum = 0;
                for (String str : source.getCallbackWords()){
                    if (callbackWords.contains(str)){
                        matchNum++;
                    }
                }
                if (matchNum > 0) {
                    score = -1 / matchNum;
                    resultCalculationFormual.append("CallbackWordsOnly:").append("-1").append("/").append(matchNum);
                }
            }

            if (score == 1.0){
                score = 0.0;
            } else {
                score = score * getDiseasePrevalenceScore(source.getPrevalence());
                resultCalculationFormual.append(getDiseasePrevalenceScore(source.getPrevalence()));

            }

            Map debugResult = new HashMap();
            Map<String, DiseaseSymptomMapping> diseaseSymptomMapNew = new HashMap<>();
            for (String symptom : matchSymptom){
                diseaseSymptomMapNew.put(symptom, diseaseSymptomMap.get(symptom));
            }
            debugResult.put("diseaseSymptom",JSON.toJSONString(diseaseSymptomMapNew));
            debugResult.put("symptomScore",symptomScore);
            debugResult.put("mathchSymptom", matchSymptom);
            debugResult.put("prevalence", source.getPrevalence());
            debugResult.put("prevalenceScore", getDiseasePrevalenceScore(source.getPrevalence()));
            debugResult.put("symptomidSpecifityRelevanceListAndPrevalence", resultCalculationFormual);
            debugResult.put("score", score);
            debugResult.put("age", scoreParams.getAge());
            debugResult.put("gender", scoreParams.getGender());
            boolean debug = scoreParams.getDebug();
            if (debug) {
                // 打印分数
                return debugResult;
//                System.out.println(debugResult);
            }
            return score;
        }
    }


    static String calcAgeGroup(Double age) {
        String ageGroup = "";
        if (age <= 6) {
            ageGroup = "0-6";
        } else if (age <= 12 && age > 6) {
            ageGroup = "7-12";
        } else if (age <= 18 && age > 12) {
            ageGroup = "13-18";
        } else if (age <= 50 && age > 18) {
            ageGroup = "19-50";
        } else if (age <= 80 && age > 50) {
            ageGroup = "51-80";
        } else {
            ageGroup = "81-100";
        }
        return ageGroup;
    }

    static double getSymptomSpecificityScore(String specificity){
        /*if (StringUtils.isEmpty(specificity) || "0".equals(specificity)){
            return 1.0;
        } else {
            return Math.pow(Double.parseDouble(specificity), 0.5);
        }*/
        return 1.0;
    }

    static double getDiseasePrevalenceScore(Integer prevalence){
        Map<Integer, Double> prevalenceScore = new HashMap<>();
        prevalenceScore.put(1, 0.1);
        prevalenceScore.put(2, 1.0);
        prevalenceScore.put(3, 3.0);
        prevalenceScore.put(4, 4.0);
        prevalenceScore.put(5, 5.0);
        prevalenceScore.put(6, 6.0);
        if (prevalenceScore.containsKey(prevalence)) {
            return prevalenceScore.get(prevalence);
        } else {
            return 0.1;
        }
    }

    static double getDiseaseSymptomRelevanceScore(String relevance, String crowdRelevance, Double age, Integer gender){
        if (!StringUtils.isEmpty(crowdRelevance)){
            String[] strings = crowdRelevance.split(",");
            if (strings.length == 4){
                String crowdGender = "";
                if (!StringUtils.isEmpty(strings[0]) && !"-1".equals(strings[0])){
                    crowdGender = strings[0];
                }
                Double minAge = 0.0;
                if (!StringUtils.isEmpty(strings[1])){
                    minAge = Double.parseDouble(strings[1]) * 360;
                }
                Double maxAge = Double.MAX_VALUE;
                if (!StringUtils.isEmpty(strings[2])){
                    maxAge = Double.parseDouble(strings[2]) * 360;
                }
                if (!StringUtils.isEmpty(crowdGender) && (gender == null || !crowdGender.equals(gender.toString()))){
                    return 0.0;
                }
                if ((minAge > 0.0 || maxAge < 540000) && (age == null || age < minAge || age > maxAge)){
                    return 0.0;
                }
            }
        }
        Map<String, Double> relevanceScore = new HashMap<>();
        relevanceScore.put("1", 1.1);
        relevanceScore.put("2", 2.0);
        relevanceScore.put("3", 3.3);
        relevanceScore.put("4", 4.08);
        relevanceScore.put("5", 20.0);
        if (relevanceScore.containsKey(relevance)){
            return relevanceScore.get(relevance);
        } else {
            return 0.0;
        }
    }
}
