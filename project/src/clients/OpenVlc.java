package clients;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class OpenVlc 
{
	public static void main(String[] args) throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		String str = "cvlc -vvv media/media_cache/tesserato.mp4 --sout '#transcode{vcodec=h264,acodec=mp3,samplerate44100}:std{access=http,mux=ffmpeg{mux=flv},dst=localhost:23456/tesserato.mp4}'";
		String command1 = "echo $PWD";
		//ProcessBuilder pb = new ProcessBuilder("cvlc", "-vvv", " tesserato.mp4", " --sout",  "'#transcode{vcodec=h264,acodec=mp3,samplerate44100}:std{access=http,mux=ffmpeg{mux=flv},dst=localhost:23456/tesserato.mp4}'");
		//String output = IOUtils.toString(pb.start().getInputStream());
		//pb.directory(new File("media/media_cache/"));
		//String output = IOUtils.toString(pb.start().getInputStream());
		//System.out.println(output);
		//Process p = pb.start();
		/*Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
	      Matcher m = p.matcher(str);
	 
	      System.out.println(str);
	      int count = 0;
	      while (m.find()) 
	      {
	         count = count+1;
	         System.out.println("position "  + m.start() + ": " + str.charAt(m.start()));
	      }
	      System.out.println("There are " + count + " special characters");*/
	   //}
	      String s;
		Process pr = rt.exec(new String[]{"bash","-c",str});
		BufferedReader br = new BufferedReader(
                new InputStreamReader(pr.getInputStream()));
            while ((s = br.readLine()) != null)
                System.out.println("line: " + s);
	}
}
