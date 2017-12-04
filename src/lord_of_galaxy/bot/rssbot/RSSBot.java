package lord_of_galaxy.bot.rssbot;

import static lord_of_galaxy.bot.BotUtils.getBuiltDiscordClient;
import static lord_of_galaxy.bot.BotUtils.sendMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import lord_of_galaxy.rss.RSSFeedReader;
import lord_of_galaxy.rss.model.FeedMessage;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;

//TODO - When do you plan t add the comments??
public class RSSBot{
	private final IDiscordClient c;
	private HashMap<RSSFeedReader, HashSet<IChannel>> feedToChannel;
	protected final Settings settings;
	public final boolean DEBUG;
	protected final FeedMessageFormatter formatter;
	
	public RSSBot(String settingsPath) throws Exception{
		this(settingsPath, false);
	}
	
	public RSSBot(String settingsPath, boolean DEBUG) throws Exception{
		this(settingsPath, (msg)->{
				String text = ":newspaper: | **" + msg.getTitle() + "**\n" + msg.getLink();
				return text;
			}, DEBUG);
	}
	
	public RSSBot(String settingsPath, FeedMessageFormatter f) throws Exception{
		this(settingsPath, f, false);
	}
	
	public RSSBot(String settingsPath, FeedMessageFormatter f, boolean DEBUG) throws Exception{
		settings = readSettings(settingsPath);
		formatter = f;
		this.DEBUG = DEBUG;
		if(DEBUG){
			//DEBUG START
			println("Settings:");
			println("\tToken: " + settings.token);
			println("\tRSS Backup Folder: " + settings.rssBackupFolderPath);
			println("\tUpdate Period: " + settings.period);
			println("\tFeeds:");
			for(FeedSettings fs:settings.feeds){
				println("\t\tLink :" + fs.link);
				for(long ch:fs.channels){
					println("\t\t\tChannel: " + ch);
				}
			}
			//DEBUG END
		}
		c = getBuiltDiscordClient(settings.token);
		c.getDispatcher().registerListener(this);
		c.login();
	}
	
