package com.revyuk.krok_helper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    SharedPreferences preferences;
    EditText flcode;
    TextView totalCount;
    CheckBox fullsearch;
    ListView listView;
    SameerDB db;
    MytextWatcher watcher;
    SearchAdapter adapter;
    ArrayList<QueryResult> list = new ArrayList<>();
    ProgressDialog dialog;
    int database_count;
    boolean key_flg = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        preferences = getSharedPreferences(getPackageName()+"_preferences", MODE_PRIVATE);

        findViewById(R.id.mymenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        flcode = (EditText) findViewById(R.id.flcode);
        fullsearch = (CheckBox) findViewById(R.id.fullSeach);
        listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(this);
        dialog = new ProgressDialog(this);
        findViewById(R.id.red_erase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flcode.setText("");
            }
        });
        //checkKey();
    }

    @Override
    protected void onPause() {
        flcode.removeTextChangedListener(watcher);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        watcher = new MytextWatcher();
        flcode.addTextChangedListener(watcher);
        adapter = new SearchAdapter(this, 0, list);
        listView.setAdapter(adapter);
        db = new SameerDB(MainActivity.this);
        database_count = db.getCount();
        db.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SearchAdapter searchAdapter = (SearchAdapter) parent.getAdapter();
        for(int i=0; i<searchAdapter.getCount(); i++) {
            if(i == position) {
                searchAdapter.getItem(position).question_expanded = !searchAdapter.getItem(position).question_expanded;
            } else {
                searchAdapter.getItem(i).question_expanded = false;
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
    }

    private class MytextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            try {
                db = new SameerDB(MainActivity.this);
                adapter.clear();
                ArrayList<QueryResult> list = db.searchFlcode(s.toString().toLowerCase(), fullsearch.isChecked());
                for(int i=0; i<list.size(); i++) {
                    adapter.add(list.get(i));
                }
                adapter.notifyDataSetChanged();
                db.close();
                db = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(flcode.getText().length() > 0) {
            flcode.setText("");
            return;
        }
        super.onBackPressed();
    }

    private class SearchAdapter extends ArrayAdapter<QueryResult> {

        public SearchAdapter(Context context, int resource, List<QueryResult> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            ViewHolder holder;
            if(v == null) {
                holder = new ViewHolder();
                v = getLayoutInflater().inflate(R.layout.search_item, parent, false);
                holder.answer = (TextView) v.findViewById(R.id.answer);
                holder.question = (TextView) v.findViewById(R.id.question);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            holder.answer.setText(getItem(position).answer);
            if(!getItem(position).question_expanded) {
                holder.question.setSingleLine(true);
                holder.answer.setSingleLine(true);
            } else {
                holder.question.setSingleLine(false);
                holder.answer.setSingleLine(false);
            }
            holder.question.setText(getItem(position).question);
            return v;
        }

        class ViewHolder {
            TextView answer;
            TextView question;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1, 1, 1, "Update database");
        menu.add(1, 2, 2, "Clear all databases");
        menu.add(1, 4, 4, "About");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == 1) {
            checkKey();
            //new LoadingDatabase().execute();
        } else if(item.getItemId() == 2) {
            db = new SameerDB(this);
            db.erase();
            db.close();
            new Toast(this).makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
        } else if(item.getItemId() == 4) {
            View v = getLayoutInflater().inflate(R.layout.about, null, false);
            totalCount = (TextView) v.findViewById(R.id.total_count);
            totalCount.setText("Total "+database_count+" questions");
            new AlertDialog.Builder(this).setView(v).setNeutralButton("ok", null).show();
        }
        return false;
    }

    void checkKey() {

        String instalation_code;
        if(!preferences.contains("instalation_code")) {
            CRC32 crc32;
            crc32 = new CRC32();
            crc32.update(UUID.randomUUID().toString().getBytes());
            instalation_code = String.valueOf(crc32.getValue());
            preferences.edit().putString("instalation_code", instalation_code).apply();
        } else {
            instalation_code = preferences.getString("instalation_code", "XXX");
        }
        //Log.d("XXX", "UUID: " + instalation_code);

        View view = getLayoutInflater().inflate(R.layout.license_key, null, false);
        final EditText phone_number = (EditText) view.findViewById(R.id.phone);
        phone_number.setText(instalation_code);
        final EditText phone_key = (EditText) view.findViewById(R.id.key);
        new AlertDialog.Builder(this).setView(view).setTitle("Krok Helper")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            int c_size = Integer.valueOf(phone_key.getText().subSequence(0, 1).toString());
                            String db_code = phone_key.getText().subSequence(1, 1+c_size).toString();
                            String r_key = phone_key.getText().subSequence(1+c_size, phone_key.length()).toString();
                            CRC32 _crc32 = new CRC32();
                            String tmp = phone_number.getText().toString()+db_code;
                            _crc32.update(tmp.getBytes());
                            long key = Math.abs(_crc32.getValue());
                            //Log.d("XXX", "CSIZE: "+c_size+" CODE: "+db_code+" tmp: "+tmp+" key: "+key+" r_key: "+r_key);
                            if(r_key.equals(String.valueOf(key))) {
                                //preferences.edit().putString("key_code", db_code).putBoolean("key_flg", true).apply();
                                new LoadingDatabase(db_code).execute();
                            } else {
                                new Toast(MainActivity.this).makeText(MainActivity.this, "Wrong code !!!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false).show();
    }

    class LoadingDatabase extends AsyncTask<Void, Integer, Void> {
        boolean cancel_flg = false;
        String code;

        public LoadingDatabase(String code) {
            this.code = code;
        }

        @Override
        protected void onPreExecute() {
            //new Toast(MainActivity.this).makeText(MainActivity.this, "COUNT: "+preferences.getString("key_code", ""), Toast.LENGTH_SHORT).show();
            dialog.setMessage("Updating database ...");
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                    cancel_flg = true;
                }
            });
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            db = new SameerDB(MainActivity.this);
            database_count = db.getCount();
            totalCount.setText(database_count+" questions");
            db.close();
            dialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            dialog.dismiss();
            new AlertDialog.Builder(MainActivity.this).setTitle("Warning")
                    .setMessage("You aborted updating database. Applications may not work properly. You must update the database again.")
                    .setNegativeButton("Yes, I understand", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setCancelable(true).show();
            super.onCancelled();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int max, progress;
            max = values[0]; progress = values[1];
            dialog.setMax(max);
            dialog.setProgress(progress);
        }

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress(0, 0);
            try {
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(5, TimeUnit.SECONDS);
                Request request = new Request.Builder().url("http://revyuk.bugs3.com/get.php?code="+code).build();
                //Request request = new Request.Builder().url("http://192.168.1.3/sameer/get.php").build();
                Response response = client.newCall(request).execute();

                String str = response.body().string();
                Log.d("XXX", "DB: "+str);
                JSONTokener tokener = new JSONTokener(str);
                if(tokener.more()) {
                    JSONArray array = (JSONArray) tokener.nextValue();
                    db = new SameerDB(MainActivity.this);
                    SQLiteDatabase sqLiteDatabase = db.getWritableDatabase();
                    //sqLiteDatabase.execSQL("delete from "+SameerDB.TABLE_NAME);
                    for(int i=0; i < array.length(); i++) {
                        if(isCancelled() || cancel_flg) break;
                        JSONObject obj = array.getJSONObject(i);
                        String query = String.format("INSERT INTO '%s' VALUES (%d, \"%s\", \"%s\", \"%s\")"
                                ,SameerDB.TABLE_NAME, obj.getInt("id"), obj.getString("question"), obj.getString("answer"), obj.getString("flcode"));
                        sqLiteDatabase.execSQL(query);
                        publishProgress(array.length(), i);
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            if(db!=null) db.close();
            return null;
        }
    }
}
