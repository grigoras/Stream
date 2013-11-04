package com.example.videostream;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

public class MainActivity extends Activity {

	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	static final Object threadObject = new Object();
	String file_name;
	View dialog_view;
	CharSequence[] items;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void startFilm(View view) {

		// Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

		// intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
		// intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024);
		// intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

		// startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

		Intent intent = new Intent(this, Video.class);
		startActivity(intent);

	}

	public void getVideos(View view) {

		ArrayList<String> value = new ArrayList<String>();

		try {
			HttpClient client = new DefaultHttpClient();
			String getURL = "http://10.0.90.9:7000/wowza/getAllVideosURL";
			HttpGet get = new HttpGet(getURL);
			HttpResponse responseGet = client.execute(get);
			HttpEntity resEntityGet = responseGet.getEntity();
			if (resEntityGet != null) {
				String response = EntityUtils.toString(resEntityGet);
				System.out.println(response);
				JsonParserFactory factory = JsonParserFactory.getInstance();
				JSONParser parser = factory.newJsonParser();
				Map<String, ArrayList> jsonData = parser.parseJson(response);
				System.out.println(jsonData);
				value = jsonData.get("Videos");
				for (int i = 0; i < value.size(); i++) {
					if (!(value.get(i).endsWith(".3gp") || value.get(i).endsWith(".mp4"))) {
						value.remove(i);
					}
				}
				value.remove(0);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		items = new CharSequence[value.size()];
		for (int i = 0; i < value.size(); i++) {
			items[i] = value.get(i);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("List of files ");
		builder.setItems(items, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int item) {
				playFilm(items[item].toString());
			}
		}).show();

	}

	public void playFilm(String name) {

		System.out.println(name);
		final String SrcPath = "rtsp://10.0.90.9:1935/vod/mp4:" + name;
		new Thread(new Runnable() {

			@Override
			public void run() {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SrcPath));
				startActivity(intent);
				// myVideoView.setVideoURI(Uri.parse(SrcPath));
			}

		}).start();
		// Log.i("parse path", Uri.parse(SrcPath) + "");
		// myVideoView.setMediaController(new MediaController(this));
		// myVideoView.requestFocus();
		// myVideoView.start();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

		if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {

				LayoutInflater inflater = this.getLayoutInflater();
				dialog_view = inflater.inflate(R.layout.dialog_moviename, null);
				new AlertDialog.Builder(this).setView(dialog_view)
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
						// Video captured and saved to fileUri specified in the
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
							String a = getRealPathFromVideoURI(data.getData());
							entity.addTextBody("title", file_name);
							entity.addPart("file", new FileBody(new File(a)));
							httpost.setEntity(entity.build());
							httpClient.execute(httpost);
							File from = new File(a);
							File to = new File(a.substring(0, a.lastIndexOf("/") + 1) + file_name);
							from.renameTo(to);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}

				}).start();
			}
			else if (resultCode == RESULT_CANCELED) {
				// User cancelled the video capture
			}
			else {
				// Video capture failed, advise user
			}
		}
	}

	public String getRealPathFromVideoURI(Uri uri) {
		String yourRealPath = "";
		String[] filePathColumn = { MediaStore.Video.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
		if (cursor.moveToFirst()) {
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			yourRealPath = cursor.getString(columnIndex);
		}
		cursor.close();
		return yourRealPath;
	}

}
