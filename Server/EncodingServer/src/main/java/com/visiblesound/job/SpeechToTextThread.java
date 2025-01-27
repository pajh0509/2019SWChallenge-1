package com.visiblesound.job;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

import java.io.*;

import java.nio.charset.StandardCharsets;
import static java.nio.charset.StandardCharsets.UTF_8;

//gcp bucket upload
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;


//firebase
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.auth.oauth2.*;
import com.google.firebase.database.*;
import com.google.firebase.messaging.*;


import java.util.Map;
import java.util.HashMap;

public class SpeechToTextThread extends Thread {
	private String username;
	private String filename;

    private static final String ffmpegPath = "/usr/bin/ffmpeg";
	private static final String ffprobePath = "/usr/bin/ffprobe";
	private static final String formatName = "flac";
	private static final String storagePath = "/home/vvuser/";


	public SpeechToTextThread(String username, String filename) {
		this.username = username;
		this.filename = filename;
	}

	public void run() {
		String convertedFileName = convertFile(username, filename);
		System.out.println("LOG_STT_THREAD: " + "convert to flac");

		runCommand(new String[] { "python", "src/main/java/com/visiblesound/gcp/upload_from_server_to_GCP.py", storagePath + username + "/" + convertedFileName, username + "/" + convertedFileName });
	
		System.out.println("LOG_STT_THREAD: " + "upload to gc");

        // Request Speech 2 Text
        runCommand(new String[] { "python", "src/main/java/com/visiblesound/gcp/transcribe_async.py", "gs://visible_voice/", username, convertedFileName });
		System.out.println("LOG_STT_THREAD: " + "stt success!!");

		runCommand(new String[] { "python", "src/main/java/com/visiblesound/wordcloud/generate_word_cloud_with_args.py", storagePath + username + "/" + filename, storagePath + username + "/" + filename.split("\\.")[0] + "." + "png"});

        sendNotification();
        System.out.println("LOG_STT_THREAD: " + "sent notification!!!");
	}

    public static void sendNotification(){

        System.out.println("LOG_STT_THREAD: running sendNotification");
        //init firebase
        FileInputStream serviceAccount = null;
        try{
            serviceAccount = new FileInputStream("visiblevoice-a4862-firebase-adminsdk-7f6ie-48cf0a7c77.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://visiblevoice-a4862.firebaseio.com")
            .build();
            FirebaseApp.initializeApp(options);
        }catch(Exception e){
            System.out.println("LOG_STT_THREAD_FireBase Init error: "+e.getMessage());
        }

        
        //query
        //get registrationToken from the query
        
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("users");

        // Attach a listener to read the data at our posts reference
        ref.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            for (DataSnapshot messageData : dataSnapshot.getChildren()) {

                    String email = ((Map<String,Object>)messageData.getValue()).get("userID").toString() ;

                    if(email.equals("gygacpu@naver.com")){
                        String registrationToken = ((Map<String,Object>)messageData.getValue()).get("deviceToken").toString();
                        System.out.println("LOG_STT_THREAD_registrationToken: "+registrationToken);
                        
                        Message message = Message.builder()
                            .putData("Your audio has been visualized successfully!", "1")
                            .setToken(registrationToken)
                            .build();

                        // Send a message to the device corresponding to the provided
                        // registration token.
                        try{
                        String response = FirebaseMessaging.getInstance().send(message);
                        System.out.println("LOG_STT_THREAD_response: "+response);
                        // Response is a message ID string.
                        }catch(Exception e){
                            System.out.println("LOG_STT_THREAD_send_Error: "+e.getMessage());
                        }
                        break;
                    }
                }   
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            System.out.println("The read failed: " + databaseError.getCode());
        }});

		Message message = Message.builder()
			.putData("Your audio has been visualized successfully!", "1")
			.setToken("dg5H0sAUfBs:APA91bGPD1gflUQvHiCsPv6LVrt_vUQ0vzy0YedMl3Vx-OngJtv5UXUdZ_9BzFOmoppT1mdsJuI3cOwfgbEaXViRbJHDdANN27JVWaTe6J5W0URdwhcF_Vd2BZd2qA-1c2cbAOhQs-vu")
			.build();

		try {
			String response = FirebaseMessaging.getInstance().send(message);
			System.out.println("Response: " + response);
		} catch (Exception e) {
			System.out.println("err");
		}

        
    }

	public String convertFile(String username, String filename) {
        String convertFileName = filename.split("\\.")[0] + "." + formatName;
        try {
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(storagePath + username + "/" + filename)
                    .overrideOutputFiles(true)
                    .addOutput(storagePath + username + "/" + convertFileName)
                    .setAudioChannels(1)
            .setAudioSampleRate(16000) //(16_000)???
            .setFormat(formatName)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
			runCommand(new String[] { "chmod", "757" ,(storagePath + username + "/" + convertFileName) } );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return convertFileName;
    }

	public void runCommand(String [] args) {
        Process process = null;

        try {
            /* run command*/
			System.out.print("running command [");
			for(int i = 0; i < args.length; i++)
            	System.out.print(args[i] + " ");
			System.out.println("]\n");
            process = Runtime.getRuntime().exec(args);

        } catch (Exception e) {
            /* Exception handling*/
            System.out.println("Exception Raised" + e.toString());
        }

        /* get stdout from the execution*/
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();
        BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));
        BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = stdOutReader.readLine()) != null) {
                System.out.println("stdout: " + line);
            }
            while ((line = stdErrReader.readLine()) != null) {
                System.out.println("stderr: " + line);
            }
        } catch (IOException e) {
            /* Exception handling*/
            System.out.println("Exception in reading output" + e.toString());
        }


    }
}

