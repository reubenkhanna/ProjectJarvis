package com.rtg.finalproject;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.text.Line;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private ImageButton micBtn;
    private ImageButton sendBtn;
    private EditText inputMessage;
    private ChatAdapter chatAdapter;
    private ArrayList<Message> messageArrayList;
    private TextToSpeech textToSpeech;
    private static String url = "http://ec2-3-83-178-179.compute-1.amazonaws.com:5050/";
    private RecyclerView recyclerView;
    private boolean permissionToRecordAccepted = false;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        this.inputMessage = (EditText) findViewById(R.id.message);
        this.sendBtn = (ImageButton) findViewById(R.id.btn_send);
        this.micBtn = (ImageButton) findViewById(R.id.micBtn);
        this.messageArrayList = new ArrayList();
        this.chatAdapter = new ChatAdapter(this.messageArrayList);
        this.recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(chatAdapter);
        this.inputMessage.setText("");
        //Text to speech instance being created using local language US
        this.textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != -1) {
                    MainActivity.this.textToSpeech.setLanguage(Locale.US);
                }
            }
        });
        //Check if permission is given to use microphone else make request
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != 0) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }
        //Call when we click the send button call the send message function
        this.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Main Act", MainActivity.this.inputMessage.getText().toString().trim());
                if(!MainActivity.this.inputMessage.getText().toString().trim().equals("")){
                    MainActivity.this.sendMessage();
                }
            }
        });
        //Called when we press the mic calls prompSpeech Input method.
        this.micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.promptSpeechInput();
            }
        });
        //Call set user message Id for setup of self or client messages
        setUserMessageId();
    }
    //Called when we request the permission
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean z = false;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 101:
                if (grantResults.length == 0 || grantResults[0] != 0) {
                    Log.i(TAG, "Permission has been denied by user");
                    return;
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                    return;
                }
            case 200:
                if (grantResults[0] == 0) {
                    z = true;
                }
                this.permissionToRecordAccepted = z;
                break;
        }
        if (!this.permissionToRecordAccepted) {
            finish();
        }
    }
    //Make a request for permission
    //Created a separate function as it helps when we try to add additional permission list in future
    public void makeRequest() {
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO"}, 101);
    }

    /**
     *     Called when we have to send message
     */
    public void sendMessage() {
        //Gets the user input
        String inputmessage = this.inputMessage.getText().toString().trim();
        //Create a new instance of message
        Message inputMessage2 = new Message();
        //Sets the message
        inputMessage2.setMessage(inputmessage);
        //Sets the ID
        //Note: ID can be ignored added this for future case if we want to persists conversation and save contexts
        inputMessage2.setId("1");
        //Add to the message array
        this.messageArrayList.add(inputMessage2);
        //Send request to server
        sendingMessageToServer();
        //Set the text to blank
        this.inputMessage.setText("");
        //Notify the adapter that data is changed
        this.chatAdapter.notifyDataSetChanged();
    }

    /**
     *     This is called initially to set user message ID to 100 and specify that the message is being sent from client
     */
    public void setUserMessageId() {
        Message inputMessage3 = new Message();
        inputMessage3.setMessage(this.inputMessage.toString());
        inputMessage3.setId("100");
    }

    /**
     *   Has two events get the message and send the message to either News API or the Search API based on request
     */
    public void sendingMessageToServer() {
        String requestMsg = this.inputMessage.getText().toString();
        if (requestMsg.contains("news")) {
            handlingNewsEvent(requestMsg);
        } else {
            handlingChatEvents(requestMsg);
        }
    }

    /**
     *  This function is called to handle and search query by user
     *  This uses volley to call gets a JSONobject as result from the server
     *  Parses JSON Object
     *  Sets the message updated the adapter
     *  Scrolls to the response position in recycler view
      * @param message Message to be sent to server
     */

    public void handlingChatEvents(String message) {
        String requestString = message.replaceAll(" ", "_").replaceAll("'", "");
        String requestUrl = MainActivity.url + "search/" + requestString;
        this.requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(0, requestUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String data = new JSONObject(response.toString()).getString("result");
                    MainActivity.this.textToSpeech.speak(data, 0, null);
                    Message outputMessage = new Message();
                    outputMessage.setMessage(data);
                    outputMessage.setId("2");
                    MainActivity.this.messageArrayList.add(outputMessage);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.chatAdapter.notifyDataSetChanged();
                            if (MainActivity.this.chatAdapter.getItemCount() > 1) {
                                MainActivity.this.recyclerView.getLayoutManager().smoothScrollToPosition(MainActivity.this.recyclerView, null, MainActivity.this.chatAdapter.getItemCount() - 1);
                            }
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG + " JSON", e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.e(TAG, error.getMessage());
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, -1, 1.0f));
        this.requestQueue.add(jsonObjectRequest);
    }

    /**
     *  This function is called to handle News query by user
     *  This uses volley to call gets a JSONobject as result from the server
     *  Parses JSON Object
     *  Sets the message updated the adapter
     *  Scrolls to the response position in recycler view
     *  NOTE: A separate function was created because the category is vast and can be expanded exponentially
     *  Additional feature added to news is a URL  which points to the link of the article
     * @param message Message to be sent to server
     */
    public void handlingNewsEvent(String message) {
        String str = message;
        String requestUrl = MainActivity.url + "news";
        this.requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(0, requestUrl, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                try {
                    JSONObject obj = response.getJSONObject("result");
                        String title = obj.getString("title");
                        String url = obj.getString("url");
                        MainActivity.this.textToSpeech.speak(title, 0, null);
                        Message outmessage = new Message();
                        outmessage.setMessage(title + "\n" + url);
                        outmessage.setId("2");
                        MainActivity.this.messageArrayList.add(outmessage);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                MainActivity.this.chatAdapter.notifyDataSetChanged();
                                if (MainActivity.this.chatAdapter.getItemCount() > 1) {
                                    MainActivity.this.recyclerView.getLayoutManager().smoothScrollToPosition(MainActivity.this.recyclerView, null, MainActivity.this.chatAdapter.getItemCount() - 1);
                                }
                            }
                        });
                } catch (JSONException e) {
                    try {
                        e.printStackTrace();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, -1, 1.0f));
        this.requestQueue.add(jsonObjectRequest);
    }

    /**
     * This prompts the Google Speech to text interface to user
     */
    public void promptSpeechInput() {
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.LANGUAGE", Locale.getDefault());
        intent.putExtra("android.speech.extra.PROMPT", getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * This is called once the Speech to Text Input interface closes and to capture the query asked by user
     * @param requestCode Check for the request code
     * @param resultCode   Result of the action
     * @param data  data provided by the action
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == -1 && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra("android.speech.extra.RESULTS");
            this.inputMessage.setText((CharSequence) result.get(0));
            String input = (String) result.get(0);
            sendMessage();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
