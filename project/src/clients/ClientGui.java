package clients;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * This class provides a simple user interface.
 * The user can select a video and play it through VLC
 * 
 * @author Andrea Bugin ad Ilie Sarpe
 *
 */
public class ClientGui
{

	private final static String PROXY_ADDRESS = "localhost";
	private final static int PROXY_PORT = 9856;
	private JFrame frame;

	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					ClientGui window = new ClientGui();
					window.frame.setVisible(true);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ClientGui()
	{
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JComboBox<String> comboBox = new JComboBox<>();
		comboBox.addItem("docking");
		comboBox.addItem("father");
		comboBox.addItem("theone");
		comboBox.setSelectedItem(null);
		
		comboBox.setBounds(143, 67, 189, 24);
		frame.getContentPane().add(comboBox);
		
		JButton btnNewButton = new JButton("Play Video");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String video = (String) comboBox.getSelectedItem();
				if(video == null)
				{
					JOptionPane.showMessageDialog(null, "You have to select one video!");
				}
				else
				{
					try
					{
						Socket proxySock = new Socket(PROXY_ADDRESS, PROXY_PORT);
						Scanner scannerProxy = new Scanner(proxySock.getInputStream());
						PrintWriter pwProxy = new PrintWriter(proxySock.getOutputStream());
						
						System.err.println("Socket connected");
						// sending the request 
						pwProxy.println("get video " + video.toLowerCase());
						pwProxy.flush();
						
						System.err.println("Video is: "+ video);
						
						//reading the response
						String line = null;
						line = scannerProxy.nextLine();
						if(line.equals("200 OK"))
						{
							while(!scannerProxy.hasNext())
							{
								continue;
							}
							line = scannerProxy.nextLine();
							System.err.println("line is: " + line);
							String[] chunks = line.split(" ");
							if(chunks[0].equals("LINK") && chunks[1].equals("AT"))
							{
								String url = chunks[2];
								System.err.println("launching read video on: "  + url);
								readVideo(url);
							}
							else
							{
								JOptionPane.showMessageDialog(null, "Protocol not respected");
							}
						}
						else if(line.equals("404 NOT FOUND"))
						{
							JOptionPane.showMessageDialog(null, "Video not in our databases!");
						}
						
					} catch (IOException ioe)
					{
						ioe.printStackTrace();
					}
				}
			}
		});
		btnNewButton.setBounds(183, 182, 117, 25);
		frame.getContentPane().add(btnNewButton);
	}
	
	/**
	 * Read the video specified in the url
	 * @param url the url of the video
	 * @return true if no errors occur, false otherwise.
	 */
	private boolean readVideo(String url)
	{
		Process pr;
		try
		{
			ProcessBuilder pb = new ProcessBuilder();
			//Runtime rt = Runtime.getRuntime();
			String command = "vlc " + url;
			pb.command("bash","-c",command); // = rt.exec(new String[]{});
			Thread.currentThread();
			Thread.sleep(0);
			pb.start();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			return false;
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return true;
	}
}
