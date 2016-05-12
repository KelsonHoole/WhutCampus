package cn.hukecn.whutcampus;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class CampusNetLogin {

	public static final int SUCCESS = 0,
			LOCATIONERR = 1,
			ERROR = 2,
			NETERROR = 3;

	public static CampusNetLogin instance = null;
	public String username = null;
	public String password = null;
	String mac = null,ssid = null;
	public PostFinishListener listener;

	private CampusNetLogin(Context context){
		WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wm.getConnectionInfo();
		if(Build.VERSION.SDK_INT >= 23)
			mac = getMac();
		else
			mac = info.getMacAddress();
		ssid = info.getSSID();
	}

	public synchronized static CampusNetLogin getInstance(Context context){
		if(instance == null)
		{
			instance = new CampusNetLogin(context);
		}

		return instance;
	}

	public void setOnPostFinishListener(PostFinishListener listener){
		this.listener = listener;
	}

	public void login(String username,String password){
		this.username = username;
		this.password = password;
		LoginThread mLoginThread = new LoginThread();
		mLoginThread.start();
	}

	public class LoginThread extends Thread{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String url = "http://172.30.16.53/cgi-bin/srun_portal";
			HttpClient client =  new DefaultHttpClient();
			String result = "";
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("action", "login"));
			params.add(new BasicNameValuePair("ac_id", "6"));
			params.add(new BasicNameValuePair("uid", "-1"));
			params.add(new BasicNameValuePair("force", "0"));
			params.add(new BasicNameValuePair("mac", mac));
			params.add(new BasicNameValuePair("is_pad", "0"));
			params.add(new BasicNameValuePair("pop", "-1"));
			params.add(new BasicNameValuePair("is_pop", "1"));
			params.add(new BasicNameValuePair("is_debug", "1"));
			params.add(new BasicNameValuePair("type", "1"));
			//此处进行教学区还是宿舍区的判断
			if(ssid.equals("\"WHUT-WLAN\""))
				params.add(new BasicNameValuePair("nas_ip", "172.30.12.247"));
			else
				params.add(new BasicNameValuePair("nas_ip", "172.30.12.243"));

			params.add(new BasicNameValuePair("page_succeed", "http://172.30.16.53/help.html"));
			params.add(new BasicNameValuePair("page_logout", "http://www.whut.edu.cn"));
			params.add(new BasicNameValuePair("page_error", "http://172.30.16.53/help.html"));
			params.add(new BasicNameValuePair("page_error", "/ac_detect.php"));

			params.add(new BasicNameValuePair("username", username+""));
			params.add(new BasicNameValuePair("password", password+""));
			params.add(new BasicNameValuePair("save_me", "1"));

			try {
				HttpEntity entity;
				entity = new UrlEncodedFormEntity(params,HTTP.UTF_8);
				HttpPost post = new HttpPost(url);
				post.setEntity(entity);
				client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
				HttpResponse httpResponse = client.execute(post);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
				{
					result = EntityUtils.toString(httpResponse.getEntity(),HTTP.UTF_8);
					if(listener != null){
						if(result.contains("您已在线"))
							listener.onPostFinishListener(username, SUCCESS,result);
						else
							listener.onPostFinishListener(username, NETERROR,"请求错误");
					}
				}else
				{
					if(listener != null)
						listener.onPostFinishListener(username, NETERROR,"请求错误");
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				if(listener != null)
					listener.onPostFinishListener(username, NETERROR,"UnsupportedEncodingException");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				if(listener != null)
					listener.onPostFinishListener(username, NETERROR,"ClientProtocolException");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if(listener != null)
					listener.onPostFinishListener(username, NETERROR,"IOException");
			}
		}
	}

	public interface PostFinishListener{
		void onPostFinishListener(String username,int flag,String info);
	}
	/**
	 * 获取手机的MAC地址
	 * @return
	 */
	public String getMac(){
		String str="";
		String macSerial="";
		try {
			Process pp = Runtime.getRuntime().exec(
					"cat /sys/class/net/wlan0/address ");
			InputStreamReader ir = new InputStreamReader(pp.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);

			for (; null != str;) {
				str = input.readLine();
				if (str != null) {
					macSerial = str.trim();// 去空格
					break;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (macSerial == null || "".equals(macSerial)) {
			try {
				return loadFileAsString("/sys/class/net/eth0/address")
						.toUpperCase().substring(0, 17);
			} catch (Exception e) {
				e.printStackTrace();

			}

		}
		return macSerial;
	}
	public static String loadFileAsString(String fileName) throws Exception {
		FileReader reader = new FileReader(fileName);
		String text = loadReaderAsString(reader);
		reader.close();
		return text;
	}
	public static String loadReaderAsString(Reader reader) throws Exception {
		StringBuilder builder = new StringBuilder();
		char[] buffer = new char[4096];
		int readLength = reader.read(buffer);
		while (readLength >= 0) {
			builder.append(buffer, 0, readLength);
			readLength = reader.read(buffer);
		}
		return builder.toString();
	}
}
