package vlc_xugg_tests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.caprica.vlcj.binding.LibC;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.provider.DirectoryProviderDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.BaseNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.LinuxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

public class VlcjTutor
{
	final static String[] F = {"libvlc\\.so(?:\\.\\d)*",
	        "libvlccore\\.so(?:\\.\\d)*"};
	final static String[] D = {"%s"};
	final static String DIR = "//usr//lib//";
	public static void main(String[] args) 
	{
		MyNativeDiscoveryStrategy lnds = new MyNativeDiscoveryStrategy(F,D);
		System.out.println(lnds.discover());
		if(lnds.discover() != null)
		{
			System.out.println("-------------------------");
			for(String path : lnds.discoveryDirectories())
			{			
				System.out.println(path);
				if(lnds.onSetPluginPath(path))
				{
					System.out.println("Setted");
				}
			}
			System.out.println("-------------------------");
		}
		NativeDiscovery nd = new NativeDiscovery(lnds);
		if(nd.discover())
		{
			System.out.println("Found at" + nd.discoveredPath());
		}
		else
		{
			System.err.println("No match found");
			System.exit(1);
		}
		EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
	}
		/*Pattern[] patternsToMatch = new Pattern[F.length];
        for (int i = 0; i < F.length; i++) {
            patternsToMatch[i] = Pattern.compile(F[i]);
        }
        
		 File dir = new File(D);
	        if (!dir.exists()) {
	            System.err.println("No directory");
	            
	        }
	        File[] files = dir.listFiles();
	        if (files != null) {
	            Set<String> matches = new HashSet<String>(F.length);
	            for (File file : files) {
	                for (Pattern pattern : patternsToMatch) {
	                    Matcher matcher = pattern.matcher(file.getName());
	                    if (matcher.matches()) {
	                        // A match was found for this pattern (note that it may be possible to match multiple times, any
	                        // one of those matches will do so a Set is used to ignore duplicates)
	                        matches.add(pattern.pattern());
	                        System.out.println(pattern);
	                        if (matches.size() == F.length) {
	                            System.out.println("Reached");
	                        }
	                    }
	                }
	            }
	        }
	}*/
	
}

class MyNativeDiscoveryStrategy extends BaseNativeDiscoveryStrategy 
{
	public MyNativeDiscoveryStrategy(String[] filenamePatterns, String[] pluginPathFormats)
	{
		super(filenamePatterns, pluginPathFormats);
	}
	@Override
	public boolean supported()
	{
		return true;
	}
	@Override
	protected boolean setPluginPath(String pluginPath)
	{
		 return LibC.INSTANCE.setenv(PLUGIN_ENV_NAME, pluginPath, 1) == 0;
	}
	@Override
	protected List<String> discoveryDirectories()
	{
		List<String> dirs = new ArrayList<String>() {{add("//usr//lib//");}};
		return dirs;
	}
	@Override
    public boolean onFound(String path)
	{
        return true;
    }
}