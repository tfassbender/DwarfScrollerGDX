package net.jfabricationgames.gdx.object.event;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

import net.jfabricationgames.gdx.character.player.PlayableCharacter;
import net.jfabricationgames.gdx.event.EventConfig;
import net.jfabricationgames.gdx.event.EventHandler;
import net.jfabricationgames.gdx.event.EventType;
import net.jfabricationgames.gdx.object.GameObject;
import net.jfabricationgames.gdx.object.GameObjectTypeConfig;
import net.jfabricationgames.gdx.physics.CollisionUtil;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator.PhysicsBodyProperties;
import net.jfabricationgames.gdx.physics.PhysicsCollisionType;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.screens.game.GameScreen;

public class EventObject extends GameObject implements ContactListener {
	
	public static final String MAP_PROPERTY_KEY_EVENT_PARAMETER = "eventParameter";
	public static final String MAP_PROPERTY_KEY_MULTIPLE_EXECUTIONS_POSSIBLE = "singleExecution";
	
	public static final String EVENT_KEY_RESPAWN_CHECKPOINT = "respawnCheckpoint";
	
	private String eventParameter;
	private boolean singleExecution;
	private boolean executed = false;
	private Vector2 eventObjectCenter;
	
	public EventObject(GameObjectTypeConfig typeConfig, Sprite sprite, MapProperties mapProperties) {
		super(typeConfig, sprite, mapProperties);
		PhysicsWorld.getInstance().registerContactListener(this);
	}
	
	@Override
	protected void createPhysicsBody(float x, float y) {
		float width = mapProperties.get("width", Float.class) * GameScreen.WORLD_TO_SCREEN;
		float height = mapProperties.get("height", Float.class) * GameScreen.WORLD_TO_SCREEN;
		
		x = mapProperties.get("x", Float.class) * GameScreen.WORLD_TO_SCREEN + width * 0.5f;
		y = mapProperties.get("y", Float.class) * GameScreen.WORLD_TO_SCREEN + height * 0.5f;
		
		eventObjectCenter = new Vector2(x, y);
		
		PhysicsBodyProperties properties = physicsBodyProperties.setX(x).setY(y).setWidth(width).setHeight(height)
				//change the collision type to OBSTACLE_SENSOR to not interact with projectiles
				.setCollisionType(PhysicsCollisionType.OBSTACLE_SENSOR);
		body = PhysicsBodyCreator.createRectangularBody(properties);
		body.setUserData(this);
		
		changeBodyToSensor();
	}
	
	@Override
	protected void processMapProperties() {
		super.processMapProperties();
		eventParameter = mapProperties.get(MAP_PROPERTY_KEY_EVENT_PARAMETER, String.class);
		singleExecution = Boolean.parseBoolean(mapProperties.get(MAP_PROPERTY_KEY_MULTIPLE_EXECUTIONS_POSSIBLE, "false", String.class));
	}
	
	@Override
	public void beginContact(Contact contact) {
		if (isPlayableCharacterContact(contact) && canBeExecuted()) {
			EventConfig event = new EventConfig().setEventType(EventType.EVENT_OBJECT_TOUCHED).setStringValue(eventParameter)
					.setParameterObject(this);
			EventHandler.getInstance().fireEvent(event);
			executed = true;
		}
	}
	
	private boolean isPlayableCharacterContact(Contact contact) {
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		
		//use the collision type OBSTACLE_SENSOR here, because the event object is changed to this type (see createPhysicsBody)
		if (CollisionUtil.containsCollisionType(PhysicsCollisionType.OBSTACLE_SENSOR, fixtureA, fixtureB)) {
			Object sensorUserData = CollisionUtil.getCollisionTypeUserData(PhysicsCollisionType.OBSTACLE_SENSOR, fixtureA, fixtureB);
			Object sensorCollidingUserData = CollisionUtil.getOtherTypeUserData(PhysicsCollisionType.OBSTACLE_SENSOR, fixtureA, fixtureB);
			
			if (sensorUserData == this && sensorCollidingUserData != null && sensorCollidingUserData instanceof PlayableCharacter) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canBeExecuted() {
		return !singleExecution || !executed;
	}
	
	@Override
	public void endContact(Contact contact) {}
	
	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {}
	
	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {}
	
	public Vector2 getEventObjectCenterPosition() {
		return eventObjectCenter;
	}
	
	@Override
	public void remove() {
		super.remove();
		PhysicsWorld.getInstance().removeContactListener(this);
	}
}
