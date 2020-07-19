package net.jfabricationgames.gdx.character.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

public class CharacterAnimationAssetManager {
	
	public static final String CONFIG_DIR = "config/animation/";
	
	private static CharacterAnimationAssetManager instance;
	
	public static synchronized CharacterAnimationAssetManager getInstance() {
		if (instance == null) {
			instance = new CharacterAnimationAssetManager();
		}
		return instance;
	}
	
	private AssetManager assetManager;
	private Map<String, Animation<TextureRegion>> animations;
	private Map<String, AnimationConfig> animationConfigurations;
	
	private CharacterAnimationAssetManager() {
		assetManager = new AssetManager();
		animations = new HashMap<>();
		animationConfigurations = new HashMap<>();
	}
	
	/**
	 * Load the animations, defined in the configuration file in "assets/config/animation/__the_config_parameter__.json
	 * 
	 * Example: To load dwarf animations from file "assets/config/animation/dwarf.json" enter the parameter "dwarf"
	 * 
	 * @param config
	 *        The configuration file, that's animations are to be loaded (without the file ending)
	 */
	public void loadAnimations(String... configurations) {
		for (String config : configurations) {
			AnimationConfigList animationConfig = loadAnimationConfig(config);
			
			animationConfigurations
					.putAll(animationConfig.getConfigList().stream().collect(Collectors.toMap(AnimationConfig::getName, Function.identity())));
			
			animationConfig.getConfigList().forEach(this::markAnimationForLoading);
			assetManager.finishLoading();
			
			animationConfig.getConfigList().forEach(this::createAnimation);
		}
	}
	
	private AnimationConfigList loadAnimationConfig(String config) {
		FileHandle configFile = Gdx.files.internal(CONFIG_DIR + config + ".json");
		Json json = new Json();
		return json.fromJson(AnimationConfigList.class, configFile);
	}
	
	private void markAnimationForLoading(AnimationConfig animation) {
		if (!animations.containsKey(animation.getName())) {
			assetManager.load(animation.getAtlas(), TextureAtlas.class);
		}
	}
	
	private void createAnimation(AnimationConfig config) {
		TextureAtlas textureAtlas = assetManager.get(config.getAtlas(), TextureAtlas.class);
		Animation<TextureRegion> animation = new Animation<>(config.getFrameDuration(), textureAtlas.findRegions(config.getName()),
				config.getPlayMode());
		animations.put(config.getName(), animation);
	}
	
	/**
	 * Get an {@link Animation} from the loaded animations.
	 * 
	 * @param name
	 *        The name of the animation that was defined in the JSON configuration file from which the animations were loaded.
	 * 		
	 * @return The {@link Animation}.
	 */
	public Animation<TextureRegion> getAnimation(String name) {
		if (!animations.containsKey(name)) {
			throw new IllegalArgumentException("The animation \"" + name + "\" doesn't exist in this asset manager");
		}
		return animations.get(name);
	}
	
	/**
	 * Get an {@link AnimationDirector} from the loaded animations.
	 * 
	 * @param name
	 *        The name of the animation that was defined in the JSON configuration file from which the animations were loaded.
	 * 		
	 * @return The {@link AnimationDirector}.
	 */
	public AnimationDirector<TextureRegion> getAnimationDirector(String name) {
		return new AnimationDirector<TextureRegion>(getAnimation(name));
	}
	
	/**
	 * Get a {@link List} of all names of animations that are currently loaded. The names can then be used to get the animations using the
	 * getAnimation(String) method.
	 * 
	 * @return A {@link List} of the names of all animations.
	 */
	public List<String> getAvailableAnimations() {
		return new ArrayList<String>(animations.keySet());
	}
	
	/**
	 * Get a {@link TextureAtlas} from the asset manager.
	 * 
	 * @param name
	 *        The atlas' name
	 * 		
	 * @return The {@link TextureAtlas}
	 */
	public TextureAtlas getAtlas(String name) {
		return assetManager.get(name, TextureAtlas.class);
	}
	
	/**
	 * Get some {@link AtlasRegion}s as an {@link Array} from a {@link TextureAtlas}.
	 * 
	 * @param atlas
	 *        The name of the {@link TextureAtlas}
	 * 		
	 * @param region
	 *        The name of the {@link AtlasRegion}s within the {@link TextureAtlas}
	 * 		
	 * @return An {@link Array} of {@link AtlasRegion} objects.
	 */
	public Array<AtlasRegion> getAtlasRegions(String atlas, String regions) {
		return getAtlas(atlas).findRegions(regions);
	}
	
	/**
	 * Get an {@link AnimationConfig} by it's name.
	 * 
	 * @param configName
	 *        The name of the configuration that is defined in the configuration's JSON file.
	 * 		
	 * @return The {@link AnimationConfig}
	 */
	public AnimationConfig getAnimationConfig(String configName) {
		return animationConfigurations.get(configName);
	}
	
	/**
	 * Dispose the asset manager.
	 */
	public void dispose() {
		assetManager.dispose();
	}
}
