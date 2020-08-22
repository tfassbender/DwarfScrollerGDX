package net.jfabricationgames.gdx.item;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.World;

import net.jfabricationgames.gdx.assets.AssetGroupManager;
import net.jfabricationgames.gdx.factory.AbstractFactory;
import net.jfabricationgames.gdx.map.GameMap;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.screens.GameScreen;

public class ItemFactory extends AbstractFactory {
	
	private static final String configFile = "config/factory/item_factory.json";
	private static Config config;
	
	private TextureAtlas atlas;
	private World world;
	
	private GameMap gameMap;
	
	public ItemFactory(GameMap gameMap) {
		this.gameMap = gameMap;
		
		if (config == null) {
			config = loadConfig(Config.class, configFile);
		}
		
		AssetGroupManager assetManager = AssetGroupManager.getInstance();
		atlas = assetManager.get(config.itemAtlas);
		world = PhysicsWorld.getInstance().getWorld();
	}
	
	public Item createItem(String name, float x, float y, MapProperties properties) {
		Sprite sprite = new Sprite(atlas.findRegion(name));
		sprite.setX(x * GameScreen.WORLD_TO_SCREEN - sprite.getWidth() * 0.5f);
		sprite.setY(y * GameScreen.WORLD_TO_SCREEN - sprite.getHeight() * 0.5f);
		sprite.setScale(GameScreen.WORLD_TO_SCREEN);
		
		Item item = new Item(name, sprite, properties, gameMap);
		item.createPhysicsBody(world, x * GameScreen.WORLD_TO_SCREEN, y * GameScreen.WORLD_TO_SCREEN);
		
		return item;
	}
	
	public static class Config {
		
		public String itemAtlas;
	}
}
