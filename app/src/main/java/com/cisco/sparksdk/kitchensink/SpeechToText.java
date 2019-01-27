package com.cisco.sparksdk.kitchensink;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class SpeechToText {

    private final String API_KEY = "RKJZUgL5ymmvsvsdpDbBZECs1ipzUbL11Os6mBmAK8rc";
    private String audioUrl;

    public SpeechToText(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    IamOptions options = new IamOptions.Builder()
            .apiKey(API_KEY)
            .build();

    public void startAPI(String filename, String[] keywords) {

        IamOptions options = new IamOptions.Builder()
                .apiKey(API_KEY)
                .build();

        com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText speechToText = new com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText();
        speechToText.setEndPoint("https://gateway-lon.watsonplatform.net/speech-to-text/api");

        String audioType = audioUrl + ".mpeg4";
        try {
            List<String> files = Arrays.asList(filename);
            for (String file : files) {
                RecognizeOptions recognizeOptions = new RecognizeOptions.Builder()
                        .audio(new File(file))
                        .contentType(audioType)
//                        .timestamps(true)
//                        .wordAlternativesThreshold((float) 0.9)
//                        .keywords(Arrays.asList("colorado", "tornado", "tornadoes"))
//                        .keywordsThreshold((float) 0.5)
                        .build();

                SpeechRecognitionResults speechRecognitionResults =
                        speechToText.recognize(recognizeOptions).execute();
                System.out.println(speechRecognitionResults);
//                speechRecognitionResults.getResults();
//                System.out.println(speechRecognitionResults.getResults().get(0));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
