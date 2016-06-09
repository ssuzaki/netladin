package jp.codepanic.netladin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.os.Handler;

public class Server extends AsyncTask<Void, Integer, ServerInfo> {

	final String URL		= "http://yp.ladio.net/stats/server.dat";
	final String PASSWORD 	= "Password: ";

	public static final int MSG_CONNECT				= 100;
	public static final int MSG_DISCONNECT			= 101;
	public static final int MSG_ERROR_ID_PASS		= 102;
	public static final int MSG_ERROR_MOUNT_USE		= 103;
	public static final int MSG_ERROR_MOUNT_LONG	= 104;
	public static final int MSG_ERROR_NOT_SUPPORT	= 105;
	public static final int MSG_ERROR_OVER_CONNECT	= 106;
	public static final int MSG_ERROR_ETC			= 0xFFFF;
	
	public Socket 			_sock 	= null;
	public BufferedWriter	_writer = null;
	public BufferedReader	_reader = null;	// データ受信用
	public DataOutputStream	_dos	= null;
	
	MainActivity _main = null;
	String		_password;
	Handler		_handler = null;
	boolean		_isConnected = false;

	public Server(MainActivity activity){
		_main = activity;
	}
	
	public ServerInfo getServer(String url)
	{
		ArrayList<ServerInfo> array = new ArrayList<ServerInfo>();
		
	    try
	    {
	        HttpGet method = new HttpGet( url );

	        DefaultHttpClient client = new DefaultHttpClient();

	        // ヘッダを設定する
//	        method.setHeader( "Connection", "Keep-Alive" );
	        
	        HttpResponse response = client.execute( method );
	        int status = response.getStatusLine().getStatusCode();
	        if ( status != HttpStatus.SC_OK )
	            return null;
	        
	        String data = EntityUtils.toString( response.getEntity(), "Shift_JIS" /*"UTF-8"*/ );
	        
	        String[] lines = data.split("\n");
	        
	        boolean lineServer = false;
	        for(String line : lines){
	        	if( line.indexOf(PASSWORD) >= 0 ){
	        		_password = line.split(PASSWORD)[1];
	        	}else if(line.equals("")){
	        		lineServer = true;
	        	}else if(lineServer){
	        		array.add(new ServerInfo(line));
	        	}
	        }
	        
	        //
	        // 混雑してないサーバーを自動選択
	        //
	        int minBusy = Integer.MAX_VALUE;;
	        int index = -1;
	        
	        for(int i = 0; i < array.size(); i++){
	        	ServerInfo info = array.get(i);
	        	
	        	// 値が 0 の場合は特殊な状態(ポートが落ちている/メンテ中/パスワードが異なる) を表します。
	        	if(info._busy < minBusy && info._busy != 0){
	        		minBusy = info._busy;
	        		index = i;
	        	}
	        }
	        
	        if(index != -1){
	        	return array.get(index);
	        }
	        
	        return null;
	    }
	    catch ( Exception e )
	    {
	        return null;
	    }
	}

	@Override
	protected ServerInfo doInBackground(Void... params) {
		ServerInfo info = getServer(URL);
		
		connect( info._host, info._port );
		
		return info;
	}

	@Override
	protected void onPostExecute(ServerInfo info) {
//		super.onPostExecute(result);
		
//		if(_main != null)
//			_main.connect( info._host, info._port );
	}
	
	
	public void connect(String host, int port){
		
		String mount	= _main._textMount.getText().toString().trim();
		String title	= _main._textTitle.getText().toString().trim();
		String desc		= _main._textDesc.getText().toString().trim();
		String gnl		= _main._textGnl.getText().toString().trim();
		String dj		= _main._textDJ.getText().toString().trim();
		String url		= _main._textURL.getText().toString().trim();
		
		try {
			//
			// 接続
			//
			_sock = new Socket(host, port);
			
			_dos = new DataOutputStream( _sock.getOutputStream() );	// 音声用
			
			//
			// ヘッダ送信
			//
			String encDJ 	= URLEncoder.encode(dj, 	"Shift_JIS");
			
			String header = 
			String.format(	"SOURCE %s ICE/1.0\r\n", 		mount) 	+
							"Content-Type: audio/mpeg\r\n"			+
							"User-Agent: 1.0 (Android)\r\n"			+
							"Authorization: Basic c291cmNlOmxhZGlv\r\n"	+
			String.format(	"ice-name: %s\r\n", 			title)	+
			String.format(	"ice-genre: %s\r\n",			gnl)	+
			String.format(	"ice-description: %s\r\n",		desc)	+
			String.format(	"ice-url: %s\r\n",				url)	+
							"ice-bitrate: 32\r\n" +
							"ice-public: 0\r\n" +
							"ice-audio-info:ice-samplerate=22050;ice-bitrate=32;ice-channels=1\r\n" +
			String.format(	"x-ladio-info:charset=sjis;dj=%s\r\n", encDJ) +
							"\r\n";
			
			_writer = new BufferedWriter(new OutputStreamWriter(_sock.getOutputStream(), "Shift_JIS"));
			_writer.write(header);
			_writer.flush();
			
			//
			// 結果受信
			//
			_reader = new BufferedReader(new InputStreamReader(_sock.getInputStream()));
			String msg = _reader.readLine();
			
			String res = null;
			if(msg.indexOf("200 OK") >= 0){
				_isConnected = true;
				
				res = "接続成功";
				sendMsg(MSG_CONNECT);
			}else{
				_isConnected = false;
				
				if (msg.indexOf("401 Authentication Required") >= 0){
//					res = "認証に失敗。ID,Passを確認";
					sendMsg(MSG_ERROR_ID_PASS);
				}else if (msg.indexOf("403 Mountpoint in use") >= 0){
//					res = "マウント名が使用中です。";
					sendMsg(MSG_ERROR_MOUNT_USE);
				}else if (msg.indexOf("403 Mountpoint too long") >= 0){
//					res = "マウント名が不正。/のみ或は48byte-over";
					sendMsg(MSG_ERROR_MOUNT_LONG);
				}else if (msg.indexOf("403 Content-type not supported") >= 0){
//					res = "サポート外のストリーム形式";
					sendMsg(MSG_ERROR_NOT_SUPPORT);
				}else if (msg.indexOf("403 too many sources connected") >= 0){
//					res = "指定ポートの接続上限";
					sendMsg(MSG_ERROR_OVER_CONNECT);
				}else{
					sendMsg(MSG_ERROR_ETC);
				}
			}
			
//			Log.d("unko", msg);
//			Log.d("unko", res);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendMsg(MSG_ERROR_ETC);
		} catch (IOException e) {
			e.printStackTrace();
			sendMsg(MSG_ERROR_ETC);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(!_isConnected)
				clear();
		}
	}
	
	public void clear(){
		
		try {
			if(_reader != null) _reader.close();
			if(_writer != null) _writer.close();
			if(_dos    != null)	_dos.close();
			if(_sock   != null) _sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		_isConnected = false;
	}

	void sendMsg(int msg){
		if(_handler != null)
			_handler.sendEmptyMessage(msg);
	}
	
	public void setHandle(Handler handler) {
		this._handler = handler;
	}	
}
