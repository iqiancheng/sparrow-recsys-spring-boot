package com.sparrowrecsys.online.service;

import com.sparrowrecsys.online.datamanager.Movie;
import com.sparrowrecsys.online.datamanager.RedisClient;
import com.sparrowrecsys.online.datamanager.User;
import com.sparrowrecsys.online.util.Config;
import com.sparrowrecsys.online.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.sparrowrecsys.online.util.HttpClient.asyncSinglePostRequest;

/**
 * Recommendation process of similar movies
 */
@Service
@Slf4j
public class RecForYouService {

    @Resource
    private DataManager dataManager;

    /**
     * function to calculate similarity score based on embedding
     *
     * @param user      input user
     * @param candidate candidate movie
     * @return similarity score
     */
    public static double calculateEmbSimilarScore(User user, Movie candidate) {
        if (null == user || null == candidate || null == user.getEmb()) {
            return -1;
        }
        return user.getEmb().calculateSimilarity(candidate.getEmb());
    }

    /**
     * get recommendation movie list
     *
     * @param userId input user id
     * @param size   size of similar items
     * @param model  model used for calculating similarity
     * @return list of similar movies
     */
    public List<Movie> getRecList(int userId, int size, String model) {
        User user = dataManager.getUserById(userId);
        if (null == user) {
            return new ArrayList<>();
        }

        /** 召回数据，采用了sql的方式，按照排名高低召回800部电影
         * 召回没有使用任何模型
         * TODO：与重排一样，自己调研并实现
         */
        final int CANDIDATE_SIZE = 800;
        List<Movie> candidates = dataManager.getMovies(CANDIDATE_SIZE, "rating");

        //load user emb from redis if data source is redis
        if (Config.EMB_DATA_SOURCE.equals(Config.DATA_SOURCE_REDIS)) {
            String userEmbKey = "uEmb:" + userId;
            String userEmb = RedisClient.getInstance().get(userEmbKey);
            if (null != userEmb) {
                user.setEmb(Utility.parseEmbStr(userEmb));
            }
        }

        if (Config.IS_LOAD_USER_FEATURE_FROM_REDIS) {
            String userFeaturesKey = "uf:" + userId;
            Map<String, String> userFeatures = RedisClient.getInstance().hgetAll(userFeaturesKey);
            if (null != userFeatures) {
                user.setUserFeatures(userFeatures);
            }
        }

        /** 排序数据，对召回的大量数据进一步排序，找出与用户喜好更为接近的数据
         * 这个部分使用了2个模型：
         * ①emb模型，只是计算电影和用户的特征向量的相似度，相似度高的排名高
         * ②neuralcf模型，需要本地将tensorflow的模型服务启动起来，会给每个电影打分
         */
        List<Movie> rankedList = ranker(user, candidates, model);

        /** 截取Top N 数据返回给用户
         * 重排需要提升用户的多样性体验，精排大部分时候返回的是比较优质但重复的内容
         * 重排没有使用任何模型，常用的是point wise、pair wise、list wise
         * 还有1种MMR算法（最大边缘相关算法），用来增强多样性的，可以研究一下
         * 重排、混排的目的是类似的：为了防止精排越推越窄
         * TODO：理解清楚3种模型，看怎么用python编写1个模型的实现
         */
        if (rankedList.size() > size) {
            return rankedList.subList(0, size);
        }
        return rankedList;
    }

    /**
     * rank candidates
     *
     * @param user       input user
     * @param candidates movie candidates
     * @param model      model name used for ranking
     * @return ranked movie list
     */
    public List<Movie> ranker(User user, List<Movie> candidates, String model) {
        HashMap<Movie, Double> candidateScoreMap = new HashMap<>();

        /**
         * 策略模式，使用特定模型
         */
        switch (model) {
            case "emb":
                for (Movie candidate : candidates) {
                    double similarity = calculateEmbSimilarScore(user, candidate);
                    candidateScoreMap.put(candidate, similarity);
                }
                break;
            case "neuralcf":
                callNeuralCFTFServing(user, candidates, candidateScoreMap);
                break;
            default:
                //default ranking in candidate set
                for (int i = 0; i < candidates.size(); i++) {
                    candidateScoreMap.put(candidates.get(i), (double) (candidates.size() - i));
                }
        }

        List<Movie> rankedList = new ArrayList<>();
        // 通过
        candidateScoreMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEach(m -> rankedList.add(m.getKey()));
        return rankedList;
    }

    /**
     * 使用point wise排序，将用户和电影的2元组丢入模型，预测出用户对电影的打分
     * ，然后对于按照预测出的分数高低进行排序，将分数高的返回给用户
     * <p>
     * call TenserFlow serving to get the NeuralCF model inference result
     *
     * @param user              input user
     * @param candidates        candidate movies
     * @param candidateScoreMap save prediction score into the score map
     */
    public void callNeuralCFTFServing(User user, List<Movie> candidates, HashMap<Movie, Double> candidateScoreMap) {
        if (null == user || null == candidates || candidates.size() == 0) {
            return;
        }

        JSONArray instances = new JSONArray();
        for (Movie m : candidates) {
            JSONObject instance = new JSONObject();
            instance.put("userId", user.getUserId());
            instance.put("movieId", m.getMovieId());
            instances.put(instance);
        }

        JSONObject instancesRoot = new JSONObject();
        instancesRoot.put("instances", instances);

        //need to confirm the tf serving end point
        String predictionScores = asyncSinglePostRequest("http://localhost:8501/v1/models/recmodel:predict", instancesRoot.toString());
        log.info("send user" + user.getUserId() + " request to tf serving.");

        JSONObject predictionsObject = new JSONObject(predictionScores);
        JSONArray scores = predictionsObject.getJSONArray("predictions");
        for (int i = 0; i < candidates.size(); i++) {
            candidateScoreMap.put(candidates.get(i), scores.getJSONArray(i).getDouble(0));
        }
    }
}
