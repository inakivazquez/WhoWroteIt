/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.whowroteit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * FetchBook is an AsyncTask implementation that opens a network connection
 * and queryies the Book Service API.
 */
public class FetchBook extends AsyncTask<String, Integer, String> {

    // Variables for the results TextViews.
    // These are WeakReferences to prevent "leaky context" -- weak references
    // enable the activity to be garbage collected if it is not needed.
    private WeakReference<MainActivity> activity;
    private Bitmap mBookImage;

    // Constructor, provides references to the views in MainActivity.
    FetchBook(MainActivity activity) {
        this.activity = new WeakReference<>(activity);
    }

    /* onPreExecute() is invoked before doInBackground() and has access to the UI Thread
     * We generally prepare the UI at this point.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        activity.get().mDownloadbar.setVisibility(View.VISIBLE);
        activity.get().mDownloadbar.setProgress(20);
    }

    /**
     * Use the getBookInfo() method in the NetworkUtils class to make
     * the connection in the background.
     *
     * @param strings String array containing the search data.
     * @return Returns the JSON string from the Books API, or
     * null if the connection failed.
     */
    @Override
    protected String doInBackground(String... strings) {
        String json;
        publishProgress(50);
        json = NetworkUtils.getBookInfo(strings[0]);
        publishProgress(100);
        return json;
    }

    /* onProgressUpdate() is invoked whenever we invoke publishProgress() and has access to the UI Thread
     * We generally update the UI at this point.
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        activity.get().mDownloadbar.setProgress(values[0].intValue());
    }

    /**
     * Handles the results on the UI thread. Gets the information from
     * the JSON result and updates the views.
     *
     * @param s Result from the doInBackground() method containing the raw
     *          JSON response, or null if it failed.
     */
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        activity.get().mDownloadbar.setProgress(0);
        activity.get().mDownloadbar.setVisibility(View.GONE);

        try {
            // Convert the response into a JSON object.
            JSONObject jsonObject = new JSONObject(s);
            // Get the JSONArray of book items.
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            // Initialize iterator and results fields.
            int i = 0;
            String title = null;
            String authors = null;
            String imgUri = null;

            // Look for results in the items array, exiting when both the
            // title and author are found or when all items have been checked.
            while (i < itemsArray.length() &&
                    (authors == null && title == null)) {
                // Get the current item information.
                JSONObject book = itemsArray.getJSONObject(i);
                JSONObject volumeInfo = book.getJSONObject("volumeInfo");

                // Try to get the author and title from the current item,
                // catch if either field is empty and move on.
                try {
                    title = volumeInfo.getString("title");
                    authors = volumeInfo.getString("authors");
                    JSONObject imgLinks = volumeInfo.getJSONObject("imageLinks");
                    if( imgLinks != null) {
                        imgUri = imgLinks.getString("thumbnail");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Move to the next item.
                i++;
            }

            // If both are found, display the result.
            if (title != null && authors != null) {
                activity.get().mTitleText.setText(title);
                activity.get().mAuthorText.setText(authors);
                if(imgUri != null){
                    activity.get().mBookCover.setVisibility(View.VISIBLE);
                    // Glide is a great library to work with images: download and caching
                    // It works asynchronously, so it can be called from the UI Thread
                    Glide.with(activity.get().getApplicationContext())
                            .load(Uri.parse(imgUri))
                            .into(activity.get().mBookCover);
                }else{
                    activity.get().mBookCover.setVisibility(View.INVISIBLE);
                }
            } else {
                // If none are found, update the UI to show failed results.
                activity.get().mTitleText.setText(R.string.no_results);
                activity.get().mAuthorText.setText("");
            }

        } catch (Exception e) {
            // If onPostExecute() does not receive a proper JSON string,
            // update the UI to show failed results.
            activity.get().mTitleText.setText(R.string.no_results);
            activity.get().mTitleText.setText("");
            e.printStackTrace();
        }

    }
}
