package jp.codepanic.netladin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class VisualizerView extends View {
	short[] mBytes;
	int		mSize;
	float[] mPoints;
	Rect mRect = new Rect();
	
	Paint mForePaint = new Paint();
	
	boolean _isActive = false;
	
	public VisualizerView(){
		super(null);
	}
	
	public VisualizerView(Context context){
		super(context);
		init();
	}
	
	public VisualizerView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	void init(){
		mBytes = null;
		
		// 線の太さ
		mForePaint.setStrokeWidth(3f);
		
		// アンチエイリアス有効に
		mForePaint.setAntiAlias(true);
		
		// 色
		mForePaint.setColor(Color.BLACK);
	}
	
//	public void updateVisualizer(byte[] buffer/*, int readsize*/){
//		
////		Log.d("unko", String.valueOf(readsize));
//		
//		mBytes = buffer;
////		mBytes = new byte[256];
//
////		for(int i = 0; i < mBytes.length; i++){
////			mBytes[i] = (byte) 128;
////		}
////		for(int i = 0; i < readsize; i++){
////			int val = buffer[i] + 128;
////			mBytes[val] ++;
////		}
////		for(int i = 0; i < mBytes.length; i++){
////			mBytes[i] /= 8;
////		}
//		
//		// 再描画
////		invalidate();
//		
//		handler.post(new Runnable() {
//			
//			@Override
//			public void run() {
//				invalidate();
//			}
//		});
//	}

	private Handler handler = new Handler();
	public void updateVisualizer(short[] buffer, int size){
		
//		Log.d("unko", String.valueOf(size) + " / " + String.valueOf(buffer.length));
		
		mBytes 	= buffer.clone();
		mSize	= size;
		
		handler.post(new Runnable() {
		
			@Override
			public void run() {
				invalidate();
			}
		});
	}
	
	public void reset(){
		mBytes = null;
		mSize = 0;
		invalidate();
	}

	public void start(){
		_isActive = true;
	}
	
	public void stop(){
		_isActive = false;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

//		canvas.drawColor(Color.BLACK);
		canvas.drawColor( Color.rgb(0x80, 0x80, 0x00) );
		
		if(mBytes == null || !_isActive)
			return;
		
//		if(mPoints == null || mPoints.length < mBytes.length * 4){
//			mPoints = new float[ mBytes.length * 4 ];
//		}
		
//		for(int i = 0; i < mBytes.length; i++)
//			mBytes[i] = (byte) 200;
		
		// 描画領域を指定
		mRect.set(0, 0, getWidth(), getHeight());
		
//		// 表示するラインの頂点座標を作成
//		for(int i = 0; i < mBytes.length - 1; i++){
//			mPoints[i * 4] 		= mRect.width() *  i      / (mBytes.length - 1);
//			mPoints[i * 4 + 1] 	= mRect.height() / 2 + ((byte)(mBytes[i]     + 128)) * (mRect.height() / 2) / 128;
//			mPoints[i * 4 + 2]	= mRect.width() * (i + 1) / (mBytes.length - 1);
//			mPoints[i * 4 + 3]	= mRect.height() / 2 + ((byte)(mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
//
////			mPoints[i * 4] 		= mRect.width() *  i      / (mBytes.length - 1);
////			mPoints[i * 4 + 1] 	= mRect.height() / 2 + ((short)(mBytes[i]     + 32768)) * (mRect.height() / 2) / 32768;
////			mPoints[i * 4 + 2]	= mRect.width() * (i + 1) / (mBytes.length - 1);
////			mPoints[i * 4 + 3]	= mRect.height() / 2 + ((short)(mBytes[i + 1] + 32768)) * (mRect.height() / 2) / 32768;
//			
////			mPoints[i * 4] 		= mRect.width() *  i      / (mBytes.length - 1);
////			mPoints[i * 4 + 1] 	= (float)mRect.height() / 2f + ((byte)(mBytes[i]     + 128)) * ((float)mRect.height() / 2f) / 128f;
////			mPoints[i * 4 + 2]	= mRect.width() * (i + 1) / (mBytes.length - 1);
////			mPoints[i * 4 + 3]	= (float)mRect.height() / 2f + ((byte)(mBytes[i + 1] + 128)) * ((float)mRect.height() / 2f) / 128f;
//		}
//		
//		// 線描画
//		canvas.drawLines( mPoints, mForePaint );
		
		
		
		int start = 0;
		int y;
		int baseLine = mRect.height() / 2;
		int rate = 4;
		int oldX = 0;
		int oldY = baseLine;
		for(int i = 0; i < mSize; i++){
			int x = (int)((float)mRect.width() / (float)mSize * (float)i + (float)start);
			y = mBytes[i] / 128 + baseLine;
			
			canvas.drawLine(oldX, oldY, x, y, mForePaint);
			
			oldX = x;
			oldY = y;
		}
	}

	
}
