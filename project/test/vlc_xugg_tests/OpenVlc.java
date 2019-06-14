package vlc_xugg_tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class opens VLC
 * @author Andrea Bugin ad Ilie Sarpe
 *
 */

public class OpenVlc 
{
	public static void main(String[] args) throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		String str = "cvlc -vvv media/media_cache/tesserato.mp4 --sout '#transcode{vcodec=h264,acodec=mp3,samplerate44100}:std{access=http,mux=ffmpeg{mux=flv},dst=localhost:23456/tesserato.mp4}'";
		String command1 = "echo $PWD";
	    String s;
		Process pr = rt.exec(new String[]{"bash","-c",str});
		BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		while ((s = br.readLine()) != null)
			System.out.println("line: " + s);
	}
}
