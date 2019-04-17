package proxies_frontend;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaViewer;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.demos.VideoImage;
import com.xuggle.xuggler.io.DataInputOutputHandler;
import com.xuggle.xuggler.io.XugglerIO;

import javafx.util.Pair;




public class ProxyFrontend implements Runnable
{
	final static int DEFAULT_PORT = 9856;
	final static int WAIT_TIME = 15000;
	private final static int C_SIZE = 1000; // size of each chunk of the file in bytes
	
	private static HashMap<String, Integer> l2Map; // list of L2 servers <IP, PORT>
	private static ReentrantLock mapLock; // need the lock for the updates on l2 list
	private static List<Socket> sockAlive;
	private static ReentrantLock lockSockets;
	private static Condition socketAvailable;
	
	public static void main(String[] args)
	{
		(new ProxyFrontend()).exec(args);
	}
	
	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		
		sockAlive = new LinkedList<Socket>();
		l2Map = new HashMap<String, Integer>(); 
		mapLock = new ReentrantLock();
		lockSockets = new ReentrantLock();
		socketAvailable = lockSockets.newCondition();
		
		try
		{
			serverSock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Ok, Serversocket <proxy> created!");

			//TODO: need a distributed structure of l2 servers!!
			
			l2Map.put("localhost", 28517); // just for test
			
			//TODO: need to instantiate a class that updates the map.. pings etc..
			
			
		}
		catch(IOException ioe)
		{
			System.err.println("Failed to instatiate the <proxy> serversocket ");
			ioe.printStackTrace();
		}
		
