package net.jfabricationgames.gdx.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;

import net.jfabricationgames.gdx.screens.game.GameScreen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AnimationDirector<T extends TextureRegion> {
	
	public static boolean isTextureRight(boolean initialAnimationDirectionIsRight, AnimationDirector<TextureRegion> animation) {
		return initialAnimationDirectionIsRight != animation.getKeyFrame().isFlipX();
	}
	
	public static boolean isTextureLeft(boolean initialAnimationDirectionIsRight, AnimationDirector<TextureRegion> animation) {
		return initialAnimationDirectionIsRight == animation.getKeyFrame().isFlipX();
	}
	
	private float stateTime;
	private Animation<T> animation;
	
	private AnimationSpriteConfig spriteConfig;
	
	public AnimationDirector(Animation<T> animation) {
		this.animation = animation;
		initializeSpriteConfigWithoutPosition();
	}
	
	protected void initializeSpriteConfigWithoutPosition() {
		T keyFrame = animation.getKeyFrame(0);
		spriteConfig = new AnimationSpriteConfig().setWidth(keyFrame.getRegionWidth()).setHeight(keyFrame.getRegionHeight());
	}
	
	/**
	 * Draw the current key frame of this animation onto the {@link SpriteBatch}.<br>
	 * ATTENTION: This method will throw an {@link IllegalStateException} if this AnimationDirector does not contain an AnimationSpriteConfig object.
	 */
	public void draw(SpriteBatch batch) {
		if (spriteConfig == null) {
			throw new IllegalStateException("No AnimationSpriteConfig. Please add an AnimationSpriteConfig in order to use the draw method");
		}
		T keyFrame = getKeyFrame();
		float x = spriteConfig.x + ((spriteConfig.width - keyFrame.getRegionWidth()) * GameScreen.WORLD_TO_SCREEN * 0.5f);
		float y = spriteConfig.y + ((spriteConfig.height - keyFrame.getRegionHeight()) * GameScreen.WORLD_TO_SCREEN * 0.5f);
		batch.draw(keyFrame, x, y, spriteConfig.width * 0.5f, spriteConfig.height * 0.5f, keyFrame.getRegionWidth(), keyFrame.getRegionHeight(),
				GameScreen.WORLD_TO_SCREEN, GameScreen.WORLD_TO_SCREEN, 0f);
	}
	
	public void drawInMenu(SpriteBatch batch) {
		if (spriteConfig == null) {
			throw new IllegalStateException("No AnimationSpriteConfig. Please add an AnimationSpriteConfig in order to use the draw method");
		}
		
		T keyFrame = getKeyFrame();
		batch.draw(keyFrame, spriteConfig.x, spriteConfig.y, spriteConfig.width, spriteConfig.height);
	}
	
	/**
	 * Get the frame at the current time.
	 */
	public T getKeyFrame() {
		return animation.getKeyFrame(stateTime);
	}
	
	public float getStateTime() {
		return stateTime;
	}
	
	public void increaseStateTime(float delta) {
		stateTime += delta;
	}
	
	public void setStateTime(float stateTime) {
		this.stateTime = stateTime;
	}
	
	/**
	 * Reset the state time to 0 to restart the animation.
	 */
	public void resetStateTime() {
		stateTime = 0;
	}
	
	/**
	 * Set the animation state time to the end of the animation.
	 */
	public void endAnimation() {
		stateTime = animation.getAnimationDuration();
	}
	
	public void setPlayMode(PlayMode playMode) {
		animation.setPlayMode(playMode);
	}
	
	/**
	 * Get the {@link Animation} that this object holds.
	 */
	public Animation<T> getAnimation() {
		return animation;
	}
	
	public boolean isAnimationFinished() {
		return animation.isAnimationFinished(stateTime);
	}
	
	public float getAnimationDuration() {
		return animation.getAnimationDuration();
	}
	
	/**
	 * Flip all key frames of the animation.
	 */
	public void flip(boolean x, boolean y) {
		for (TextureRegion region : animation.getKeyFrames()) {
			region.flip(x, y);
		}
	}
	
	public AnimationSpriteConfig getSpriteConfig() {
		return spriteConfig;
	}
	
	public void setSpriteConfig(AnimationSpriteConfig spriteConfig) {
		this.spriteConfig = spriteConfig;
	}
	
	public AnimationSpriteConfig getSpriteConfigCopy() {
		return new AnimationSpriteConfig(spriteConfig);
	}
}
