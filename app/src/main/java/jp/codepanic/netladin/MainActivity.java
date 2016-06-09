package jp.codepanic.netladin;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final int	INPUT_MOUNT		= 0;
	public static final int	INPUT_TITLE		= 1;
	public static final int	INPUT_DESC		= 2;
	public static final int	INPUT_GNL		= 3;
	public static final int	INPUT_DJ		= 4;
	public static final int	INPUT_URL		= 5;

	// ユニークなIDを取得するために、R.layout.mainのリソースIDを使います
	private static int NOTIFICATION_ONAIR  = R.layout.activity_main;
	
    //
    // 設定
    //
    SharedPreferences _pref;
    static final String KEY_MOUNT		= "mount";
    static final String KEY_TITLE		= "title";
    static final String KEY_DESC		= "desc";
    static final String KEY_GNL			= "gnl";
    static final String KEY_DJ			= "dj";
    static final String KEY_URL			= "url";

	Server _server = null;
	
	TextView	_textTime;
	TextView	_textMicVolume;
	SeekBar		_barVolume;
	public TextView	_textMount;
	public TextView	_textTitle;
	public TextView	_textDesc;
	public TextView	_textGnl;
	public TextView	_textDJ;
	public TextView	_textURL;
	Button		_btnOnAir;
	ImageView	_imageOnAir;
	
	int 		_micVolume;
	boolean		_isMute = false;
	boolean		_isOnAir = false;
	
	ArrayList<Channel>	_arrayCH 		= new ArrayList<Channel>();	// 全件
	
	int _lisner = 0;
	int _lisnerTotal = 0;
	int _lisnerMax = 0;
	String _strShare = null;
	
	// エミュレータではマイクからの入力サンプリングレートは8KHzしかサポートしていない模様
	private RecMicToMp3 mRecMicToMp3 = new RecMicToMp3(Environment.getExternalStorageDirectory() + "/mezzo.mp3", 22050);
	
	Visualizer _v;
	public static VisualizerView _vv = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 画面向き固定
