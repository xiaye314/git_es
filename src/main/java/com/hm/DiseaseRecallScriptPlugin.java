package com.hm;

import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DiseaseRecallScriptPlugin extends Plugin implements ScriptPlugin {
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
            return "diseaseRecall";
        }
    }


    public static class MyNativeScript extends AbstractSearchScript {
        Map<String, Object> params;

        public MyNativeScript(Map<String, Object> params) {
            this.params = params;
        }

        @Override
        public Object run() {
            Map<Integer, Double> likelihoodMap = new HashMap();
            likelihoodMap.put(1, 0.5);
            likelihoodMap.put(2, 1.0);
            likelihoodMap.put(3, 10000.0);
            likelihoodMap.put(4, 10000.0);
            likelihoodMap.put(5, 10000.0);
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
            List<Integer> userNoneSymptomList = scoreParams.getUserNoneSymptom();
            if (userNoneSymptomList == null) userNoneSymptomList = new ArrayList();
            Set<Long> userNoneSymptomSet = new HashSet();
            for (Integer userNoneSymptomId : userNoneSymptomList) {
                userNoneSymptomSet.add(Long.valueOf(userNoneSymptomId));
            }
            List<String> symptomList = source.getSymptomList();

            Map<String, Map<String, String>> diseaseSymptomMap = new HashMap();
            for (String symptomStr : symptomList) {
                Map<String, String> map = new HashMap();
                String[] symptom = symptomStr.split("\\|");
                if (symptom.length < 4) continue;
                map.put("id", symptom[0]);
                map.put("group", symptom[1]);
                map.put("relevance", symptom[2]);
                map.put("symptomDiseaseRate", symptom[3]);
                diseaseSymptomMap.put(symptom[0], map);
                if (("4".equals(symptom[2]) || "5".equals(symptom[2])) && userNoneSymptomSet.contains(Long.parseLong(symptom[0]))) {
                    return 0;
                }
            }

            List<String> userSymptomList = scoreParams.getUserSymptom();
            if (userSymptomList == null) userSymptomList = new ArrayList();
            Map<String, Double> userSymptomMap = new HashMap();
            for (String userSymptom : userSymptomList) {
                String[] symptom = userSymptom.split(":");
                if (symptom.length < 2) continue;
                userSymptomMap.put(symptom[0], Double.parseDouble(symptom[1]));
            }
            List<String> symptomGroupList = scoreParams.getSymptomGroup();
            Map<String, Set<String>> recognitionSynonymMap = new HashMap();
            List<Set<String>> synonymGroup = new ArrayList();
            Set synonymSymptomSet = new HashSet();
            for (String symptomGroup : symptomGroupList) {
                String[] symptomPair = symptomGroup.split(":");
                if (symptomPair.length < 2) continue;
                String symptomId = symptomPair[0];
                String[] list = symptomPair[1].split(",");
                if ("".equals(symptomId)) {
                    Set<String> set = new HashSet<>();
                    set.addAll(Arrays.asList());
                    synonymGroup.add(set);
                } else {
                    recognitionSynonymMap.put(symptomId, new HashSet(Arrays.asList(list)));
                }
                synonymSymptomSet.add(symptomId);
                for (String str : list) {
                    synonymSymptomSet.add(str);
                }
            }

            for (String str : userSymptomMap.keySet()) {
                if (!synonymSymptomSet.contains(str)) {
                    recognitionSynonymMap.put(str, new HashSet());
                }
            }


            Map debugResult = new HashMap();

            double mathcedSymptomLikelihood = 1.0;
            StringBuilder sb = new StringBuilder();
            sb.append(mathcedSymptomLikelihood);

            Map<String, Set<String>> unmatchedRecognitionSynonymMap = new HashMap();
            List<Set<String>> unmatchedSynonymGroup = new ArrayList();
            Set diseaseGroupSet = new HashSet();
            Map<String, Set<String>> temp = new HashMap();

            boolean noHighRe = true;
            double lowMatchedScore = 0.0;

            for (String str : recognitionSynonymMap.keySet()) {
                if (diseaseSymptomMap.containsKey(str)) {
                    Map<String, String> symptom = diseaseSymptomMap.get(str);
                    String relevanceStr = symptom.get("relevance");
                    int relevance = relevanceStr == null || relevanceStr.equals("") ? 0 : Integer.parseInt(relevanceStr);
                    if (!userSymptomMap.containsKey(str)) continue;
                    double symptomDiseaseRate = userSymptomMap.get(str);
                    if (!diseaseGroupSet.contains(symptom.get("group")) && relevance > 1) {
                        noHighRe = false;
                        diseaseGroupSet.add(symptom.get("group"));
                    } else {
                        temp.put(str, recognitionSynonymMap.get(str));
                        continue;
                    }
                    mathcedSymptomLikelihood = mathcedSymptomLikelihood * likelihoodMap.get(relevance);
                    sb.append("*").append(likelihoodMap.get(relevance)).append("(").append(str).append(")");
                    double rate = Math.pow(symptomDiseaseRate, 0.05);
                    mathcedSymptomLikelihood = mathcedSymptomLikelihood * rate;
                    sb.append("*").append(rate);
                } else {
                    temp.put(str, recognitionSynonymMap.get(str));
                }
            }

            for (String str : temp.keySet()) {
                if (contains(diseaseSymptomMap.keySet(), temp.get(str))) {
                    Map<String, String> symptom = null;
                    for (String str1 : temp.get(str)) {
                        if (diseaseSymptomMap.containsKey(str1)) {
                            symptom = diseaseSymptomMap.get(str1);
                        }
                    }
                    String relevanceStr = symptom.get("relevance");
                    int relevance = relevanceStr == null || relevanceStr.equals("") ? 0 : Integer.parseInt(relevanceStr);
                    if (!userSymptomMap.containsKey(str)) continue;
                    double symptomDiseaseRate = userSymptomMap.get(str);
                    if (!diseaseGroupSet.contains(symptom.get("group")) && relevance > 1) {
                        noHighRe = false;
                        diseaseGroupSet.add(symptom.get("group"));
                    } else {
                        unmatchedRecognitionSynonymMap.put(str, recognitionSynonymMap.get(str));
                        continue;
                    }
                    symptomDiseaseRate = symptomDiseaseRate * 0.9;
                    mathcedSymptomLikelihood = mathcedSymptomLikelihood * likelihoodMap.get(relevance);
                    sb.append("*").append(likelihoodMap.get(relevance)).append("(").append(str).append(")");
                    double rate = Math.pow(symptomDiseaseRate, 0.05);
                    mathcedSymptomLikelihood = mathcedSymptomLikelihood * rate;
                    sb.append("*").append(rate);
                } else {
                    unmatchedRecognitionSynonymMap.put(str, recognitionSynonymMap.get(str));
                }
            }
            for (Set<String> groupSet : synonymGroup) {
                if (!contains(diseaseSymptomMap.keySet(), groupSet)) {
                    unmatchedSynonymGroup.add(groupSet);
                } else {
                    for (String str : groupSet) {
                        if (diseaseSymptomMap.containsKey(str)) {
                            Map<String, String> symptom = diseaseSymptomMap.get(str);
                            String relevanceStr = symptom.get("relevance");
                            int relevance = relevanceStr == null || relevanceStr.equals("") ? 0 : Integer.parseInt(relevanceStr);
                            if (!diseaseGroupSet.contains(symptom.get("group")) && relevance > 1) {
                                noHighRe = false;
                                diseaseGroupSet.add(symptom.get("group"));
                            } else {
                                continue;
                            }
                            if (!userSymptomMap.containsKey(str)) continue;
                            double symptomDiseaseRate = userSymptomMap.get(str) * 0.9;
                            mathcedSymptomLikelihood = mathcedSymptomLikelihood * likelihoodMap.get(relevance);
                            sb.append("*").append(likelihoodMap.get(relevance)).append("(").append(str).append(")");
                            double rate = Math.pow(symptomDiseaseRate, 0.05);
                            mathcedSymptomLikelihood = mathcedSymptomLikelihood * rate;
                            sb.append("*").append(rate);
                            break;
                        }
                    }
                }
            }

            if (noHighRe) {
                unmatchedRecognitionSynonymMap.clear();
                unmatchedSynonymGroup.clear();
                for (String str : recognitionSynonymMap.keySet()) {
                    Set<String> synonymGroupSet = new HashSet(recognitionSynonymMap.get(str));
                    synonymGroupSet.add(str);
                    if (!contains(diseaseSymptomMap.keySet(), synonymGroupSet)) {
                        unmatchedRecognitionSynonymMap.put(str, recognitionSynonymMap.get(str));
                    } else {
                        for (String str1 : synonymGroupSet) {
                            if (diseaseSymptomMap.containsKey(str1)) {
                                if (!userSymptomMap.containsKey(str1)) continue;
                                double symptomDiseaseRate = userSymptomMap.get(str1);
                                lowMatchedScore += symptomDiseaseRate;
                                break;
                            }
                        }
                    }
                }

                for (Set<String> groupSet : synonymGroup) {
                    if (!contains(diseaseSymptomMap.keySet(), groupSet)) {
                        unmatchedSynonymGroup.add(groupSet);
                    } else {
                        for (String str1 : groupSet) {
                            if (diseaseSymptomMap.containsKey(str1)) {
                                if (!userSymptomMap.containsKey(str1)) continue;
                                double symptomDiseaseRate = userSymptomMap.get(str1);
                                lowMatchedScore += symptomDiseaseRate;
                                break;
                            }
                        }
                    }
                }
            }

            double unmatchedSymptomLikelihood = 1.0;
            StringBuilder sb1 = new StringBuilder();
            sb1.append(unmatchedSymptomLikelihood);
            for (String str : unmatchedRecognitionSynonymMap.keySet()) {
                if (!userSymptomMap.containsKey(str)) continue;
                double symptomDiseaseRate = userSymptomMap.get(str);
                unmatchedSymptomLikelihood = unmatchedSymptomLikelihood * symptomDiseaseRate;
                sb1.append("*").append(symptomDiseaseRate).append("(").append(str).append(")");
            }
            for (Set groupSet : unmatchedSynonymGroup) {
                if (groupSet.size() > 0) {
                    if (!userSymptomMap.containsKey(groupSet.toArray()[0])) continue;
                    double symptomDiseaseRate = userSymptomMap.get(groupSet.toArray()[0]);
                    unmatchedSymptomLikelihood = unmatchedSymptomLikelihood * symptomDiseaseRate;
                    sb1.append("*").append(symptomDiseaseRate);
                }
            }

            String season = calcSeason();
            int diseaseCount = 0;
            int totalCnt = 0;
            Integer gender = scoreParams.getGender();
            Double age = scoreParams.getAge();
            if (gender != null && age != null) {
                String ageGroup = calcAgeGroup(age);
                List<String> distGroupList = source.getDistGroup();
                for (String distGroup : distGroupList) {

                    String[] groupList = distGroup.split("\\|");
                    if (ageGroup.equals(groupList[0]) && groupList[1].equals(String.valueOf(gender))
                            && season.equals(groupList[2])) {
                        diseaseCount = Integer.parseInt(groupList[3]);
                        totalCnt = Integer.parseInt(groupList[4]);
                        break;
                    }
                }
            } else if (gender != null && gender > 0) {
                List<String> distGroupByGenderSeasonList = source.getDistGroupByGenderSeason();
                for (String distGroupByGenderSeason : distGroupByGenderSeasonList) {
                    String[] groupList = distGroupByGenderSeason.split("\\|");
                    if (groupList[1].equals(String.valueOf(gender))
                            && season.equals(groupList[2])) {
                        diseaseCount = Integer.parseInt(groupList[3]);
                        totalCnt = Integer.parseInt(groupList[4]);
                        break;
                    }
                }
            } else if (age != null && age > 0) {
                String ageGroup = calcAgeGroup(age);
                List<String> distGroupByAgeGroupSeasonList = source.getDistGroupByAgeGroupSeason();
                for (String distGroupByAgeGroupSeason : distGroupByAgeGroupSeasonList) {
                    String[] groupList = distGroupByAgeGroupSeason.split("\\|");
                    if (ageGroup.equals(groupList[0]) && season.equals(groupList[2])) {
                        diseaseCount = Integer.parseInt(groupList[3]);
                        totalCnt = Integer.parseInt(groupList[4]);
                        break;
                    }
                }
            } else {
                List<String> distGroupBySeasonList = source.getDistGroupBySeason();
                for (String distGroupBySeason : distGroupBySeasonList) {
                    String[] groupList = distGroupBySeason.split("\\|");
                    if (season.equals(groupList[2])) {
                        diseaseCount = Integer.parseInt(groupList[3]);
                        totalCnt = Integer.parseInt(groupList[4]);
                        break;
                    }
                }
            }
            Integer isCritical = source.getIsCritical();
            double priorProb = getPriorProbOfDisease(diseaseCount, totalCnt, isCritical);
            double score = priorProb * mathcedSymptomLikelihood * unmatchedSymptomLikelihood;

            if (noHighRe) {
                score = -1 / (priorProb * lowMatchedScore);
            }
            debugResult.put("diseaseCount", diseaseCount);
            debugResult.put("totalCnt", totalCnt);
            debugResult.put("priorProb", priorProb);
            debugResult.put("mathcedSymptomLikelihood", mathcedSymptomLikelihood);
            debugResult.put("unmatchedSymptomLikelihood", unmatchedSymptomLikelihood);
            debugResult.put("matchedScore", sb.toString());
            debugResult.put("unmatchedScore", sb1.toString());
            debugResult.put("score", score);
            boolean debug = scoreParams.getDebug();
            if (debug) {
                // 打印分数
                return debugResult;
//                System.out.println(debugResult);
            }
            return score;
        }
    }

    static String calcSeason() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String str = df.format(date).substring(4, 6);
        String season = "";
        if (str.equals("12") || str.equals("01") || str.equals("02")) {
            season = "12-2";
        } else if (str.equals("03") || str.equals("04") || str.equals("05")) {
            season = "3-5";
        } else if (str.equals("06") || str.equals("07") || str.equals("08")) {
            season = "6-8";
        } else {
            season = "9-11";
        }
        return season;
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


    double getPriorProbOfDisease(Integer freq) {
        double priorProb = 0.001;
        if (freq >= 1000)
            priorProb *= 100;
        else if (freq >= 100)
            priorProb *= 100;
        else if (freq >= 10)
            priorProb *= 20;
        else if (freq >= 5)
            priorProb *= 10;
        return priorProb;
    }

    static double getPriorProbOfDisease(Integer diseaseCount, Integer totalCnt, Integer isCritical) {
        double priorProb = 0.0001;
        int diseaseCntFactor = 500;
        int totalCntFactor = 50000;
        //if (isCritical == 1) diseaseCntFactor = 200;
        priorProb = (diseaseCount + diseaseCntFactor) * 1.0 / (totalCnt + totalCntFactor);
        return priorProb;
    }

    static boolean contains(Set set1, Set set2) {
        for (Object o : set2) {
            if (set1.contains(o)) {
                return true;
            }
        }
        return false;
    }


}