		while (true)
		{
			Socket clientSock = null;
			try
			{
				// TODO: Accept the client only if the size of the list is <= 60K else redirect!
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to served by the proxy!");
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
			
			lockSockets.lock();
			try 
			{
				sockAlive.add(clientSock);
				if(!lockSockets.hasWaiters(socketAvailable))
				{
					(new Thread(this)).start();
				}
				else
				{
					socketAvailable.signal();
				}
			}
			finally
			{
				lockSockets.unlock();
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run()
	{
		Socket clientSock;
		Scanner scannerClient = null;
		PrintWriter pwClient = null;
		CheckerProxy check;
		String myname = Thread.currentThread().getName();
		boolean alive = true;
		
		Scanner scannerCache = null;
		PrintWriter pwCache = null;
		
		while(true)
		{
			System.out.println("I am alive  <" + myname + ">");
			lockSockets.lock();
			try
			{
				while(sockAlive.isEmpty())
				{
					
					try
					{
						if(!socketAvailable.await(WAIT_TIME, TimeUnit.MILLISECONDS))
						{
							// I have to die
							System.out.println("I terminated my existence " + myname);
							alive = false; 
							break;
						}
					}
					catch(InterruptedException ie)
					{
						ie.printStackTrace();
					}
				}
				if(!alive)
				{
					System.out.println("Exiting all " + myname);
					break;
				}
				clientSock = sockAlive.remove(0); // This must exist at this point
				System.out.println("I am handling a connection " + myname);
			}
			finally
			{
				lockSockets.unlock();
			}
			
			try
			{
				String request = null;
				Socket cacheConn = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());
				List<Pair<String, Integer>> updatedlist = new ArrayList<Pair<String, Integer>>(); 
				DataInputStream dis = null;
				DataOutputStream dos = null;
				byte[] buffer = new byte[C_SIZE];

				// get requests 
				while (scannerClient.hasNextLine())
				{
					request = scannerClient.nextLine();
					check = UtilitiesProxyFrontend.parseRequest(request);
					
					if(!check.isCorrect())
					{
						// Malformed request
						pwClient.println(check.getId()); // send error code
						pwClient.flush();
					}
					else
					{
						
						if(check.getType() == 1)
						{
							// GET VIDEO ID_VID
							
							//need to know the available caches
							mapLock.lock();
							try
							{
								for (Map.Entry<String, Integer> entry : l2Map.entrySet())
								{
									// make a copy
									updatedlist.add(new Pair<String, Integer>(entry.getKey(), entry.getValue()));
								}
							}
							finally
							{
								mapLock.unlock();
							}
							
							// Select servers at random -> not the best method but ok for our purposes..
							Collections.shuffle(updatedlist);
							String cacheResponse;
							
							for(Pair<String, Integer>pair : updatedlist)
							{
								// Ask each proxy if it has the resource
								cacheConn = new Socket(pair.getKey(), pair.getValue());
								try
								{
									scannerCache = new Scanner(cacheConn.getInputStream());
									pwCache = new PrintWriter(cacheConn.getOutputStream());
									
									pwCache.println("GET IN_CACHE " + check.getId());
									pwCache.flush();
									
									cacheResponse = scannerCache.nextLine(); // Assume it is alive
									System.out.println("Prima del parsing..");
									if(cacheResponse.toLowerCase().equals("200 ok"))
									{
										System.out.println("Ci passo");
										dis = new DataInputStream(new BufferedInputStream(cacheConn.getInputStream()));
										
										File video = new File("media//media_proxy//" + check.getId());
										FileOutputStream fos = new FileOutputStream(video);
										
										
										int n = -1;
										byte[] chunck = new byte[1000];
										while ((n = dis.read(chunck)) != -1)
										{
											fos.write(chunck, 0, n);
											fos.flush();
										}
										fos.close();
										System.out.println("Ci Passo");
										
										IContainer container = IContainer.make();
										IContainerFormat iContForm = IContainerFormat.make();
										
										if(iContForm.setInputFormat("m4a")>=0)
										{
											System.out.println("Format Found");
										}
										else
										{
											System.out.println("Format not supported");
										}
										IMediaReader mediaReader;
										IMediaViewer mediaViewer;
										IPacket pack; 
										
										if (container.open("media//media_proxy//" + check.getId(), IContainer.Type.READ, iContForm) >= 0)
										{

											// query how many streams the call to open found
											int numStreams = container.getNumStreams();

											// and iterate through the streams to find the first audio stream
											int videoStreamId = -1;
											IStreamCoder videoCoder = null;
											int audioStreamId = -1;
											IStreamCoder audioCoder = null;
											for (int i = 0; i < numStreams; i++)
											{
												// Find the stream object
												IStream stream = container.getStream(i);
												// Get the pre-configured decoder that can decode this stream;
												IStreamCoder coder = stream.getStreamCoder();

												if (videoStreamId == -1
														&& coder.getCodecType() == com.xuggle.xuggler.ICodec.Type.CODEC_TYPE_VIDEO)
												{
													videoStreamId = i;
													videoCoder = coder;
												} else if (audioStreamId == -1
														&& coder.getCodecType() == com.xuggle.xuggler.ICodec.Type.CODEC_TYPE_AUDIO)
												{
													audioStreamId = i;
													audioCoder = coder;
												}
											}
											if (videoStreamId == -1 && audioStreamId == -1)
												throw new RuntimeException(
														"could not find audio or video stream in container: ");

											/*
											 * Check if we have a video stream in this file. If so let's open up our
											 * decoder so it can do work.
											 */
											IVideoResampler resampler = null;
											if (videoCoder != null)
											{
												if (videoCoder.open() < 0)
													throw new RuntimeException(
															"could not open audio decoder for container: ");

												if (videoCoder.getPixelType() != com.xuggle.xuggler.IPixelFormat.Type.BGR24)
												{
													// if this stream is not in BGR24, we're going to need to
													// convert it. The VideoResampler does that for us.
													resampler = IVideoResampler.make(videoCoder.getWidth(),
															videoCoder.getHeight(), com.xuggle.xuggler.IPixelFormat.Type.BGR24,
															videoCoder.getWidth(), videoCoder.getHeight(),
															videoCoder.getPixelType());
													if (resampler == null)
														throw new RuntimeException(
																"could not create color space resampler for: ");
												}
												/*
												 * And once we have that, we draw a window on screen
												 */
												openJavaVideo();
											}

											if (audioCoder != null)
											{
												if (audioCoder.open() < 0)
													throw new RuntimeException(
															"could not open audio decoder for container: ");

												/*
												 * And once we have that, we ask the Java Sound System to get itself
												 * ready.
												 */
												try
												{
													openJavaSound(audioCoder);
												} catch (javax.sound.sampled.LineUnavailableException ex)
												{
													throw new RuntimeException(
															"unable to open sound device on your system when playing back container: ");
												}
											}

											/*
											 * Now, we start walking through the container looking at each packet.
											 */
											IPacket packet = IPacket.make();
											mFirstVideoTimestampInStream = Global.NO_PTS;
											mSystemVideoClockStartTime = 0;
											while (container.readNextPacket(packet) >= 0)
											{
												/*
												 * Now we have a packet, let's see if it belongs to our video stream
												 */
												if (packet.getStreamIndex() == videoStreamId)
												{
													/*
													 * We allocate a new picture to get the data out of Xuggler
													 */
													IVideoPicture picture = IVideoPicture.make(
															videoCoder.getPixelType(), videoCoder.getWidth(),
															videoCoder.getHeight());

													/*
													 * Now, we decode the video, checking for any errors.
													 * 
													 */
													int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
													if (bytesDecoded < 0)
														throw new RuntimeException(
																"got error decoding audio in: ");

													/*
													 * Some decoders will consume data in a packet, but will not be able
													 * to construct a full video picture yet. Therefore you should
													 * always check if you got a complete picture from the decoder
													 */
													if (picture.isComplete())
													{
														IVideoPicture newPic = picture;
														/*
														 * If the resampler is not null, that means we didn't get the
														 * video in BGR24 format and need to convert it into BGR24
														 * format.
														 */
														if (resampler != null)
														{
															// we must resample
															newPic = IVideoPicture.make(
																	resampler.getOutputPixelFormat(),
																	picture.getWidth(), picture.getHeight());
															if (resampler.resample(newPic, picture) < 0)
																throw new RuntimeException(
																		"could not resample video from: ");
														}
														if (newPic.getPixelType() != com.xuggle.xuggler.IPixelFormat.Type.BGR24)
															throw new RuntimeException(
																	"could not decode video as BGR 24 bit data in: ");

														long delay = millisecondsUntilTimeToDisplay(newPic);
														// if there is no audio stream; go ahead and hold up the main
														// thread. We'll end
														// up caching fewer video pictures in memory that way.
														try
														{
															if (delay > 0)
																Thread.sleep(delay);
														} catch (InterruptedException e)
														{
															return;
														}

														// And finally, convert the picture to an image and display it

														mScreen.setImage(com.xuggle.xuggler.Utils.videoPictureToImage(newPic));
													}
												} else if (packet.getStreamIndex() == audioStreamId)
												{
													/*
													 * We allocate a set of samples with the same number of channels as
													 * the coder tells us is in this buffer.
													 * 
													 * We also pass in a buffer size (1024 in our example), although
													 * Xuggler will probably allocate more space than just the 1024
													 * (it's not important why).
													 */
													com.xuggle.xuggler.IAudioSamples samples = com.xuggle.xuggler.IAudioSamples.make(1024,
															audioCoder.getChannels());

													/*
													 * A packet can actually contain multiple sets of samples (or frames
													 * of samples in audio-decoding speak). So, we may need to call
													 * decode audio multiple times at different offsets in the packet's
													 * data. We capture that here.
													 */
													int offset = 0;

													/*
													 * Keep going until we've processed all data
													 */
													while (offset < packet.getSize())
													{
														int bytesDecoded = audioCoder.decodeAudio(samples, packet,
																offset);
														if (bytesDecoded < 0)
															throw new RuntimeException(
																	"got error decoding audio in: ");
														offset += bytesDecoded;
														/*
														 * Some decoder will consume data in a packet, but will not be
														 * able to construct a full set of samples yet. Therefore you
														 * should always check if you got a complete set of samples from
														 * the decoder
														 */
														if (samples.isComplete())
														{
															// note: this call will block if Java's sound buffers fill
															// up, and we're
															// okay with that. That's why we have the video "sleeping"
															// occur
															// on another thread.
															playJavaSound(samples);
														}
													}
												} else
												{
													/*
													 * This packet isn't part of our video stream, so we just silently
													 * drop it.
													 */
													do
													{
													} while (false);
												}

											}
											/*
											 * Technically since we're exiting anyway, these will be cleaned up by the
											 * garbage collector... but because we're nice people and want to be invited
											 * places for Christmas, we're going to show how to clean up.
											 */
											if (videoCoder != null)
											{
												videoCoder.close();
												videoCoder = null;
											}
											if (audioCoder != null)
											{
												audioCoder.close();
												audioCoder = null;
											}
											if (container != null)
											{
												container.close();
												container = null;
											}
											closeJavaSound();
											closeJavaVideo();
											
											
											/*pack = IPacket.make(10000);
											iContainer.setReadRetryCount(10);
											System.out.println("num stream is " + iContainer.getNumStreams());
											System.out.println(iContainer.getStream(1).toString()); 
											System.out.println(iContainer.getStream(0).toString());
											while(iContainer.readNextPacket(pack) == 0)
											{
												System.out.println("Ci sono");
											}*/
											
											//System.out.println("Before error1");
											/*
									        mediaReader = ToolFactory.makeReader(iContainer);
									        //System.out.println("Before error1-2");
									        mediaViewer = ToolFactory.makeViewer(true);
									        System.out.println("Size is " + iContainer.getInputBufferLength());
									        
									        if(mediaReader.addListener(mediaViewer))
									        {
									        	System.out.println("Listener added");
									        }
									        System.out.println(mediaReader.getUrl());
									        
									        //mediaReader.addListener(ToolFactory.makeWriter("output.mp4", mediaReader));
									        
									        while (mediaReader.readPacket() == null) 
									        {
									        	System.out.println("Faccio qualcosa");
									        }*/
									       
									    }
										else
										{
											System.err.println("Failed to open the stream over socket");
										}
										
										/*while((n = dis.read(buffer)) != -1)
										{
											StreamedContent media = new DefaultStreamedContent(dis, "video/risorsa");
											
										}*/
										/*
										com.xuggle.xuggler.io.DataInputOutputHandler StreamManager = new DataInputOutputHandler(dis, dos, true);
										int n = -2;
										while((n = StreamManager.read(buffer, C_SIZE)) > 0)
										{
											System.out.println("Ho letto qualcosa");
											// response to the client
											StreamManager.write(buffer, n);
										}
										if(n == 0)
										{
											//EOF REACHED.. do something
											System.out.println("EOF REACHED");
										}
										else if(n == -1)
										{
											//Error.
											System.err.println("SOMETHING IS WRONG n is <" + n + ">");
										} */
										// create a media writer and specify the output stream

									}
								}
								catch(IOException ioe)
								{
									ioe.printStackTrace();
								}
							}
						}
						else if(check.getType() == 99)
						{
							// END THE CONNECTION
							System.out.println("Ending the connection");
							break;
						}
					}
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			finally
			{
				try 
				{
					clientSock.close();
				}
				catch(IOException ioe2)
				{
					System.out.println("Failed to close the connection");
					ioe2.printStackTrace();
				}
			}
			
		}
	}

	  /**
	   * The audio line we'll output sound to; it'll be the default audio device on your system if available
	   */
	  private static javax.sound.sampled.SourceDataLine mLine;

	  /**
	   * The window we'll draw the video on.
	   * 
	   */
	  private static VideoImage mScreen = null;

	  private static long mSystemVideoClockStartTime;

	private static long mFirstVideoTimestampInStream;
	 private static long millisecondsUntilTimeToDisplay(IVideoPicture picture)
	  {
	    /**
	     * We could just display the images as quickly as we decode them, but it turns
	     * out we can decode a lot faster than you think.
	     * 
	     * So instead, the following code does a poor-man's version of trying to
	     * match up the frame-rate requested for each IVideoPicture with the system
	     * clock time on your computer.
	     * 
	     * Remember that all Xuggler IAudioSamples and IVideoPicture objects always
	     * give timestamps in Microseconds, relative to the first decoded item.  If
	     * instead you used the packet timestamps, they can be in different units depending
	     * on your IContainer, and IStream and things can get hairy quickly.
	     */
	    long millisecondsToSleep = 0;
	    if (mFirstVideoTimestampInStream == Global.NO_PTS)
	    {
	      // This is our first time through
	      mFirstVideoTimestampInStream = picture.getTimeStamp();
	      // get the starting clock time so we can hold up frames
	      // until the right time.
	      mSystemVideoClockStartTime = System.currentTimeMillis();
	      millisecondsToSleep = 0;
	    } else {
	      long systemClockCurrentTime = System.currentTimeMillis();
	      long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - mSystemVideoClockStartTime;
	      // compute how long for this frame since the first frame in the stream.
	      // remember that IVideoPicture and IAudioSamples timestamps are always in MICROSECONDS,
	      // so we divide by 1000 to get milliseconds.
	      long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - mFirstVideoTimestampInStream)/1000;
	      final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
	      millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo -
	          (millisecondsClockTimeSinceStartofVideo+millisecondsTolerance));
	    }
	    return millisecondsToSleep;
	  }

	/**
	 * Opens a Swing window on screen.
	 */
	private static void openJavaVideo()
	{
		mScreen = new VideoImage();
	}

	/**
	 * Forces the swing thread to terminate; I'm sure there is a right way to do
	 * this in swing, but this works too.
	 */
	private static void closeJavaVideo()
	{
		System.exit(0);
	}

	private static void openJavaSound(IStreamCoder aAudioCoder) throws javax.sound.sampled.LineUnavailableException
	{
		javax.sound.sampled.AudioFormat audioFormat = new javax.sound.sampled.AudioFormat(aAudioCoder.getSampleRate(),
				(int) com.xuggle.xuggler.IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()), aAudioCoder.getChannels(),
				true, /* xuggler defaults to signed 16 bit samples */
				false);
		javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, audioFormat);
		mLine = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
		/**
		 * if that succeeded, try opening the line.
		 */
		mLine.open(audioFormat);
		/**
		 * And if that succeed, start the line.
		 */
		mLine.start();

	}

	private static void playJavaSound(com.xuggle.xuggler.IAudioSamples aSamples)
	{
		/**
		 * We're just going to dump all the samples into the line.
		 */
		byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
		mLine.write(rawBytes, 0, aSamples.getSize());
	}

	private static void closeJavaSound()
	{
		if (mLine != null)
		{
			/*
			 * Wait for the line to finish playing
			 */
			mLine.drain();
			/*
			 * Close the line.
			 */
			mLine.close();
			mLine = null;
		}
	}

}
