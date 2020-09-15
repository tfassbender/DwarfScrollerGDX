package net.jfabricationgames.gdx.object;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.MapProperties;

import net.jfabricationgames.gdx.animation.AnimationManager;
import net.jfabricationgames.gdx.assets.AssetGroupManager;
import net.jfabricationgames.gdx.factory.AbstractFactory;
import net.jfabricationgames.gdx.map.GameMap;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.screens.game.GameScreen;

public class ObjectFactory extends AbstractFactory {
	
	private static final String configFile = "config/factory/object_factory.json";
	private static Config config;
	
	private Map<String, ObjectTypeConfig> typeConfigs;
	
	public ObjectFactory(GameMap gameMap) {
		this.gameMap = gameMap;
		
		if (config == null) {
			config = loadConfig(Config.class, configFile);
		}
		
		loadTypeConfigs();
		
		AssetGroupManager assetManager = AssetGroupManager.getInstance();
		AnimationManager.getInstance().loadAnimations(config.objectAnimations);
		atlas = assetManager.get(config.objectAtlas);
		world = PhysicsWorld.getInstance().getWorld();
	}
	
	@SuppressWarnings("unchecked")
	private void loadTypeConfigs() {
		typeConfigs = json.fromJson(HashMap.class, ObjectTypeConfig.class, Gdx.files.internal(config.objectTypesConfig));
	}
	
	public GameObject createObject(String type, float x, float y, MapProperties properties) {
		ObjectTypeConfig typeConfig = typeConfigs.get(type);
		if (typeConfig == null) {
			throw new IllegalStateException("No type config known for type: " + type
					+ ". Either the type name is wrong or you have to add it to the objectTypesConfig (see \"" + configFile + "\")");
		}
		
		Sprite sprite = createSprite(x, y, typeConfig.texture);
		
		GameObject object;
		switch (type) {
			case "barrel":
			case "box":
			case "pot":
				object = new DestroyableObject(typeConfig, sprite, properties);
				break;
			case "chest":
			case "signboard":
			case "directionboard":
				object = new InteractiveObject(typeConfig, sprite, properties);
				break;
			default:
				throw new IllegalStateException("Unknown object type: " + type);
		}
		object.setGameMap(gameMap);
		object.createPhysicsBody(world, x * GameScreen.WORLD_TO_SCREEN, y * GameScreen.WORLD_TO_SCREEN);
		object.setTextureAtlas(atlas);
		
		return object;
	}
	
	public static class Config {
		
		public String objectAtlas;
		public String objectAnimations;
		public String objectTypesConfig;
	}
}
