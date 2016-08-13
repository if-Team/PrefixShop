package me.onebone.prefixshop;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.onebone.chatformatter.ChatFormatter;
import me.onebone.economyapi.EconomyAPI;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

public class PrefixShop extends PluginBase implements Listener{
	private Config shops;
	private EconomyAPI api;
	private ChatFormatter formatter;
	private Map<String, Long> taps;
	
	public void onEnable(){
		if(!this.getDataFolder().exists()){
			this.getDataFolder().mkdir();
		}
		
		api = EconomyAPI.getInstance();
		formatter = (ChatFormatter) this.getServer().getPluginManager().getPlugin("ChatFormatter");
		
		shops = new Config(new File(this.getDataFolder(), "shops.yml"), Config.YAML);
		
		taps = new HashMap<String, Long>();
		
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("prefix")){
			if(shops.exists(sender.getName().toLowerCase())){
				@SuppressWarnings("unchecked")
				List<String> prefixes = shops.getList(sender.getName().toLowerCase());
				if(args.length == 0){
					sender.sendMessage(TextFormat.GREEN + "구매한 칭호들:\n" + TextFormat.AQUA + String.join(TextFormat.WHITE + ", " + TextFormat.AQUA, prefixes));
				}else{
					String value = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
					if(prefixes.contains(value)){
						formatter.setPrefix(sender.getName().toLowerCase(), value);
						sender.sendMessage(TextFormat.GREEN + "칭호가 " + value + "로 설정되었습니다.");
					}else{
						sender.sendMessage(TextFormat.RED + "칭호가 존재하지 않습니다.");
					}
				}
			}else{
				sender.sendMessage(TextFormat.RED + "구매한 칭호가 존재하지 않습니다. 칭호 상점에서 구매하세요!");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event){
		String[] lines = event.getLines();
		
		if(lines[0].equals("칭호")){
			Player player = event.getPlayer();
			if(player.hasPermission("prefixshop.create")){
				if(lines[1].equals("")){
					player.sendMessage(TextFormat.RED + "칭호가 올바르지 않습니다.");
					return;
				}
				
				double price;
				try{
					price = Double.parseDouble(lines[2]);
				}catch(NumberFormatException e){
					player.sendMessage(TextFormat.RED + "가격이 올바르지 않습니다.");
					return;
				}
				
				Block block = event.getBlock();
				
				String key = (int) block.x + "-" + (int) block.y + "-" + (int) block.z + "-" + block.level.getName();
				LinkedHashMap<String, Object> d = new LinkedHashMap<String, Object>(shops.getAll());
				d.put(key, new Object[]{
						lines[1],
						price
				});
				
				shops.setAll(d);
				shops.save();
				
				event.setLine(0, TextFormat.DARK_GREEN + "[칭호 상점]");
				event.setLine(2, TextFormat.GOLD + api.getMonetaryUnit() + lines[2]);
				player.sendMessage(TextFormat.GREEN + "칭호 상점이 생성되었습니다.");
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "serial" })
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		Block block = event.getBlock();
		
		String key = (int) block.x + "-" + (int) block.y + "-" + (int) block.z + "-" + block.level.getName();
		if(shops.exists(key)){
			event.setCancelled();
			Player player = event.getPlayer();
			
			Object[] shop = (Object[])shops.get(key);
			
			final String prefix = shop[0].toString();
			
			double price = (Double) shop[1];
			if(!(taps.containsKey(player.getName().toLowerCase()) && System.currentTimeMillis() - taps.get(player.getName().toLowerCase()) < 1000)){
				player.sendMessage("칭호 " + prefix + " 를 " + api.getMonetaryUnit() + price + " 로 구매하시려면 한 번 더 터치해주세요.");
				taps.put(player.getName().toLowerCase(), System.currentTimeMillis());
				return;
			}
			
			taps.remove(player.getName().toLowerCase());
			
			if(shops.exists(player.getName().toLowerCase())){
				if(shops.getList(player.getName().toLowerCase()).contains(prefix)){
					player.sendMessage(TextFormat.RED + "칭호가 이미 존재합니다.");
					return;
				}
			}
			if(api.reduceMoney(player, price) == EconomyAPI.RET_SUCCESS){
				player.sendMessage("칭호를 구매했습니다.");
				
				if(shops.exists(player.getName().toLowerCase())){
					shops.getList(player.getName().toLowerCase()).add(prefix);
				}else{
					shops.set(player.getName().toLowerCase(), new ArrayList<String>(){
						{
							add(prefix);
						}
					});
				}
				shops.save();
			}else{
				player.sendMessage(TextFormat.RED + "칭호를 살 돈이 부족합니다.");
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event){
		Block block = event.getBlock();
		
		String key = (int) block.x + "-" + (int) block.y + "-" + (int) block.z + "-" + block.level.getName();
		if(shops.exists(key)){
			Player player = event.getPlayer();
			if(player.hasPermission("prefixshop.remove")){
				shops.remove(key);
				shops.save();
				player.sendMessage(TextFormat.GREEN + "칭호 상점이 제거되었습니다.");
			}else{
				player.sendMessage(TextFormat.RED + "칭호 상점을 제거할 권한이 없습니다.");
				event.setCancelled();
			}

		}
	}
}