//		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// ステータスバーを隠す
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// タイトルバーを隠す
//		requestWindowFeature(Window.FEATURE_NO_TITLE);		

		
		setContentView(R.layout.activity_main);
		
		_pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		_textTime	= (TextView)findViewById(R.id.textTime);
		_textMicVolume= (TextView)findViewById(R.id.textMicVolume);
		_barVolume	= (SeekBar)findViewById(R.id.micVolume);
		_textMount	= (TextView)findViewById(R.id.textMount);
		_textTitle	= (TextView)findViewById(R.id.textTitle);
		_textDesc	= (TextView)findViewById(R.id.textDesc);
		_textGnl	= (TextView)findViewById(R.id.textGnl);
		_textDJ		= (TextView)findViewById(R.id.textDJ);
		_textURL	= (TextView)findViewById(R.id.textURL);
		_btnOnAir	= (Button)findViewById(R.id.btnOnAir);
		_imageOnAir = (ImageView)findViewById(R.id.imageOnAir);
		
		_vv = (VisualizerView)findViewById(R.id.visualizer);
		_vv.init();

		mRecMicToMp3.setHandle(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				
				case RecMicToMp3.MSG_REFRESH:
//					_vv.invalidate();
					break;
				
				case RecMicToMp3.MSG_REC_STARTED:
//					if(_v == null){
//						_v = new Visualizer(mRecMicToMp3._audioSessionID);
//						_v.setEnabled(false); // ※これやらないと有効にならない？
//						_v.setCaptureSize( Visualizer.getCaptureSizeRange()[1] );
//						_v.setDataCaptureListener(
//							new OnDataCaptureListener() {
//								
//								@Override
//								public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
//									// TODO 自動生成されたメソッド・スタブ
//									_vv.updateVisualizer(waveform);
//								}
//								
//								@Override
//								public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//									// TODO 自動生成されたメソッド・スタブ
//									
//								}
//							}, Visualizer.getMaxCaptureRate() / 2, true, false);
//						_v.setEnabled(true);
//					}
					break;
				case RecMicToMp3.MSG_REC_STOPPED:
//					_v.setEnabled(false);
					reset();
					break;
				case RecMicToMp3.MSG_ERROR_GET_MIN_BUFFERSIZE:
					toast("録音が開始できませんでした。この端末が録音をサポートしていない可能性があります。");
					break;
				case RecMicToMp3.MSG_ERROR_CREATE_FILE:
					toast("ファイルが生成できませんでした");
					break;
				case RecMicToMp3.MSG_ERROR_REC_START:
					toast("録音が開始できませんでした");
					break;
				case RecMicToMp3.MSG_ERROR_AUDIO_RECORD:
					toast("録音ができませんでした");
					break;
				case RecMicToMp3.MSG_ERROR_AUDIO_ENCODE:
					toast("エンコードに失敗しました");
					break;
				case RecMicToMp3.MSG_ERROR_WRITE_FILE:
					toast("ファイルの書き込みに失敗しました");
					break;
				case RecMicToMp3.MSG_ERROR_CLOSE_FILE:
					toast("ファイルの書き込みに失敗しました");
					break;
					
				default:
					break;
				}
			}
		});
		
		load();
		setupMicVolume();
		changeMicVolume(_micVolume);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
			// ※HOMEボタンを押した場合と同じように終わる
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			startActivity(intent);				

			return true;
		}
		
		return false;
	}
	
	// ※AsyncTaskは使うたびに生成しないと、同じものをexecuteできない
	void setupServer(){
		_server = new Server(this);
		
		_server.setHandle(new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case Server.MSG_CONNECT:
					mRecMicToMp3.start(_server);
//					mRecMicToMp3.startWave();
					return;
				case Server.MSG_ERROR_ID_PASS:
					toast("認証に失敗。ID,Passを確認");
					break;
				case Server.MSG_ERROR_MOUNT_USE:
					toast("マウント名が使用中です。");
					break;
				case Server.MSG_ERROR_MOUNT_LONG:
					toast("マウント名が不正。/のみ或は48byte-over");
					break;
				case Server.MSG_ERROR_NOT_SUPPORT:
					toast("サポート外のストリーム形式");
					break;
				case Server.MSG_ERROR_OVER_CONNECT:
					toast("指定ポートの接続上限");
					break;
				case Server.MSG_ERROR_ETC:
					toast("不明なエラー");
					break;
				default:
					break;
				}
				
				reset();
			}
			
		});
	}
	
	void reset(){
		
		mRecMicToMp3.stop();
		
		if(_server != null)
			_server.clear();
		
		_isOnAir = false;
		_btnOnAir.setText("放送開始");
		
		_imageOnAir.setImageResource(R.drawable.onair_gray);
		
		updateControl(_isOnAir);		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
//		_v.release();
		
		mRecMicToMp3.stop();

		if(_server != null)
			_server.clear();
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
	
	public void onClickOnAir(View view){
		
		_strShare = null;
		
		if(_isOnAir){
			stopTimer();
			
			_server.clear();

			_vv.stop();
			mRecMicToMp3.stop();
			_isOnAir = false;
			_btnOnAir.setText("放送開始");
			
			cancelNotifyGoing();
			toast("放送終了");

			_imageOnAir.setImageResource(R.drawable.onair_gray);
		}else{
			if(!checkStart()){
				toast("関連URL以外は必須入力です");
				return;
			}
			
			_lisner = 0;
			_lisnerTotal = 0;
			_lisnerMax = 0;
			_sec = 0;
			updateHeader();
			
			startTimer();
			
			setupServer();
			_server.execute();
			
			_vv.start();
			_isOnAir = true;
			_btnOnAir.setText("放送中");

			notifyGoing("放送中");
			toast("放送中");
			
			_imageOnAir.setImageResource(R.drawable.onair);
		}
		
		updateControl(_isOnAir);
	}
	
	public void onClickShare(View view){
		if(_isOnAir && _strShare != null && !_strShare.isEmpty()){
	        try {
	            Intent intent = new Intent();
	            intent.setAction(Intent.ACTION_SEND);
	            intent.setType("text/plain");
	            intent.putExtra(Intent.EXTRA_TEXT, _strShare);
	            startActivity(intent);
	        } catch (Exception e) {
	        }
		}else{
			toast("放送開始後、番組情報に反映されるまでお待ちください。");
		}
	}
	
	boolean checkStart(){
		String mount	= _textMount.getText().toString().trim();
		String title	= _textTitle.getText().toString().trim();
		String desc		= _textDesc.getText().toString().trim();
		String gnl		= _textGnl.getText().toString().trim();
		String dj		= _textDJ.getText().toString().trim();
		
		if(mount.isEmpty() || title.isEmpty() || desc.isEmpty() || gnl.isEmpty() || dj.isEmpty())
			return false;
		
		return true;
	}
	
	void updateControl(boolean onAir){
		_textMount.setEnabled(!onAir);
		_textTitle.setEnabled(!onAir);
		_textDesc.setEnabled(!onAir);
		_textGnl.setEnabled(!onAir);
		_textDJ.setEnabled(!onAir);
		_textURL.setEnabled(!onAir);
	}
	
	public void onClickMount(View view){
		showInputDialog("マウント名：半角/で始まる英数字（番組識別用）", INPUT_MOUNT);
	}
	
	public void onClickTitle(View view){
		showInputDialog("タイトル", INPUT_TITLE);
	}
	
	public void onClickDesc(View view){
		showInputDialog("放送内容", INPUT_DESC);
	}
	
	public void onClickGnl(View view){
		showInputDialog("ジャンル", INPUT_GNL);
	}
	
	public void onClickDJ(View view){
		showInputDialog("ＤＪ：放送者（あなた）の名前", INPUT_DJ);
	}
	
	public void onClickURL(View view){
		showInputDialog("関連URL：番組と関連する掲示板などのURL", INPUT_URL);
	}
	
	public void onClickMute(View view){
//		AudioManager man = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//		
//		man.setMode(AudioManager.MODE_IN_CALL);
//		
//		if(man.isMicrophoneMute()){
//			man.setMicrophoneMute(false);
//			Log.d("unko", "false");
//		}else{
//			man.setMicrophoneMute(true);
//			Log.d("unko", "true");
//		}
		
		int old = _micVolume;
		
		if(_isMute){
			_barVolume.setProgress(_micVolume);
			changeMicVolume(_micVolume);
			_isMute = false;
		}else{
			_barVolume.setProgress(0);
			changeMicVolume(0);
			_isMute = true;
		}
		
		_micVolume = old;
	}
	
	
	String showInputDialog(String title, final int type){
		String ret = "";
		
		final EditText editView = new EditText(this);
		
		final AlertDialog dlg = new AlertDialog.Builder(this)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle( title )
		    .setView(editView)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	
		        	String value = editView.getText().toString().trim();
		        	
//		        	if(value.length() <= 0)
//		        		return;
		        	
		        	switch(type){
		        	case INPUT_MOUNT:	_textMount.setText(value);	break;
		        	case INPUT_TITLE:	_textTitle.setText(value);	break;
		        	case INPUT_DESC:	_textDesc.setText(value);	break;
		        	case INPUT_GNL:		_textGnl.setText(value);	break;
		        	case INPUT_DJ:		_textDJ.setText(value);		break;
		        	case INPUT_URL:		_textURL.setText(value);	break;
		        	}
		        	
		        	save();
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    })
		    .create();
		
		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				
				switch(type){
				case INPUT_MOUNT:
					if(_textMount.getText().toString().trim().length() <= 1)
						editView.setText("/");
					else
						editView.setText( _textMount.getText().toString().trim() );
					editView.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
					break;

				case INPUT_TITLE:
					editView.setText( _textTitle.getText().toString().trim() );
					break;
					
				case INPUT_DESC:
					editView.setText( _textDesc.getText().toString().trim() );
					break;
					
				case INPUT_GNL:
					editView.setText( _textGnl.getText().toString().trim() );
					break;
					
				case INPUT_DJ:
					editView.setText( _textDJ.getText().toString().trim() );
					break;
					
				case INPUT_URL:
					editView.setText( _textURL.getText().toString().trim() );
					editView.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
					break;
				}
				
            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            	editView.setSelection( editView.getText().toString().trim().length() );
			}
		});
		
		dlg.show();
		
		return ret;
	}
	
	void setupMicVolume(){
		_micVolume = 100;
		
		_barVolume.setMax(200);
		_barVolume.incrementProgressBy(10);
		_barVolume.setProgress(_micVolume);
		
		_barVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				_micVolume = progress;
				changeMicVolume(progress);
				
				if(progress > 0)
					_isMute = false;
			}
		});
	}
	
	void changeMicVolume(int vol){
		mRecMicToMp3._micVolume = vol;
		
		_textMicVolume.setText( String.valueOf(vol) + "%" );
	}
	
	void save(){
		Editor e = _pref.edit();
		
		e.putString(KEY_MOUNT, 	_textMount.getText().toString().trim());
		e.putString(KEY_TITLE, 	_textTitle.getText().toString().trim());
		e.putString(KEY_DESC, 	_textDesc.getText().toString().trim());
		e.putString(KEY_GNL, 	_textGnl.getText().toString().trim());
		e.putString(KEY_DJ, 	_textDJ.getText().toString().trim());
		e.putString(KEY_URL, 	_textURL.getText().toString().trim());
		
		e.commit();
	}
	
	void load(){
		_textMount.setText(	_pref.getString(KEY_MOUNT, "") );
		_textTitle.setText(	_pref.getString(KEY_TITLE, "") );
		_textDesc.setText(	_pref.getString(KEY_DESC, "ねとらじんで放送中！") );
		_textGnl.setText(	_pref.getString(KEY_GNL, "") );
		_textDJ.setText(	_pref.getString(KEY_DJ, "") );
		_textURL.setText(	_pref.getString(KEY_URL, "") );
	}
	