	@EventSubscriber
	public void onReady(ReadyEvent evt){
		feedToChannel = new HashMap<RSSFeedReader, HashSet<IChannel>>();
		FeedSettings[] feeds = settings.feeds;
		Path rssPath = Paths.get(settings.rssBackupFolderPath);
		if(!Files.exists(rssPath)){
			try{
				Files.createDirectories(rssPath);
			}catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException("Could not create the RSS Backup Folder. Please create it manually, and then try again.");
			}
		}else if(!Files.isDirectory(rssPath)){
			throw new RuntimeException("The path provided for the RSS Backup Folder is not a folder!");
		}
		for(FeedSettings fs:feeds){
			HashSet<IChannel> channels = new HashSet<IChannel>();
			RSSFeedReader r = new RSSFeedReader(fs.link, settings.period, settings.rssBackupFolderPath + File.separator + "rssfeed" + fs.id + ".rfb", DEBUG);
			r.setListener((msg)->{
				gotEntry(r, msg);
			});
			for(long id:fs.channels){
				channels.add(c.getChannelByID(id));
			}
			feedToChannel.put(r, channels);
			r.start();
		}
	}
	
	public void shutdown(){
		c.logout();
		try{
			Thread.sleep(3000);
		}catch(Exception e){
			//don't care
		}
		System.exit(0);
	}
	
	private void gotEntry(RSSFeedReader r, FeedMessage msg){
		HashSet<IChannel> channels = feedToChannel.get(r);
		String text = formatter.format(msg);
		for(IChannel channel:channels){
			sendMessage(channel, text);
			try{
				Thread.sleep(1500);
			}catch(Exception e){
				//don't care
			}
		}
	}
	
	private static Settings readSettings(String settingsPath) throws Exception{
		 // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader
        InputStream in = new FileInputStream(settingsPath);
        XMLEventReader r = inputFactory.createXMLEventReader(in);
        
        boolean correct = false;
        String token = null;
        String rssBackupFolderPath = null;
        int period = 900;
        int curFeedId = 0;
        ArrayList<FeedSettings> feeds = new ArrayList<FeedSettings>();
        boolean inFeed = false;
        ArrayList<Long> curChannels = new ArrayList<Long>();
        String curLink = null;
        // read the XML document
        while(r.hasNext()){
        	XMLEvent e = r.nextEvent();
        	if(e.isStartElement()){
        		StartElement se = e.asStartElement();
        		String name = se.getName().getLocalPart();
        		if(!correct){
        			if(!name.equals("rssbot"))throw new RuntimeException("The settings file is incorrect!");
        			correct = true;
        		}
        		switch(name){
        		case "token":
        			token = getCharacterData(e, r);
        			break;
        		case "rssbackupfolder":
        			rssBackupFolderPath = getCharacterData(e, r);
        			break;
        		case "updateperiod":
        			period = Integer.parseInt(getCharacterData(e, r));
        			break;
        		case "feed":
        			inFeed = true;
        			Attribute id = se.getAttributeByName(new QName("id"));
        			if(id != null)curFeedId = Integer.parseInt(id.getValue().trim());
        			else curFeedId++;
        			break;
        		case "link":
        			if(!inFeed)throw new RuntimeException("The settings file is incorrect!");
        			curLink = getCharacterData(e, r);
        			break;
        		case "channel":
        			if(!inFeed)throw new RuntimeException("The settings file is incorrect!");
        			curChannels.add(Long.parseLong(getCharacterData(e, r)));
        			break;
        		}
        	}else if(e.isEndElement()){
        		EndElement ee = e.asEndElement();
        		String name = ee.getName().getLocalPart();
        		if(name.equals("feed")){
        			if(curLink == null || curChannels.isEmpty())throw new RuntimeException("The settings file is incorrect!");
        			long[] cids = new long[curChannels.size()];
        			for(int i = 0; i < curChannels.size(); cids[i] = curChannels.get(i++));
        			feeds.add(new FeedSettings(curFeedId, curLink, cids));
        			inFeed = false;
        			curChannels.clear();
        		}
        	}
        }
        if(feeds.isEmpty())throw new RuntimeException("The settings file is incorrect!");
        if(token == null || rssBackupFolderPath == null)throw new RuntimeException("The settings file is incorrect!");
        rssBackupFolderPath = (rssBackupFolderPath.endsWith("\\")||rssBackupFolderPath.endsWith("/"))?(rssBackupFolderPath.substring(0, rssBackupFolderPath.length()-1)):rssBackupFolderPath;
        return new Settings(feeds.toArray(new FeedSettings[feeds.size()]), token, period, rssBackupFolderPath);
	}
	
	private static String getCharacterData(XMLEvent event, XMLEventReader eventReader)
            throws XMLStreamException {
        String result = "";
        
        while(!event.isEndElement()){
        	event = eventReader.nextEvent();
        	if (event instanceof Characters) {
        		result += ((Characters) event).getData();
        	}
        }
        return result;
    }
	
//	private static void println(){
//		System.out.println();
//	}
	
	private static <T> void println(T str){
		System.out.println(str);
	}
	
//	private static <T> void print(T str){
//		System.out.print(str);
//	}
	
	//I tried a lot to avoid this
	private static final class Settings{
		final FeedSettings[] feeds;
		final String token;
		final int period;
		final String rssBackupFolderPath;
		Settings(final FeedSettings[] feeds_, final String token_, final int period_, final String rssBackupFolderPath_){
			feeds = feeds_;
			token = token_;
			period = period_;
			rssBackupFolderPath = rssBackupFolderPath_;
		}
	}
	
	private static final class FeedSettings{
		final int id;
		final String link;
		final long[] channels;
		FeedSettings(int id_, String link_, long[] channels_){
			id = id_;
			link = link_;
			channels = channels_;
		}
	}
	
	@FunctionalInterface
	public interface FeedMessageFormatter{
		public String format(FeedMessage msg);
	}
}

