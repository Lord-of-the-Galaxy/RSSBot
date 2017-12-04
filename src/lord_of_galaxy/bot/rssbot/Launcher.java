package lord_of_galaxy.bot.rssbot;

import java.io.File;
import java.util.Scanner;

import lord_of_galaxy.rss.StringUtils;

public class Launcher {

	private static Thread consoleListener;
	protected static RSSBot INSTANCE;

	public static void main(String[] args) {
		consoleListener = new Thread(new Runnable(){
			public void run(){
				Scanner sc = new Scanner(System.in);

				while(true){
					if(sc.hasNextLine()){
						String s = sc.nextLine().replaceAll("\n", "");
						if(s.equals("shutdown")){
							sc.close();
							INSTANCE.shutdown();
						}
					}else{
						try{
							Thread.sleep(100);
						}catch(InterruptedException e){
							//don't care
						}
					}
				}
			}
		});
		consoleListener.start();
		String path = System.getProperty("user.dir") + File.separator;
		try{
			
			INSTANCE = new RSSBot(path + "settings.xml", (msg)->{
				String desc = StringUtils.unescapeHtml3(msg.getDescription().replaceAll("<.+?>", "").replaceAll("[ \t]+", " ").trim());
				if(desc.length()>150){
					int i = 150;
					char c = desc.charAt(i);
					while(c != ' ' && c != '\t'){
						i++;
						if(i>=desc.length())break;
						c = desc.charAt(i);
					}
					desc = desc.substring(0, i) + "....";
				}
				String text = ":newspaper: *"+ msg.getCategory().trim() +"* | **" + msg.getTitle().trim() + "** \n" + msg.getLink() + "\n" + desc;
				return text;
			},true);
			
			//INSTANCE = new RSSBot(path + "settings.xml", true);
		}catch (Exception e){
			throw new RuntimeException(e);
		}
		System.out.println("The bot is running now.");
	}

}
