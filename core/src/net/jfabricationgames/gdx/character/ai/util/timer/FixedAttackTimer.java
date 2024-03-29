package net.jfabricationgames.gdx.character.ai.util.timer;

public class FixedAttackTimer implements AttackTimer {
	
	private final float fixedTime;
	
	public FixedAttackTimer(float fixedTime) {
		this.fixedTime = fixedTime;
	}
	
	@Override
	public float getTimeTillNextAttack() {
		return fixedTime;
	}
}
