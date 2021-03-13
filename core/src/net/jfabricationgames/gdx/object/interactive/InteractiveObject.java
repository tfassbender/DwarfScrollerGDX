package net.jfabricationgames.gdx.object.interactive;

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.ObjectMap;

import net.jfabricationgames.gdx.animation.AnimationDirector;
import net.jfabricationgames.gdx.animation.AnimationSpriteConfig;
import net.jfabricationgames.gdx.attack.AttackType;
import net.jfabricationgames.gdx.character.player.PlayableCharacter;
import net.jfabricationgames.gdx.data.handler.MapObjectDataHandler;
import net.jfabricationgames.gdx.data.state.MapObjectState;
import net.jfabricationgames.gdx.interaction.InteractionManager;
import net.jfabricationgames.gdx.interaction.Interactive;
import net.jfabricationgames.gdx.object.GameObject;
import net.jfabricationgames.gdx.object.GameObjectTypeConfig;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.screens.game.GameScreen;

public class InteractiveObject extends GameObject implements Interactive {
	
	public static final String MAP_PROPERTY_KEY_ACTIVATE_ON_STARTUP = "activateOnStartup";
	
	@MapObjectState
	private boolean actionExecuted = false;
	private boolean changedBodyToSensor = false;
	private AnimationDirector<TextureRegion> interactionAnimation;
	
	public InteractiveObject(GameObjectTypeConfig typeConfig, Sprite sprite, MapProperties properties) {
		super(typeConfig, sprite, properties);
		PhysicsWorld.getInstance().registerContactListener(this);
		
		interactionAnimation = InteractionManager.getInstance().getInteractionAnimationCopy();
		interactionAnimation.setSpriteConfig(createSpriteConfig());
		//don't show the interaction animation on startup
		playInteractionAnimationDisappear();
		interactionAnimation.endAnimation();
	}
	
	private AnimationSpriteConfig createSpriteConfig() {
		AnimationSpriteConfig spriteConfig = AnimationSpriteConfig.fromSprite(sprite);
		spriteConfig.x += (sprite.getWidth() * GameScreen.WORLD_TO_SCREEN * InteractionManager.INTERACTION_MARK_DEFAULT_OFFSET_FACTOR_X
				+ typeConfig.interactionMarkerOffsetX);
		spriteConfig.y += (sprite.getHeight() * GameScreen.WORLD_TO_SCREEN * InteractionManager.INTERACTION_MARK_DEFAULT_OFFSET_FACTOR_Y
				+ typeConfig.interactionMarkerOffsetY);
		return spriteConfig;
	}
	
	@Override
	public void postAddToGameMap() {
		super.postAddToGameMap();
		
		if (isActivateOnStartup()) {
			executeInteraction();
		}
	}
	
	@Override
	public void applyState(ObjectMap<String, String> state) {
		super.applyState(state);
		
		if (Boolean.parseBoolean(state.get("actionExecuted"))) {
			actionExecuted = true;
			
			if (typeConfig.changeBodyToSensorAfterAction) {
				changedBodyToSensor = true;
				changeBodyToSensor();
			}
			
			if (typeConfig.textureAfterAction != null) {
				sprite = createSprite(typeConfig.textureAfterAction);
			}
		}
	}
	
	public boolean isActivateOnStartup() {
		return Boolean.parseBoolean(mapProperties.get(MAP_PROPERTY_KEY_ACTIVATE_ON_STARTUP, "false", String.class));
	}
	
	public boolean isActionExecuted() {
		return actionExecuted;
	}
	
	@Override
	public void draw(float delta, SpriteBatch batch) {
		super.draw(delta, batch);
		
		if (showInteractionIcon()) {
			interactionAnimation.increaseStateTime(delta);
			interactionAnimation.draw(batch);
		}
		
		if (changeBodyToSensorAfterAction()) {
			changedBodyToSensor = true;
			changeBodyToSensor();
		}
	}
	
	private boolean showInteractionIcon() {
		return canBeExecutedByConfig() //
				&& (!interactionAnimation.isAnimationFinished() // the animation (to appear or disappear) is still playing 
						|| interactionAnimation.getAnimation().getPlayMode() == PlayMode.NORMAL); // the interaction icon appeared and is to be shown (animation finished)
	}
	
	private boolean changeBodyToSensorAfterAction() {
		return actionExecuted && animation != null && animation.isAnimationFinished() && typeConfig.changeBodyToSensorAfterAction
				&& !changedBodyToSensor;
	}
	
	@Override
	public void takeDamage(float damage, AttackType attackType) {
		if (animation == null || animation.isAnimationFinished()) {
			animation = getHitAnimation();
		}
		playHitSound();
	}
	
	@Override
	protected boolean showHitAnimation() {
		return typeConfig.animationHit != null
				&& (!actionExecuted || typeConfig.hitAnimationAfterAction || typeConfig.multipleActionExecutionsPossible);
	}
	
	@Override
	public void interact() {
		if (canBeExecuted()) {
			if (typeConfig.animationAction != null) {
				animation = getActionAnimation();
			}
			executeInteraction();
		}
	}
	
	protected boolean canBeExecuted() {
		return canBeExecutedByConfig();
	}
	private boolean canBeExecutedByConfig() {
		return typeConfig.multipleActionExecutionsPossible || !actionExecuted;
	}
	
	private void executeInteraction() {
		actionExecuted = true;
		performAction();
		dropItems();
		if (typeConfig.textureAfterAction != null) {
			sprite = createSprite(typeConfig.textureAfterAction);
		}
		
		MapObjectDataHandler.getInstance().addStatefulMapObject(this);
	}
	
	@Override
	public float getDistanceToPlayer(PlayableCharacter character) {
		return character.getPosition().sub(body.getPosition()).len();
	}
	
	private void performAction() {
		if (typeConfig.interactiveAction != null) {
			typeConfig.interactiveAction.execute(this);
		}
	}
	
	/**
	 * Makes the MapProperties available in the InteractiveAction enum
	 */
	protected MapProperties getMapProperties() {
		return mapProperties;
	}
	
	@Override
	public void beginContact(Contact contact) {
		if (isPlayableCharacterContact(contact)) {
			if (typeConfig.interactByContact) {
				interact();
			}
			else {
				InteractionManager.getInstance().movedInRange(this);
				playInteractionAnimationAppear();
			}
		}
	}
	
	@Override
	public void endContact(Contact contact) {
		if (isPlayableCharacterContact(contact)) {
			InteractionManager.getInstance().movedOutOfRange(this);
			playInteractionAnimationDisappear();
		}
	}
	
	private void playInteractionAnimationAppear() {
		interactionAnimation.setPlayMode(PlayMode.NORMAL);
		interactionAnimation.resetStateTime();
	}
	private void playInteractionAnimationDisappear() {
		interactionAnimation.setPlayMode(PlayMode.REVERSED);
		interactionAnimation.resetStateTime();
	}
	
	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {}
	
	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {}
	
	@Override
	public void remove() {
		super.remove();
		PhysicsWorld.getInstance().removeContactListener(this);
	}
}
