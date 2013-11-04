package com.example.videostream;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ToggleButton;

public class Video extends Activity implements OnClickListener, SurfaceHolder.Callback {

	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private MediaRecorder mMediaRecorder;
	boolean mInitSuccesful = false;
	File file;
	ToggleButton mToggleButton;
	View dialog_view;
	static final Object threadObject = new Object();
	Builder b;
	String file_name;
	Activity act;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		act = this;

		LayoutInflater inflater = this.getLayoutInflater();
		dialog_view = inflater.inflate(R.layout.dialog_moviename, null);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mToggleButton = (ToggleButton) findViewById(R.id.toggleRecordingButton);
		mToggleButton.setOnClickListener(new OnClickListener() {

			@Override
			// toggle video recording
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked())
					mMediaRecorder.start();
				else {
					mMediaRecorder.stop();
					new AlertDialog.Builder(act).setView(dialog_view)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int id) {
									synchronized (threadObject) {
										EditText ed = (EditText) dialog_view
												.findViewById(R.id.namemovie);
										file_name = ed.getEditableText().toString() + ".mp4";
										System.out.println("Numele este:" + file_name);
										threadObject.notifyAll();
									}
								}
							}).show();
					new Thread(new Runnable() {

						@Override
						public void run() {
							// Video captured and saved to fileUri specified in
							// the
							// Intent
							synchronized (threadObject) {
								try {
									threadObject.wait();
								}
								catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							HttpPost httpost = new HttpPost("http://10.0.90.9:7000/wowza/postVideo");
							HttpClient httpClient = new DefaultHttpClient();
							MultipartEntityBuilder entity = MultipartEntityBuilder.create();
							try {
								entity.addTextBody("title", file_name);
								entity.addPart("file", new FileBody(file));
								httpost.setEntity(entity.build());
								httpClient.execute(httpost);
								String a = file.getAbsolutePath();
								File to = new File(a.substring(0, a.lastIndexOf("/") + 1)
										+ file_name);
								file.renameTo(to);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							act.finish();
						}

					}).start();

				}
			}
		});
	}

	private void initRecorder(Surface surface) throws IOException {
		// It is very important to unlock the camera before doing setCamera
		// or it will results in a black preview
		if (mCamera == null) {
			mCamera = Camera.open();
			mCamera.setDisplayOrientation(90);
			mCamera.unlock();
		}

		if (mMediaRecorder == null)
			mMediaRecorder = new MediaRecorder();

		mMediaRecorder.setPreviewDisplay(surface);
		mMediaRecorder.setCamera(mCamera);

		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		mMediaRecorder.setOutputFile(this.initFile().getAbsolutePath());

		// No limit. Don't forget to check the space on disk.
		mMediaRecorder.setMaxDuration(50000);
		mMediaRecorder.setVideoFrameRate(24);
		mMediaRecorder.setVideoSize(1280, 720);
		mMediaRecorder.setVideoEncodingBitRate(3000000);
		mMediaRecorder.setAudioEncodingBitRate(8000);

		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

		try {
			mMediaRecorder.prepare();
		}
		catch (IllegalStateException e) {
			// This is thrown if the previous calls are not called with the
			// proper order
			e.printStackTrace();
		}

		mInitSuccesful = true;
	}

	private File initFile() {

		File dir = new File(Environment.getExternalStorageDirectory(), this.getClass().getPackage()
				.getName());

		if (!dir.exists() && !dir.mkdirs()) {
			file = null;
		}
		else {
			file = new File(dir.getAbsolutePath(), "new");
		}
		return file;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (!mInitSuccesful)
				initRecorder(mHolder.getSurface());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void shutdown() {
		// Release MediaRecorder and especially the Camera as it's a shared
		// object that can be used by other applications
		mMediaRecorder.reset();
		mMediaRecorder.release();
		mCamera.release();

		// once the objects have been released they can't be reused
		mMediaRecorder = null;
		mCamera = null;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		shutdown();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View v) {
		// nothing
	}
}