package lord_of_galaxy.bot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;

public class BotUtils {
	public static final boolean DEBUG = false;
	
	public static IDiscordClient getBuiltDiscordClient(String token){
		return new ClientBuilder()
				.withToken(token)
				.withPingTimeout(5)
				.build();
	}
	
	public static void sendEmbed(IChannel channel, EmbedObject embed){
		RequestBuffer.request(() -> {
			channel.sendMessage(embed);
		}
		);
	}
	
	public static void sendMessage(IChannel channel, String msg){
		RequestBuffer.request(() -> {
			if(DEBUG)System.out.println("Attempting to send message.");
			while(true){
				try{
					channel.sendMessage(msg);
					return;
				}catch (Exception e){
					System.err.println("Could not send message, retrying.");
					e.printStackTrace();
				}
			}
		});
	}
	
	public static IMessage sendMessageAndGet(IChannel channel, String msg){
		RequestBuffer.RequestFuture<IMessage> rf =  RequestBuffer.request(() -> {
			while(true){
				try{
					return channel.sendMessage(msg);
				}catch (Exception e){
					System.err.println("Could not send message, retrying.");
					e.printStackTrace();
					continue;
				}
			}
		});
		while(!rf.isDone()){
			try{
				Thread.sleep(10);
			}catch (Exception e){
				//don't care
			}
		}
		return rf.get();
	}
}
