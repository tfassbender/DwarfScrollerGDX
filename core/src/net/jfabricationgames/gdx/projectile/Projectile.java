package net.jfabricationgames.gdx.projectile;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;

import net.jfabricationgames.gdx.animation.AnimationDirector;
import net.jfabricationgames.gdx.attributes.Hittable;
import net.jfabricationgames.gdx.map.GameMap;
import net.jfabricationgames.gdx.physics.CollisionUtil;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator;
import net.jfabricationgames.gdx.physics.PhysicsBodyCreator.PhysicsBodyProperties;
import net.jfabricationgames.gdx.physics.PhysicsCollisionType;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.sound.SoundManager;
import net.jfabricationgames.gdx.sound.SoundSet;

public abstract class Projectile implements ContactListener {
	
	private static final String SOUND_SET_PROJECTILE = "projectile";
	private static final String EXPLOSION_PROJECTILE_TYPE = "explosion";
	
	protected GameMap gameMap;
	protected Body body;
	protected ProjectileTypeConfig typeConfig;
	protected PhysicsCollisionType collisionType;
	
	protected float distanceTraveled;
	protected float timeActive;
	protected boolean attackPerformed;
	
	protected float damage;
	protected float pushForce;
	protected boolean pushForceAffectedByBlock = true;
	
	protected float explosionDamage;
	protected float explosionPushForce;
	protected boolean explosionPushForceAffectedByBlock;
	
	protected AnimationDirector<TextureRegion> animation;
	protected Sprite sprite;
	
	public Projectile(ProjectileTypeConfig typeConfig, Sprite sprite) {
		this.typeConfig = typeConfig;
		this.sprite = sprite;
		
		initialize();
	}
	
	public Projectile(ProjectileTypeConfig typeConfig, AnimationDirector<TextureRegion> animation) {
		this.typeConfig = typeConfig;
		this.animation = animation;
		
		sprite = new Sprite(animation.getKeyFrame());
		
		initialize();
	}
	
	private void initialize() {
		distanceTraveled = 0;
		if (typeConfig.damping > 0 && isRangeDestricted()) {
			throw new IllegalStateException("A Projectile can not be range restricted AND use linear damping");
		}
		attackPerformed = false;
		
		if (typeConfig.sound != null) {
			SoundSet soundSet = SoundManager.getInstance().loadSoundSet(SOUND_SET_PROJECTILE);
			soundSet.playSound(typeConfig.sound);
		}
		
		registerAsContactListener();
	}
	
	private void registerAsContactListener() {
		PhysicsWorld physicsWorld = PhysicsWorld.getInstance();
		physicsWorld.registerContactListener(this);
	}
	
	protected void createPhysicsBody(World world, Vector2 position, PhysicsCollisionType collisionType) {
		this.collisionType = collisionType;
		PhysicsBodyProperties bodyProperties = createShapePhysicsBodyProperties().setType(BodyType.DynamicBody).setX(position.x).setY(position.y)
				.setCollisionType(collisionType).setLinearDamping(typeConfig.damping);
		body = PhysicsBodyCreator.createBody(world, bodyProperties);
		body.setUserData(this);
	}
	
	protected abstract PhysicsBodyProperties createShapePhysicsBodyProperties();
	
	protected void startProjectile(Vector2 direction) {
		Vector2 movement = direction.nor().scl(typeConfig.speed);
		
		prepareProjectile(direction);
		
		//apply the movement force only once because the linear damping is set to zero
		body.applyForceToCenter(movement.scl(body.getMass() * 10), true);
	}
	
	protected void prepareProjectile(Vector2 direction) {}
	
	protected void setGameMap(GameMap gameMap) {
		this.gameMap = gameMap;
	}
	
	public void setDamage(float damage) {
		this.damage = damage;
	}
	
	public void setPushForce(float pushForce) {
		this.pushForce = pushForce;
	}
	
	public void setPushForceAffectedByBlock(boolean pushForceAffectedByBlock) {
		this.pushForceAffectedByBlock = pushForceAffectedByBlock;
	}
	
	public void update(float delta) {
		distanceTraveled += delta * typeConfig.speed;
		timeActive += delta;
		
		if (animation != null) {
			animation.increaseStateTime(delta);
			sprite = new Sprite(animation.getKeyFrame());
			
			if (animation.isAnimationFinished()) {
				remove();
			}
		}
		
		if (reachedMaxRange()) {
			remove();
		}
		if (explosionTimeReached()) {
			explode();
		}
	}
	
	private boolean reachedMaxRange() {
		return distanceTraveled > typeConfig.range && isRangeDestricted();
	}
	
	private boolean isRangeDestricted() {
		return typeConfig.range > 0;
	}
	
	private boolean explosionTimeReached() {
		return timeActive > typeConfig.timeTillExplosion && isExplosive();
	}
	
	private boolean isExplosive() {
		return typeConfig.timeTillExplosion > 0;
	}
	
	private void explode() {
		Projectile explosion = ProjectileFactory.getInstance().createProjectile(EXPLOSION_PROJECTILE_TYPE, body.getPosition(), Vector2.Zero,
				collisionType);
		explosion.setDamage(explosionDamage);
		explosion.setPushForce(explosionPushForce);
		explosion.setPushForceAffectedByBlock(explosionPushForceAffectedByBlock);
		remove();
	}
	
	public void draw(SpriteBatch batch) {
		sprite.setScale(sprite.getScaleX() * typeConfig.textureScale, sprite.getScaleY() * typeConfig.textureScale);
		sprite.setPosition(body.getPosition().x - sprite.getOriginX(), body.getPosition().y - sprite.getOriginY());
		sprite.draw(batch);
	}
	
	@Override
	public void beginContact(Contact contact) {
		if (isAttackOver() || damage == 0) {
			return;
		}
		
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		
		Object attackUserData = CollisionUtil.getCollisionTypeUserData(collisionType, fixtureA, fixtureB);
		if (attackUserData == this) {
			Object attackedUserData = CollisionUtil.getOtherTypeUserData(collisionType, fixtureA, fixtureB);
			
			if (attackedUserData instanceof Hittable) {
				Hittable hittable = ((Hittable) attackedUserData);
				hittable.takeDamage(damage);
				//enemies define the force themselves; the force parameter is a factor for this self defined force
				hittable.pushByHit(body.getPosition().cpy(), pushForce, pushForceAffectedByBlock);
			}
			body.setLinearDamping(typeConfig.dampingAfterObjectHit);
			attackPerformed = true;
		}
	}
	
	private boolean isAttackOver() {
		return attackPerformed && !typeConfig.multipleHitsPossible;
	}
	
	@Override
	public void endContact(Contact contact) {}
	
	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {}
	
	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {}
	
	public void remove() {
		attackPerformed = true;
		removePhysicsBody();
		gameMap.removeProjectile(this);
	}
	
	private void removePhysicsBody() {
		PhysicsWorld.getInstance().destroyBodyAfterWorldStep(body);
		PhysicsWorld.getInstance().removeContactListener(this);
		body = null;
	}
	
	public void setExplosionDamage(float explosionDamage) {
		this.explosionDamage = explosionDamage;
	}
	
	public void setExplosionPushForce(float explosionPushForce) {
		this.explosionPushForce = explosionPushForce;
	}
	
	public void setExplosionPushForceAffectedByBlock(boolean explosionPushForceAffectedByBlock) {
		this.explosionPushForceAffectedByBlock = explosionPushForceAffectedByBlock;
	}
}
