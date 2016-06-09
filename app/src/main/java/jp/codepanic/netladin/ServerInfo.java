package jp.codepanic.netladin;

public class ServerInfo {
	
	public String 	_host;		// std1.ladio.net
	public int		_port;		// 8000
	public int		_busy;		// 混雑度（この数値が大きい程混雑したポートという意味になります。ただし、値が 0 の場合は特殊な状態(ポートが落ちている/メンテ中/パスワードが異なる) を表します。）
	
	public ServerInfo(String line){
		String[] params = line.split("\t");
		
		_host	= params[0].split(":")[0];
		_port	= Integer.parseInt(params[0].split(":")[1]);
		_busy	= Integer.parseInt( params[1] );
	}
}
