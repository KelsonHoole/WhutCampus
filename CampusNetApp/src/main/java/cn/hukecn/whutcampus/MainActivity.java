package cn.hukecn.whutcampus;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CampusNetLogin.PostFinishListener {
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private long lastTime = 0L;
    private List<UserAccount> accountList = null;
    private ArrayAdapter<String> arrayAdapter = null;
    private MaterialDialog progressDialog = null;
    private MaterialDialog infoDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        final SharedPreferences sp = getSharedPreferences("account", Context.MODE_PRIVATE);
        String username = sp.getString("username", "");
        String password = sp.getString("password", "");
        String jsonStr = readFromSD();
        if(jsonStr.length() > 0)
        {
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                JSONArray jsonArray = jsonObject.getJSONArray("account");
                if(jsonArray != null && jsonArray.length() > 0)
                {
                    accountList = new ArrayList<>();
                    for(int i = 0;i < jsonArray.length();i++)
                    {
                        String jsonItem = jsonArray.getString(i);
                        Gson gson = new Gson();
                        UserAccount account = gson.fromJson(jsonItem,UserAccount.class);
                        accountList.add(account);
                    }
                }
            } catch (JSONException e) {
                log(e.getMessage());
            }
        }

        if(accountList != null && accountList.size() > 0)
        {
            String [] content = new String[accountList.size()];
            for(int i = 0;i < accountList.size();i++)
            {
                content[i] = accountList.get(i).getUsername();
            }
            arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,content);
            mUsernameView.setAdapter(arrayAdapter);
        }

        if(username != null && username.length() >0)
        {
            mUsernameView.setText(username);
            mPasswordView.setText(password);
        }else {
            if(accountList != null && accountList.size()> 0)
            {
                UserAccount account = accountList.get(accountList.size() - 1);
                mUsernameView.setText(account.getUsername());
                mPasswordView.setText(account.getPassword());
            }else
            {

            }
        }

        Button btn_login = (Button) findViewById(R.id.email_sign_in_button);
        if(btn_login != null)
            btn_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String username = mUsernameView.getText().toString();
                    String password = mPasswordView.getText().toString();
                    CampusNetLogin login = CampusNetLogin.getInstance(MainActivity.this);
                    login.setOnPostFinishListener(MainActivity.this);
                    SharedPreferences.Editor et = sp.edit();
                    et.putString("username", username);
                    et.putString("password", password);
                    et.commit();
                    login.login(username, password);
                    write2SD1(new UserAccount(username,password,true));

                    progressDialog.show();
                }
            });

        mUsernameView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String username = arrayAdapter.getItem(position);
                for(UserAccount account:accountList)
                {
                    if(account.getUsername().equals(username))
                    {
                        mPasswordView.setText(account.getPassword());
                    }
                }
            }
        });

        progressDialog = new MaterialDialog.Builder(MainActivity.this).
                title("登录"+username).content("正在登陆，请稍后...")
                .progress(true,100,true)
                .build();
        infoDialog =  new MaterialDialog.Builder(MainActivity.this)
                .title("服务器返回值")
                .customView(R.layout.activity_result, false)
                .positiveText("注销").onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Toast.makeText(getApplicationContext(),"注销",Toast.LENGTH_SHORT).show();
                    }
                }).build();

        ConnectionCheck connectionCheck = ConnectionCheck.getInstance();
        final long startTime = System.currentTimeMillis();
        connectionCheck.check(new ConnectionCheck.ConnectionCheckListener() {
            @Override
            public void onConnectionCheck(boolean isConnect) {
                long delayTime = System.currentTimeMillis() - startTime;
                if(isConnect)
                    Toast.makeText(getApplicationContext(),"网络延迟:"+delayTime+"ms，网络状况良好，high起来...",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(),"您的网络貌似不给力...",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPostFinishListener(final String username, final int flag, final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                intent.putExtra("username", username);
//                intent.putExtra("info", info);
//                startActivity(intent);
                progressDialog.dismiss();
                View root = infoDialog.getCustomView();
                TextView tv_info = (TextView) root.findViewById(R.id.info);
//                if(tv_info != null)
//                    tv_info.setText(username+"\n"+info);
                if(flag ==CampusNetLogin.SUCCESS)
                {
                    tv_info.setText(username+"\n校园网登陆成功，尽情看片吧...");
                }else
                {
                    tv_info.setText(username+"\n登陆失败，错误信息如下:\n"+info);
                }
                infoDialog.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        long time = System.currentTimeMillis();
        if (time - lastTime > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出", Toast.LENGTH_SHORT).show();
            lastTime = time;
        } else {
            super.onBackPressed();
        }
    }

    @TargetApi(19)
    public void write2SD1(UserAccount accout)
    {
        try {
            File file = Environment.getExternalStorageDirectory();
            //创建文件夹、文件
            String path = file.getAbsolutePath() + "/WhutCamput/account.json";
            File jsonFile = new File(path);

            //读数据
            Gson gson = new Gson();
            String jsonStr = readFromSD();
            JSONObject jsonObject = null;
            JSONArray jsonArray = null;
            if (jsonStr.length() > 0) {
                jsonObject = new JSONObject(jsonStr);
                jsonArray = jsonObject.getJSONArray("account");

            } else {
                jsonObject = new JSONObject();
                jsonArray = new JSONArray();
            }


            for (int i = 0; i < jsonArray.length(); i++)
            {
                String item = jsonArray.getString(i);
                Gson temp = new Gson();
                UserAccount account = temp.fromJson(item,UserAccount.class);
                if(account.getUsername().equals(accout.getUsername()))
                {
                    jsonArray.remove(i);
                    break;
                }
            }
            jsonArray.put(gson.toJson(accout));

            jsonObject.put("account",jsonArray);
            log(jsonStr);

            //写数据
            FileOutputStream outputStream = new FileOutputStream(jsonFile);
            outputStream.write(jsonObject.toString().getBytes());
            outputStream.close();

        }catch (Exception e) {
            log("exception");
        }
    }

    public String readFromSD()
    {
        String jsonStr = "";
        try {
            File file = Environment.getExternalStorageDirectory();
            //创建文件夹、文件
            String path = file.getAbsolutePath()+"/WhutCamput";
            File dir = new File(path);
            if(!dir.exists())
            {
                dir.mkdir();
            }
            File jsonFile = new File(path+"/account.json");
            if(!jsonFile.exists())
                jsonFile.createNewFile();

                FileInputStream inputStream = new FileInputStream(jsonFile);
            byte [] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);

            jsonStr += new String(buffer);
        } catch (IOException e) {
            log("IOException");
        }

        return jsonStr;
    }


    public void log(String info){
        Log.e(getPackageName(),info);
    }
}
