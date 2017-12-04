package lord_of_galaxy.rss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import lord_of_galaxy.rss.model.*;

public class RSSFeedReader {
	
	@FunctionalInterface
	public interface RSSFeedEventListener{
		public void messageRecieved(FeedMessage msg);
	}
	
	private static final Timer timer = new Timer();
	protected final RSSFeedParser parser;
	protected Feed feed;
	
	protected RSSFeedEventListener l;
	
	protected HashSet<String> guids = new HashSet<String>();
	protected boolean fileBackup = false;
	Path filepath;
	
	private final boolean DEBUG;
	private final int time;
	
	public RSSFeedReader(String url, int time){
		parser = new RSSFeedParser(url);
		this.time = time;
		DEBUG = false;
	}
	
	public RSSFeedReader(String url, int time, String filename){
		this(url, time, filename, false);
	}
	
	public RSSFeedReader(String url, int time, String filename, boolean debug){
		parser = new RSSFeedParser(url);
		DEBUG = debug;
		this.time = time;
		fileBackup = true;
		filepath = Paths.get(filename);
		if(Files.exists(filepath)){
			try {
			 	List<String> lines = Files.readAllLines(filepath);
				for(String id: lines){
					guids.add(id);
				}
			} catch (IOException e) {
				// Error!
				e.printStackTrace();
				//I'll throw a runtime exception here, because I don't know what else to do.
				throw new RuntimeException("Could not load the RSS Backup File. Please give proper permissions before trying again.");
			}
		}else{
			try {
				Files.createFile(filepath);
			} catch (IOException e) {
				//Error!
				e.printStackTrace();
				//I'll throw a runtime exception here, because I don't know what else to do.
				throw new RuntimeException("Could not create the RSS Backup File. Please create it manually and try again.");
			}
		}
	}
	
	public void setListener(RSSFeedEventListener l){
		this.l = l;
		if(DEBUG)System.out.println("Listener set");
	}
	
	public void start(){
		timer.schedule(new TimerTask(){
			public void run(){
				update();
			}
		}, 5000, time*1000);
	}
	
	public void update(){
		if(DEBUG)System.out.println("Updated!");
		feed = parser.readFeed();
		HashSet<String> guidsN = new HashSet<String>();
		for(FeedMessage msg: feed.getMessages()){
			String g = msg.getGuid();
			guidsN.add(g);
			if(!guids.contains(g)){
				if(DEBUG)System.out.println("Received - " + msg.getTitle());
				l.messageRecieved(msg);
				try{
					Thread.sleep(2000);
				}catch(Exception e){
					//IDC
				}
			}
		}
		guids = guidsN;
		if(fileBackup){
			Iterator<String> it = guids.iterator();
			StringBuilder str = new StringBuilder();
			while(it.hasNext()){
				str.append(it.next() + "\n");
			}
			try{
				Files.write(filepath, str.toString().getBytes());
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}