//  private Handler handlerToast = new Handler();
    void toast(final String msg){
    	
    	// ※表示元がAlerm？だからトースト消えない対策
//    	handlerToast.post( new Runnable() {
//
//			@Override
//			public void run() {
		    	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
//			}
//    		
//    	});
    }
	
	public void cancelNotifyGoing(){
		NotificationManager man = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		man.cancel(NOTIFICATION_ONAIR);
	}
	
	void notifyGoing(String msg){
		Notification n = new Notification();
		n.icon = R.drawable.ic_launcher;
		n.tickerText = msg;
		n.flags = Notification.FLAG_ONGOING_EVENT;
		
		Intent i = new Intent(getApplicationContext(), MainActivity.class);
		PendingIntent pend = PendingIntent.getActivity(this, 0, i, 0);
		n.setLatestEventInfo(
			getApplicationContext(),
			"ねとらじん", 
			msg, 
			pend);

		//NotificationManagerへNotificationインスタンスを設定して発行！
		NotificationManager man = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//		man.cancel(NOTIFICATION_ONAIR);
		man.notify(NOTIFICATION_ONAIR, n);		    
	}	
	
	
	
	
	int		_sec = 0;
	Timer	_timer = null;	//new Timer(true);	// デーモンスレッドにする。プログラム終了時にスレッド終了待たない
	Handler handlerSleep = new Handler();
	
	class OnAirTimerTask extends TimerTask{

		@Override
		public void run() {
			
			handlerSleep.post(new Runnable() {
				
				@Override
				public void run() {
					
					_sec ++;
					
					if(_sec % 60 == 0){
						
						new AsyncTask<Void, Void, String>() {

							@Override
							protected String doInBackground(Void... params) {
								refresh();
								return null;
							}
							
						}.execute();
						
					}
					
					updateHeader();
				}
			});
		}
		
	}
	
	void updateHeader(){
		int hour= _sec / 3600;
		int min = (_sec - (hour * 3600)) / 60;
		int sec = _sec - (hour * 3600) - (min * 60);
		
		String time = String.valueOf(hour) + ":" + String.format("%1$02d", min) + ":" + String.format("%1$02d", sec);
		
		String lisner = 
			"リスナ数 " + String.valueOf(_lisner) + "  " +
			"最大 " 	+ String.valueOf(_lisnerMax) + "  " +
			"延べ "		+ String.valueOf(_lisnerTotal);
		
		_textTime.setText("放送時間 " + time + "  /  " + lisner);
	}
	
	void startTimer(){
		OnAirTimerTask task = new OnAirTimerTask();
    	
    	_timer = new Timer(true);
    	_timer.schedule(task, 1000, 1000);	// １秒後に１秒間隔で
		
	}
	
	void stopTimer(){
		if(_timer != null){
			_timer.cancel();
			_timer = null;
		}
		
		_sec = 0;
		
		updateHeader();
	}
	
	void refresh(){
		
		String mount = _textMount.getText().toString().trim();
		
		String list = getList("http://yp.ladio.net/stats/list.v2.zdat");
		
		if(!list.isEmpty()){
			_arrayCH.clear();
			
			String[] array = list.split("\n\n");
			
			for(String data : array){
				
				Channel ch = new Channel(data);
				
				if(ch._MNT.equalsIgnoreCase(mount)){
					_lisner 	= Integer.parseInt(ch._CLN);
					_lisnerTotal= Integer.parseInt(ch._CLNS);
					_lisnerMax	= Integer.parseInt(ch._MAX);
					_strShare	= ch.getShare();
					
					break;
				}
			}
		}
	}
	
    private String getList(String url) {
        try {
            URL path = new URL(url);
        } catch (MalformedURLException ex) {
//            result_view.setText(result_view.getText() + "URLが不正です\n");
            return "";
        }
        /**
         * HTTP GETリクエスト
         */
        HttpGet httpGet = new HttpGet(url);
//        if (cb_Gzipx.isChecked()) {
//            result_view.setText(result_view.getText() + "gzip圧縮\n");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate");
//        } else {
//            result_view.setText(result_view.getText() + "gzip非圧縮\n");
//        }

        /** 読み込みサイズ */
        int size = 0;
        /** 読み込みバッファ */
        byte[] w = new byte[1024];
        /** 入力ストリーム */
        InputStream in = null;
        /** 受信用ストリーム */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        /**ファイル保存用ストリーム */
        FileOutputStream writefile = null;


        /** コンテンツ取得用HTTPクライアント */
        DefaultHttpClient httpClient = new DefaultHttpClient();
        /**取得結果 */
        HttpResponse execute = null;

        try {//URLへリクエスト
            execute = httpClient.execute(httpGet);

//            switch (execute.getStatusLine().getStatusCode()) {
//                case HttpStatus.SC_OK:
//                    result_view.setText(result_view.getText() + "Status 200 OK (HTTP/1.0 - RFC 1945)\n");
//                    break;
//                case HttpStatus.SC_MOVED_PERMANENTLY:
//                    result_view.setText(result_view.getText() + "Status 301 Moved Permanently (HTTP/1.0 - RFC 1945)\n");
//                    return "";
//                case HttpStatus.SC_MOVED_TEMPORARILY:
//                    result_view.setText(result_view.getText() + "Status 302 Moved Temporarily (Sometimes Found) (HTTP/1.0 - RFC 1945)\n");
//                    return "";
//                case HttpStatus.SC_NOT_FOUND:
//                    result_view.setText(result_view.getText() + "Status 404 Not Found (HTTP/1.0 - RFC 1945)\n");
//                    return "";
//                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
//                    result_view.setText(result_view.getText() + "Status 500 Server Error (HTTP/1.0 - RFC 1945)\n");
//                    return "";
//                case HttpStatus.SC_SERVICE_UNAVAILABLE:
//                    result_view.setText(result_view.getText() + "Status 503 Service Unavailable (HTTP/1.0 - RFC 1945)\n");
//                    return "";
//                default:
//                    result_view.setText(result_view.getText() + "Status " + execute.getStatusLine().getStatusCode() + "\n");
//                    return "";
//            }

        } catch (ClientProtocolException ex) {
        } catch (IOException ex) {
        }
        try {//HttpStatus.SC_OKの場合取得開始
            /** 取得開始時刻 */
            Long stratTime = System.currentTimeMillis();
            
            if(execute == null)
            	return "";

            //gzip転送の有無で切り替え
            if (isGZipHttpResponse(execute)) {
                in = new GZIPInputStream(execute.getEntity().getContent());
            } else {
                in = execute.getEntity().getContent();
            }

            //読み込み処理
            while (true) {
                size = in.read(w);
                if (size <= 0) {
                    break;
                }
                out.write(w, 0, size);
            }
            in.close();
            /**
             * 取得終了時刻
             */
            Long endTime = System.currentTimeMillis();

//            result_view.setText(result_view.getText() + "取得時間:" + (endTime - stratTime) + "ms\n");
            
//            //ファイルに保存
//            if(checkSDCard()){
//                /** 出力ファイルパスの取得 */
//                File dir = Environment.getExternalStorageDirectory();
//                File file = null;
//                if(url.length()-1==url.lastIndexOf("/")){
//                    file=File.createTempFile("test", ".html", dir);
//                }else{
//                    file=new File(dir.getAbsolutePath()+url.substring(url.lastIndexOf("/")));
//                }
//                Log.i(LOG_TAG, url);
//                writefile = new FileOutputStream(file);
//                writefile.write(out.toByteArray());
//                writefile.flush();
//                writefile.close();
//            }
    
        } catch (IOException ex) {
        } catch (IllegalStateException ex) {
        } finally {
            if (in != null) {
                try {
                    in.close();
//                    writefile.close();
                } catch (IOException ex) {
                }
            }
            httpClient.getConnectionManager().shutdown();
        }

        try {
			return out.toString("Shift_JIS");
		} catch (UnsupportedEncodingException e) {
		}
        
        return "";
    }
	
    private boolean isGZipHttpResponse(HttpResponse response) {
        Header header = response.getEntity().getContentEncoding();
        if (header == null) {
            return false;
        }
        String value = header.getValue();
        return (!TextUtils.isEmpty(value) && value.contains("gzip"));
    }
    
}
