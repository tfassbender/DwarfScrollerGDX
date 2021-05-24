package net.jfabricationgames.gdx.character.ai;

import com.badlogic.gdx.maps.MapProperties;

import net.jfabricationgames.gdx.character.ai.util.timer.AttackTimerConfig;
import net.jfabricationgames.gdx.character.state.CharacterStateMachine;

public class ArtificialIntelligenceConfig {
	
	public ArtificialIntelligenceType type;
	public ArtificialIntelligenceConfig subAI;
	
	public String stateNameAction = "action";
	public String stateNameMove = "move";
	public String stateNameIdle = "idle";
	public String stateNameAttack = "attack";
	public String stateNameWait = "waiting";
	public String stateNameSurprise = "surprise";
	
	public float minDistToEnemy;
	public float maxDistToEnemy;
	public float maxMoveDistance;
	public float timeBetweenActions;
	
	public boolean useRelativePositions = true;
	
	public float attackDistance;
	public float attackSpeedFactor;
	public float attackSpeedDelay;
	public float minDistanceToTargetPlayer = 1f;
	public float distanceToKeepFromPlayer = 5f;
	public float distanceToStopRunning = 2f;
	public float distanceToInformTeamMates = 7f;
	public AttackTimerConfig attackTimerConfig;
	
	public ArtificialIntelligence buildAI(CharacterStateMachine stateMachine, MapProperties mapProperties) {
		return type.buildAI(this, stateMachine, mapProperties);
	}
}
