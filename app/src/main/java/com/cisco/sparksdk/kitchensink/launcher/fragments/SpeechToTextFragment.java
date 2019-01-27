package com.cisco.sparksdk.kitchensink.launcher.fragments;



import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.sparksdk.kitchensink.ui.BaseFragment;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SpeechToTextFragment extends BaseFragment {

    private final String API_KEY = "RKJZUgL5ymmvsvsdpDbBZECs1ipzUbL11Os6mBmAK8rc";
    private String audioUrl;

    IamOptions options = new IamOptions.Builder()
            .apiKey(API_KEY)
            .build();


    public SpeechToTextFragment() {
        // Required empty public constructor
    }

    void startAPI(String filename, String[] keywords) {

        IamOptions options = new IamOptions.Builder()
                .apiKey(API_KEY)
                .build();

        SpeechToText speechToText = new SpeechToText();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            audioUrl = savedInstanceState.getString("audioUrl");
        }

        if(audioUrl != null){
            startAPI(audioUrl, null);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

}
