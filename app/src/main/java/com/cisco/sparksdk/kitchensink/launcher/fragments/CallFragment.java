/*
 * Copyright 2016-2017 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.cisco.sparksdk.kitchensink.launcher.fragments;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import com.cisco.sparksdk.kitchensink.R;
import com.cisco.sparksdk.kitchensink.SpeechToText;
import com.cisco.sparksdk.kitchensink.actions.SparkAgent;
import com.cisco.sparksdk.kitchensink.actions.commands.AddCallHistoryAction;
import com.cisco.sparksdk.kitchensink.actions.commands.RequirePermissionAction;
import com.cisco.sparksdk.kitchensink.actions.commands.toggleSpeakerAction;
import com.cisco.sparksdk.kitchensink.actions.events.AnswerEvent;
import com.cisco.sparksdk.kitchensink.actions.events.DialEvent;
import com.cisco.sparksdk.kitchensink.actions.events.HangupEvent;
import com.cisco.sparksdk.kitchensink.actions.events.OnConnectEvent;
import com.cisco.sparksdk.kitchensink.actions.events.OnDisconnectEvent;
import com.cisco.sparksdk.kitchensink.actions.events.OnMediaChangeEvent;
import com.cisco.sparksdk.kitchensink.actions.events.OnRingingEvent;
import com.cisco.sparksdk.kitchensink.actions.events.PermissionAcquiredEvent;
import com.cisco.sparksdk.kitchensink.launcher.LauncherActivity;
import com.cisco.sparksdk.kitchensink.ui.BaseFragment;
import com.cisco.sparksdk.kitchensink.ui.FullScreenSwitcher;
import com.ciscospark.androidsdk.phone.CallObserver;
import com.ciscospark.androidsdk.phone.internal.PhoneImpl;
import com.github.benoitdion.ln.Ln;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Random;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.ciscospark.androidsdk.phone.CallObserver.RemoteSendingSharingEvent;
import static com.ciscospark.androidsdk.phone.CallObserver.SendingSharingEvent;
/**
 * A simple {@link BaseFragment} subclass.
 */
public class CallFragment extends BaseFragment {
    protected static final int MEDIA_PROJECTION_REQUEST = 2;
    private static final String CALLEE = "callee";
    private static final String INCOMING_CALL = "incoming";
    private static final String AUDIO_URL = "audioUrl";
    private SparkAgent agent;
    private FullScreenSwitcher screenSwitcher;
    private boolean isConnected = false;

    // Recorder
    Button buttonStart, buttonStop, buttonPlayLastRecordAudio,
            buttonStopPlayingRecording ;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder ;
    Random random ;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer ;

    @BindView(R.id.localView)
    View localView;

    @BindView(R.id.remoteView)
    View remoteView;

    @BindView(R.id.screenShare)
    View screenShare;

    @BindView(R.id.buttonHangup)
    Button buttonHangup;

    @BindView(R.id.buttonDTMF)
    Button buttonDTMF;

    @BindView(R.id.switchLoudSpeaker)
    Switch switchLoudSpeaker;

    @BindView(R.id.switchSendVideo)
    Switch switchSendingVideo;

    @BindView(R.id.switchReceiveVideo)
    Switch switchReceiveVideo;

    @BindView(R.id.switchReceiveAudio)
    Switch switchReceiveAudio;

    @BindView(R.id.radioFrontCam)
    RadioButton radioFrontCam;

    @BindView(R.id.radioBackCam)
    RadioButton radioBackCam;

    @BindView(R.id.call_layout)
    ConstraintLayout layout;

    @BindView(R.id.switchShareContent)
    Switch switchShareContent;

    // Required empty public constructor
    public CallFragment() {
    }

    public static CallFragment newAnswerCallInstance() {
        return CallFragment.newInstance(INCOMING_CALL);
    }

