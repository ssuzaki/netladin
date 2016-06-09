package jp.codepanic.netladin;

public class Channel {
	
//	static public Favorites 	_fav;
//	static public BlockList 	_block;
//	static public RecordingList	_recording;
	
	public String		_SURL;
	public String		_TIMS;
	public String		_SRV;
	public String		_PRT;
	public String		_MNT;
	public String		_TYPE;
	public String		_NAM;
	public String		_GNL;
	public String		_DESC;
	public String		_DJ;
	public String		_SONG;		// 放送ツールが送信する現在の曲名情報 
	public String		_URL;
	public String		_CLN;
	public String		_CLNS;
	public String		_MAX;
	public String		_BIT;
	public String		_SMPL;
	public String		_CHS;		// 1:mono 2:stereo

	
	public Channel(String data){
		String[] lines = data.split("\n");
		
		for(String line : lines){
			if(line.startsWith("SURL="))
				_SURL = line.replace("SURL=", "");
			else if(line.startsWith("TIMS="))
				_TIMS = line.replace("TIMS=", "");
			else if(line.startsWith("SRV="))
				_SRV = line.replace("SRV=", "");
			else if(line.startsWith("PRT="))
				_PRT = line.replace("PRT=", "");
			else if(line.startsWith("MNT="))
				_MNT = line.replace("MNT=", "");
			else if(line.startsWith("TYPE="))
				_TYPE = line.replace("TYPE=", "");
			else if(line.startsWith("NAM="))
				_NAM = line.replace("NAM=", "");
			else if(line.startsWith("GNL="))
				_GNL = line.replace("GNL=", "");
			else if(line.startsWith("DESC="))
				_DESC = line.replace("DESC=", "");
			else if(line.startsWith("DJ="))
				_DJ = line.replace("DJ=", "");
			else if(line.startsWith("SONG="))
				_SONG = line.replace("SONG=", "");
			else if(line.startsWith("URL="))
				_URL = line.replace("URL=", "");
			else if(line.startsWith("CLN="))
				_CLN = line.replace("CLN=", "");
			else if(line.startsWith("CLNS="))
				_CLNS = line.replace("CLNS=", "");
			else if(line.startsWith("MAX="))
				_MAX = line.replace("MAX=", "");
			else if(line.startsWith("BIT="))
				_BIT = line.replace("BIT=", "");
			else if(line.startsWith("SMPL="))
				_SMPL = line.replace("SMPL=", "");
			else if(line.startsWith("CHS="))
				_CHS = line.replace("CHS=", "");
		}
	}
	
	public String getData(){
		return 
			"SURL=" + _SURL	+ "\n" +
			"TIMS=" + _TIMS	+ "\n" +
			"SRV="  + _SRV	+ "\n" +
			"PRT="  + _PRT	+ "\n" +
			"MNT="  + _MNT	+ "\n" +
			"TYPE=" + _TYPE	+ "\n" +
			"NAM="  + _NAM	+ "\n" +
			"GNL="  + _GNL	+ "\n" +
			"DESC=" + _DESC	+ "\n" +
			"DJ="   + _DJ	+ "\n" +
			"SONG=" + _SONG + "\n" +
			"URL="  + _URL	+ "\n" +
			"CLN="  + _CLN	+ "\n" +
			"CLNS=" + _CLNS	+ "\n" +
			"MAX="  + _MAX	+ "\n" +
			"BIT="  + _BIT	+ "\n" +
			"SMPL=" + _SMPL	+ "\n" +
			"CHS="  + _CHS;
	}
	
	public String getChannel(){
		if(_CHS.equals("1"))	return "MONO";
		if(_CHS.equals("2"))	return "STEREO";
		
		return "unknown";
	}
	
	public String getType(){
		String type = _TYPE.replace("audio", "").replace("application", "");
		
		if(type.equals("/mpeg"))return "mp3";
		if(type.equals("/ogg"))	return "ogg";
		if(type.equals("/aacp"))return "aacp";
		if(type.equals("/acp"))	return "acp";
		if(type.equals("/aac"))	return "aac";
		
		return "unknown";
	}
	
	public boolean isMP3(){
		return getType().equalsIgnoreCase("mp3");
	}
	
	public String getSoundQuality(){
		return _BIT + "kpbs    " + _SMPL + "Hz    " + getChannel() + "    " + getType();
	}
	
	public String getListener(){
		return "現在：" + _CLN + "    /    最大：" + _MAX + "    /    延べ：" + _CLNS;
	}
	
	public String getPlayURL(){
//		return "http://" + _SRV + ":" + _PRT + _MNT + ".m3u";	再生できず
//		return "http://" + _SRV + ":" + _PRT + _MNT + ".mp3";	再生できず
		return "http://" + _SRV + ":" + _PRT + _MNT;
	}
	
	public String getShare(){
		return "放送中：" + _NAM + " " + _SURL  + " #ねとらじ #ねとらじん";
	}
	
//	public boolean isFavorite(){
//		return _fav.isExist(this);
//	}
//	
//	public boolean isBlock(){
//		return _block.isExist(this);
//	}
//	
//	public boolean isRecording(){
//		return _recording.isExist(this);
//	}
}

