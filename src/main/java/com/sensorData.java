package com;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 24.04.2018.
 */
public class sensorData {
    String UID;
    List<topicData> TOPIC_LIST;

    public sensorData(String uid){
        UID = uid;
        TOPIC_LIST = new ArrayList<>();
    }

    public void addTopic(String measureType, String topicName){
        TOPIC_LIST.add(new topicData(measureType,topicName));
    }

    public topicData getTopicByType(String mesureType){
        topicData reqTopic = null;
        for (topicData it : TOPIC_LIST){
            if (mesureType.equals(it.MEASURE_TYPE)){
                reqTopic = it;
            }
        }
        return reqTopic;
    }
}
