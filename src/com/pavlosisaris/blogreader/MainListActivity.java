package com.pavlosisaris.blogreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainListActivity extends ListActivity {
	
	//alternatively, android names are stored in the values/string.xml file
	/*protected String[] mBlogPostTitles; = {"Froyo",
										"Gingerbread",
										"HoneyComb",
										"Ice Cream Sandwich",
										"Jelly Bean",
										"Cupcake",
										"Eclair",
										"Donut"};*/
	public static final int NUMBER_OF_POSTS = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();
	int responseCode = -1;
	protected JSONObject mBlogData;
	protected ProgressBar mProgressBar;
	private final String KEY_TITLE = "title";
	private final String KEY_AUTHOR = "author";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		if(isNetworkAvailable()){
			//the progress bar is set to visible, when handleBlogResponse is called (automatically), will then be invisible.
			mProgressBar.setVisibility(View.VISIBLE);
			GetBlogPostTask getBlogPostTask = new GetBlogPostTask();
			getBlogPostTask.execute(); //execute automatically calls doInBackground(), and other 3 methods.
		}
		else{
			Toast.makeText(this, "Network is unavailable.", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		//here the "posts" field is taken
		JSONArray jsonPosts;
		try {
			jsonPosts = mBlogData.getJSONArray("posts");
			JSONObject jsonPost = jsonPosts.getJSONObject(position);
			//here the "url" field is taken
			String blogUrl = jsonPost.getString("url");
			//a new Activity is created
			Intent intent = new Intent(this, BlogWebViewActivity.class);
			intent.setData(Uri.parse(blogUrl));
			startActivity(intent);
			
		} catch (JSONException e) {
			logExeception(e);
		}

	}

	private void logExeception(Exception e) {
		Log.e(TAG, "Exception caught: "+ e);
	}

	private boolean isNetworkAvailable() {

		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		
		boolean isAvailable = false;
		if(networkInfo != null && networkInfo.isConnected()){
			isAvailable = true;
		}
		
		return isAvailable;
	}
	
	public void handleBlogResponse() 
	{
		mProgressBar.setVisibility(View.INVISIBLE);
		if(mBlogData == null)
		{
			
			updateDipsplayForError();
			
		}
		else{
			try {
				//Log.d(TAG,mBlogData.toString(2));
				JSONArray jsonPosts = mBlogData.getJSONArray("posts");
				
				//here I create an ArrayList which will store a HashMap with titles and authors.
				//the template has 2 String because both titles and authors are String fields.
				ArrayList<HashMap<String,String>> blogPosts = new ArrayList<HashMap<String,String>>();
				
				for(int i = 0; i<jsonPosts.length();i++)
				{
					//looking for the jsonPosts
					JSONObject post = jsonPosts.getJSONObject(i);
					//the "title" field is extrected from every post (we must be sure there is such a field called "title")
					String title = post.getString(KEY_TITLE);
					//the string is cleaned from HTML special characters:
					title = Html.fromHtml(title).toString();
					//the "author" from every post is taken (we must be sure there is such a field called "author")
					String author = post.getString(KEY_AUTHOR);
					//the string is cleaned from HTML special characters:
					author = Html.fromHtml(author).toString();
					//and store them to the ArrayList object
					
					HashMap<String, String> blogPost = new HashMap<String, String>();
					blogPost.put(KEY_TITLE,title);
					blogPost.put(KEY_AUTHOR, author);
					
					blogPosts.add(blogPost);
					
				}
				String[] keys = {KEY_TITLE,KEY_AUTHOR};
				int[] ids = {android.R.id.text1, android.R.id.text2};
				SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
				setListAdapter(adapter);
			} catch (JSONException e) {
				Log.e(TAG, "Exception caught: "+ e);
			}
		}
		
	}

	private void updateDipsplayForError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		//the string below are stored in res/values/strings.xml (in the "resources" tab)
		builder.setTitle(getString(R.string.error_title));
		builder.setMessage(getString(R.string.error_message));
		
		//the default "ok" android button is set, null is the click listener
		//since we don't want it to have a specific listener
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();
		
		TextView emptyTextView = (TextView) getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.no_items));
	}
	
	private class GetBlogPostTask extends AsyncTask<Object, Void, JSONObject>{

		@Override
		protected JSONObject doInBackground(Object... arg0) {
			JSONObject jsonResponse = null;

			try{
				//connecting to the internet
				URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count="+NUMBER_OF_POSTS);
				HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
				connection.connect();
				
				responseCode = connection.getResponseCode();
				if(responseCode == HttpURLConnection.HTTP_OK){
					
					//setting the InputStream from the connection
					InputStream inputStream = connection.getInputStream();
					Reader reader = new InputStreamReader(inputStream);
					int contentLength = connection.getContentLength();
					
					//putting the data into an array
					char[] charArray = new char [contentLength];
					reader.read(charArray);
					
					//transforming the data into a String 
					String responseData = new String(charArray);
					
					//parsing into JSON
					jsonResponse = new JSONObject(responseData);

				}
				else{
					Log.i(TAG,"Usuccesful Http Resposne Code: " + responseCode);
				}
				
			}
			catch(MalformedURLException e){
				logExeception(e);
			} 
			catch (IOException e) {
				logExeception(e);
			}
			catch(Exception e){
				logExeception(e);
			}
			
			return jsonResponse;
		}
		
		@Override
		protected void onPostExecute(JSONObject result){
			mBlogData = result;
			handleBlogResponse();
		}
		
	}


}