    public static CallFragment newInstance(String id) {
        CallFragment fragment = new CallFragment();
        Bundle args = new Bundle();
        args.putInt(LAYOUT, R.layout.fragment_call);
        args.putString(CALLEE, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        buttonStart = (Button) getActivity().findViewById(R.id.start_rec);
        buttonStop = (Button) getActivity().findViewById(R.id.stop_rec);
        buttonPlayLastRecordAudio = (Button) getActivity().findViewById(R.id.play_rec);
        buttonStopPlayingRecording = (Button)getActivity().findViewById(R.id.pause_rec);

        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(false);
        buttonStopPlayingRecording.setEnabled(false);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeechToText speechToText = new SpeechToText(AudioSavePathInDevice);
                speechToText.startAPI(AudioSavePathInDevice, null);
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        random = new Random();

        buttonPlayLastRecordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {

                buttonStop.setEnabled(false);
                buttonStart.setEnabled(false);
                buttonStopPlayingRecording.setEnabled(true);

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(AudioSavePathInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                Toast.makeText(getActivity(), "Recording Playing",
                        Toast.LENGTH_LONG).show();
            }
        });

        buttonStopPlayingRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);

                if(mediaPlayer != null){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    MediaRecorderReady();
                }
            }
        });


        agent = SparkAgent.getInstance();
        screenSwitcher = new FullScreenSwitcher(getActivity(), layout, remoteView);

        updateScreenShareView();
        if (!isConnected) {
            startRecording();
            setViewAndChildrenEnabled(layout, false);
            ((SurfaceView)localView).setZOrderMediaOverlay(true);
            ((SurfaceView)screenShare).setZOrderMediaOverlay(true);
            //requirePermission();
            makeCall();
        }

    }

    private void startRecording(){

        System.out.println("Mic Availability:" +validateMicAvailability());

        if(checkPermission()) {

            AudioSavePathInDevice =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                            CreateRandomAudioFileName(5) + "AudioRecording.mpeg4";

            MediaRecorderReady();

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Log.d("ButtonStart", "onClick: Started Recording");
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            buttonStart.setEnabled(false);
            buttonStop.setEnabled(true);
            buttonPlayLastRecordAudio.setEnabled(false);
            buttonStopPlayingRecording.setEnabled(false);

            Toast.makeText(getActivity(), "Recording started",
                    Toast.LENGTH_LONG).show();
        } else {
            requestPermission();
        }

    }

    private void stopRecording(){
        try {
            mediaRecorder.stop();
        } catch (IllegalStateException i){
            // Meh
        }
        mediaRecorder.release();
        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(true);
        buttonStart.setEnabled(true);
        buttonStopPlayingRecording.setEnabled(false);

        Toast.makeText(getActivity(), "Recording Completed",
                Toast.LENGTH_LONG).show();
    }

    private boolean validateMicAvailability(){
        Boolean available = true;
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try{
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
                available = false;

            }

            recorder.startRecording();
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
                recorder.stop();
                available = false;

            }
            recorder.stop();
        } finally{
            recorder.release();
            recorder = null;
        }

        return available;
    }

    public void MediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    public String CreateRandomAudioFileName(int string){
        StringBuilder stringBuilder = new StringBuilder( string );
        int i = 0 ;
        while(i < string ) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++ ;
        }
        return stringBuilder.toString();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(), new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(getActivity(), "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(),"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    private void setupWidgetStates() {
        switch (agent.getDefaultCamera()) {
            case FRONT:
                radioFrontCam.setChecked(true);
                break;
            case BACK:
                radioBackCam.setChecked(true);
                break;
            case CLOSE:
                localView.setVisibility(View.GONE);
                break;
        }
        switchLoudSpeaker.setChecked(agent.getSpeakerPhoneOn());
        switchSendingVideo.setChecked(agent.isSendingVideo());
        switchReceiveVideo.setChecked(agent.isReceivingVideo());
        switchReceiveAudio.setChecked(agent.isReceivingAudio());
        updateScreenShareView();
    }

    private void updateScreenShareView() {
        screenShare.setVisibility(agent.isScreenSharing() ? View.VISIBLE : View.INVISIBLE);
    }

    private void requirePermission() {
        new RequirePermissionAction(getActivity()).execute();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void setButtonsEnable(boolean enable) {
        buttonHangup.setEnabled(enable);
        buttonDTMF.setEnabled(false);
    }

    @OnClick(R.id.buttonHangup)
    public void onHangup() {
        /*
        if (isConnected) {
            agent.hangup();
        } else {
            ((LauncherActivity) getActivity()).goBackStack();
        }
        */
        try {
            mediaRecorder.stop();
        } catch (IllegalStateException i){
            // Meh
        }
        mediaRecorder.release();
        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(true);
        buttonStart.setEnabled(true);
        buttonStopPlayingRecording.setEnabled(false);

        Toast.makeText(getActivity(), "Recording Completed",
                Toast.LENGTH_LONG).show();
        agent.hangup();
        ((LauncherActivity) getActivity()).goBackStack();
    }

    @OnClick(R.id.buttonDTMF)
    public void sendDTMF() {
    }

    @OnClick(R.id.remoteView)
    public void onRemoteViewClicked() {
        screenSwitcher.toggleFullScreen();
        updateScreenShareView();
    }

    @OnCheckedChanged({R.id.switchSendVideo,
            R.id.switchReceiveVideo, R.id.switchReceiveAudio, R.id.switchShareContent})
    public void onSwitchCallAbility(Switch s) {
        switch (s.getId()) {
            case R.id.switchSendVideo:
                if (radioBackCam.isChecked())
                    agent.setBackCamera();
                else {
                    radioFrontCam.setChecked(true);
                    agent.setFrontCamera();
                }
                agent.sendVideo(s.isChecked());
                break;
            case R.id.switchReceiveVideo:
                agent.receiveVideo(s.isChecked());
                break;
            case R.id.switchReceiveAudio:
                agent.receiveAudio(s.isChecked());
                break;
            case R.id.switchShareContent:
                if (s.isChecked())
                    agent.getActiveCall().startSharing(r -> {Ln.d("startSharing result: " + r);});
                else
                    agent.getActiveCall().stopSharing(r -> {Ln.d("stopSharing result: " + r);});
                break;

        }
    }

    @OnCheckedChanged(R.id.switchLoudSpeaker)
    public void onSwitchLoudSpeakerChanged(Switch s) {
        //new toggleSpeakerAction(getActivity(), s.isChecked()).execute();
        switchLoudSpeaker.setChecked(true);
        Toast.makeText(getActivity(), "Loudspeaker is Enforced", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.radioBackCam)
    public void onBackCamRadioClicked() {
        agent.setBackCamera();
    }

    @OnClick(R.id.radioFrontCam)
    public void onFrontCamRadioClicked() {
        agent.setFrontCamera();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        screenSwitcher.updateOnRotation();
        updateScreenShareView();
    }

    @Override
    public void onBackPressed() {
        if (isConnected)
            agent.hangup();
    }

    private void makeCall() {
        String callee = getCallee();
        if (callee.isEmpty())
            return;

        if (callee.equals(INCOMING_CALL)) {
            setButtonsEnable(false);
            agent.answer(localView, remoteView, screenShare);
            return;
        }

        agent.dial(callee, localView, remoteView, screenShare);
        new AddCallHistoryAction(callee, "out").execute();
        setButtonsEnable(true);
    }

    private String getCallee() {
        Bundle bundle = getArguments();
        return bundle != null ? bundle.getString(CALLEE) : "";
    }

    private void feedback() {
        BaseFragment fm = new CallFeedbackFragment();
        ((LauncherActivity) getActivity()).replace(fm);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DialEvent event) {
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AnswerEvent event) {
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HangupEvent event) {
        setButtonsEnable(false);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnRingingEvent event) {
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnConnectEvent event) {
        isConnected = true;
        setViewAndChildrenEnabled(layout, true);
        if (agent.getDefaultCamera().equals(SparkAgent.CameraCap.CLOSE))
            agent.sendVideo(false);
        setupWidgetStates();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnDisconnectEvent event) {
        if (agent.getActiveCall() == null || event.getCall().equals(agent.getActiveCall())) {
            feedback();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnMediaChangeEvent event) {
        if (event.callEvent instanceof RemoteSendingSharingEvent) {
            Ln.d("RemoteSendingSharingEvent: " + ((RemoteSendingSharingEvent)event.callEvent).isSending());
            updateScreenShareView();
        } else if (event.callEvent instanceof SendingSharingEvent) {
            Ln.d("SendingSharingEvent: " + ((SendingSharingEvent)event.callEvent).isSending());
            if (((SendingSharingEvent)event.callEvent).isSending()){
                sendNotification();
                backToHome();
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PermissionAcquiredEvent event) {
        makeCall();
    }

    private void backToHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        this.startActivity(intent);
    }

    private void sendNotification(){
        Intent appIntent = new Intent(getActivity(), LauncherActivity.class);
        appIntent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent contentIntent = PendingIntent.getActivity(getActivity(), 0,appIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notifyManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Cisco Kichensink")
                .setContentText("I'm sharing content")
                .setContentIntent(contentIntent);
        notifyManager.notify(1, builder.build());
    }
}
