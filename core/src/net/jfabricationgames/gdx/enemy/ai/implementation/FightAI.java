package net.jfabricationgames.gdx.enemy.ai.implementation;

import com.badlogic.gdx.math.Vector2;

import net.jfabricationgames.gdx.enemy.ai.ArtificialIntelligence;
import net.jfabricationgames.gdx.enemy.ai.move.AIAttackingMove;
import net.jfabricationgames.gdx.enemy.ai.move.MoveType;
import net.jfabricationgames.gdx.enemy.ai.util.AttackTimer;
import net.jfabricationgames.gdx.enemy.state.EnemyState;

public class FightAI extends AbstractAttackAI implements ArtificialIntelligence {
	
	private float attackDistance;
	
	public FightAI(ArtificialIntelligence subAI, EnemyState attackState, AttackTimer attackTimer, float attackDistance) {
		super(subAI, attackState, attackTimer);
		this.attackDistance = attackDistance;
	}
	
	@Override
	public void calculateMove(float delta) {
		subAI.calculateMove(delta);
		
		super.calculateMove(delta);
		if (targetInRange(attackDistance)) {
			AIAttackingMove move = new AIAttackingMove(this);
			move.targetPosition = targetingPlayer.getPosition();
			setMove(MoveType.ATTACK, move);
		}
	}
	
	@Override
	public void executeMove() {
		AIAttackingMove move = getMove(MoveType.ATTACK, AIAttackingMove.class);
		if (isExecutedByMe(move)) {
			if (changeToAttackState()) {
				move.executed();
			}
			if (inAttackState()) {
				attackState.flipAnimationToDirection(directionToTarget());
				if (distanceToTarget() > minDistanceToTargetPlayer) {
					attackMoveTo(move.targetPosition);
				}
			}
		}
		
		subAI.executeMove();
	}
	
	protected void attackMoveTo(Vector2 targetPosition) {
		enemy.moveTo(targetPosition);
	}
}
