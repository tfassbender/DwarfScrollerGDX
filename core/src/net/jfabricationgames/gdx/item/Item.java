package net.jfabricationgames.gdx.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.ObjectMap;

import net.jfabricationgames.gdx.animation.AnimationDirector;
import net.jfabricationgames.gdx.character.player.PlayableCharacter;
import net.jfabricationgames.gdx.cutscene.action.CutsceneControlledUnit;
import net.jfabricationgames.gdx.data.handler.MapObjectDataHandler;
import net.jfabricationgames.gdx.data.properties.KeyItemProperties;
import net.jfabricationgames.gdx.data.state.MapObjectState;
import net.jfabricationgames.gdx.data.state.StatefulMapObject;
import net.jfabricationgames.gdx.map.GameMap;
import net.jfabricationgames.gdx.map.GameMapObject;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator.PhysicsBodyProperties;
import net.jfabricationgames.gdx.physics.PhysicsCollisionType;
import net.jfabricationgames.gdx.sound.SoundManager;
import net.jfabricationgames.gdx.sound.SoundSet;

public class Item implements GameMapObject, StatefulMapObject, CutsceneControlledUnit {
	
	protected static final SoundSet soundSet = SoundManager.getInstance().loadSoundSet("item");
	
	protected static ItemTypeConfig defaultTypeConfig;
	
	protected AnimationDirector<TextureRegion> animation;
	protected Sprite sprite;
	protected MapProperties properties;
	protected Body body;
	protected GameMap gameMap;
	
	protected final String itemName;
	protected ItemTypeConfig typeConfig;
	protected String pickUpSoundName;
	
	@MapObjectState
	protected boolean picked;
	
	public Item(String itemName, ItemTypeConfig typeConfig, Sprite sprite, AnimationDirector<TextureRegion> animation, MapProperties properties,
			GameMap gameMap) {
		this.itemName = itemName;
		this.typeConfig = typeConfig;
		this.sprite = sprite;
		this.animation = animation;
		this.properties = properties;
		this.gameMap = gameMap;
		
		if (animation == null && sprite == null) {
			Gdx.app.error(getClass().getSimpleName(), "Neither an animation nor a sprite was set for this item.");
		}
		
		readTypeConfig();
	}
	
	protected void readTypeConfig() {
		pickUpSoundName = typeConfig.pickUpSound;
		if (pickUpSoundName == null && defaultTypeConfig != null) {
			pickUpSoundName = defaultTypeConfig.pickUpSound;
		}
	}
	
	protected void createPhysicsBody(float x, float y) {
		PhysicsBodyProperties properties = new PhysicsBodyProperties().setType(BodyType.StaticBody).setX(x).setY(y).setSensor(false)
				.setRadius(typeConfig.physicsObjectRadius).setCollisionType(PhysicsCollisionType.ITEM);
		body = PhysicsBodyCreator.createCircularBody(properties);
		body.setUserData(this);
	}
	
	public void draw(float delta, SpriteBatch batch) {
		if (animation != null) {
			animation.increaseStateTime(delta);
			animation.draw(batch);
		}
		else if (sprite != null) {
			sprite.draw(batch);
		}
	}
	
	public boolean canBePicked(PlayableCharacter player) {
		return true;
	}
	
	public void pickUp() {
		picked = true;
		playPickUpSound();
		remove();
		
		MapObjectDataHandler.getInstance().addStatefulMapObject(this);
	}
	
	@Override
	public void removeFromMap() {
		remove();
	}
	
	public void remove() {
		if (gameMap != null) {
			gameMap.removeItem(this, body);
			body = null;// set the body to null to avoid strange errors in native Box2D methods
		}
	}
	
	private void playPickUpSound() {
		if (pickUpSoundName != null) {
			soundSet.playSound(pickUpSoundName);
		}
	}
	
	public ObjectMap<String, String> getKeyProperties() {
		return KeyItemProperties.getKeyProperties(properties);
	}
	
	public boolean containsProperty(String property) {
		return properties.containsKey(property);
	}
	
	public <T> T getProperty(String property, Class<T> clazz) {
		return properties.get(property, clazz);
	}
	
	public String getItemName() {
		return itemName;
	}
	
	@Override
	public String getUnitId() {
		return properties.get(CutsceneControlledUnit.MAP_PROPERTIES_KEY_UNIT_ID, String.class);
	}
	
	@Override
	public String toString() {
		return "Item [name=" + itemName + "; properties=" + properties + "]";
	}
	
	@Override
	public String getMapObjectId() {
		return StatefulMapObject.getMapObjectId(properties);
	}
	
	@Override
	public void applyState(ObjectMap<String, String> state) {
		if (Boolean.parseBoolean(state.get("picked"))) {
			picked = true;
			remove();
		}
	}
}
