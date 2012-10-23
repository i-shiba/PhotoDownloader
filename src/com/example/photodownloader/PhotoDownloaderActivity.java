package com.example.photodownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

public class PhotoDownloaderActivity extends Activity {

	Context mContext = this;
	Object[] objects;
	static String strUrl = "http://api.bing.net/json.aspx?AppId=<App ID>&Version=2.2&Sources=image";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_downloader);
        
        TextView tv = (TextView)findViewById(R.id.TextView1);
        tv.setText(Html.fromHtml(getResources().getString(R.string.label_branding)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        
        View button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View arg0){
        		doSearch();
        	}
        });
        
        View editText1 = findViewById(R.id.editText1);
        editText1.setOnKeyListener(new OnKeyListener(){
        	
        	@Override
        	public boolean onKey(View v, int keyCode, KeyEvent event){
        		if(event.getAction()==KeyEvent.ACTION_UP &&
        				keyCode == KeyEvent.KEYCODE_ENTER){
        			doSearch();
        			return true;
        		}
        		return false;
        	}
        });
    }
    
    public void doSearch(){
    	URL url;
    	String q = null;
    	EditText et = (EditText)this.findViewById(R.id.editText1);
    	
    	try{
    		q = URLEncoder.encode(et.getText().toString(), "UTF-8");
    		url = new URL(strUrl + "&Query=" + q);
    		new jsonTask().execute(url);
    	}catch (UnsupportedEncodingException e){
    		e.printStackTrace();
    	}catch (MalformedURLException e){
    		e.printStackTrace();
    	}
    	
    }
    
    private class jsonTask extends AsyncTask<URL, Integer, String>{

		@Override
		protected String doInBackground(URL... params) {
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) params[0].openConnection();
				connection.setDoInput(true);
				connection.connect();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(),"UTF-8"));
				String jsonText = reader.readLine();
				reader.close();
				return(jsonText);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(result != null){
				try{
					JSONObject jo = new JSONObject(result).
							getJSONObject("SearchResponse").getJSONObject("Image");
					JSONArray jsonArray = null;
					jsonArray = jo.getJSONArray("Results");
					objects = new Object[jsonArray.length()];
					URL[] thumbUrls = new URL[jsonArray.length()];
					for(int i = 0; i < jsonArray.length(); i++){
						objects[i] = jsonArray.getJSONObject(i);
						thumbUrls[i] = new URL(((JSONObject)objects[i]).
								getJSONObject("Thumbnail").getString("Url"));
					}
					new getThumbTask().execute(thumbUrls);
				}catch(JSONException el){
					el.printStackTrace();
				}catch(MalformedURLException e){
					e.printStackTrace();
				}
			}
		}
    }
    
    private class getThumbTask extends AsyncTask<URL, Integer, Bitmap[]>{

		@Override
		protected Bitmap[] doInBackground(URL... params) {
			HttpURLConnection connection = null;
			Bitmap[] bm = new Bitmap[params.length];
			try{
				for(int i = 0; i < params.length; i++){
					connection = (HttpURLConnection) params[i].openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream si = connection.getInputStream();
					bm[i] = BitmapFactory.decodeStream(si);
					si.close();
				}
				return bm;
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(connection != null){
					connection.disconnect();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap[] result) {
			super.onPostExecute(result);
			
			final ThumbnailAdapter adapter = new ThumbnailAdapter(mContext, objects, result);
			GridView lv = (GridView)findViewById(android.R.id.list);
			
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener(){
				public void onItemClick(AdapterView<?> parent, View v, int position, long id){
					try{
						Uri downloadUri = Uri.parse(
								((JSONObject)adapter.getItem(position)).getString("MediaUrl"));
						DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						DownloadManager.Request dr = new DownloadManager.Request(downloadUri);
						String fn = URLDecoder.decode(downloadUri.getLastPathSegment());
						dr.setDestinationInExternalPublicDir(
								Environment.DIRECTORY_DOWNLOADS,fn);
						dm.enqueue(dr);
					}catch(JSONException e){
						e.printStackTrace();
					}
				}

			});
		}
    }
}
