package net.jfabricationgames.gdx.character;

import net.jfabricationgames.gdx.DwarfScrollerGame;
import net.jfabricationgames.gdx.character.animation.MovingDirection;
import net.jfabricationgames.gdx.input.InputActionListener;
import net.jfabricationgames.gdx.input.InputContext;

public class CharacterInputMovementHandler implements InputActionListener {
	
	private static final float SQRT_0_5 = (float) Math.sqrt(0.5f);
	
	private static final String INPUT_MOVE_UP = "up";
	private static final String INPUT_MOVE_DOWN = "down";
	private static final String INPUT_MOVE_LEFT = "left";
	private static final String INPUT_MOVE_RIGHT = "right";
	private static final String INPUT_JUMP = "jump";
	private static final String INPUT_ATTACK = "attack";
	private static final String INPUT_ATTACK_JUMP = "attack_jump";
	private static final String INPUT_SPRINT = "sprint";
	
	private PlayableCharacter inputCharacter;
	
	private boolean moveUp = false;
	private boolean moveDown = false;
	private boolean moveLeft = false;
	private boolean moveRight = false;
	private boolean jump = false;
	private boolean attack = false;
	private boolean attackJump = false;
	private boolean sprint = false;
	private boolean changeSprint = false;
	
	private float idleTime;
	private float timeTillIdleAnimation;
	
	private MovingDirection jumpDirection;
	private MovingDirection lastMoveDirection;
	
	private InputContext inputContext;
	
	public CharacterInputMovementHandler(PlayableCharacter inputCharacter) {
		this.inputCharacter = inputCharacter;
		timeTillIdleAnimation = inputCharacter.getTimeTillIdleAnimation();
		jumpDirection = MovingDirection.NONE;
		lastMoveDirection = MovingDirection.NONE;
		inputContext = DwarfScrollerGame.getInstance().getInputContext();
		inputContext.addListener(this);
	}
	
	public void handleInputs(float delta) {
		readInputs();
		
		boolean move = moveUp || moveDown || moveLeft || moveRight;
		boolean characterActionSet = false;
		
		if (!characterActionSet && attackJump) {
			if (getAction().isInterruptable()) {
				if (move) {
					lastMoveDirection = getDirectionFromInputs();
					jumpDirection = getDirectionFromInputs();
					characterActionSet = inputCharacter.changeAction(CharacterAction.ATTACK_JUMP);
				}
				else {
					characterActionSet = inputCharacter.changeAction(CharacterAction.ATTACK);
				}
			}
		}
		if (!characterActionSet && attack) {
			if (getAction().isInterruptable()) {
				characterActionSet = inputCharacter.changeAction(CharacterAction.ATTACK);
			}
		}
		if (!characterActionSet && jump) {
			if (getAction().isInterruptable()) {
				jumpDirection = getDirectionFromInputs();
				characterActionSet = inputCharacter.changeAction(CharacterAction.JUMP);
			}
		}
		if (!characterActionSet && move) {
			lastMoveDirection = getDirectionFromInputs();
			if (getAction().isInterruptable() && getAction() != CharacterAction.RUN) {
				characterActionSet = inputCharacter.changeAction(CharacterAction.RUN);
			}
		}
		else {
			if (getAction() == CharacterAction.RUN) {
				characterActionSet = inputCharacter.changeAction(CharacterAction.NONE);
			}
		}
		
		if (inputCharacter.getCurrentAction() == CharacterAction.NONE) {
			sprint = false;
			idleTime += delta;
			
			if (idleTime > timeTillIdleAnimation) {
				if (inputCharacter.getCurrentAction() != CharacterAction.IDLE) {
					inputCharacter.changeAction(CharacterAction.IDLE);
				}
				else if (inputCharacter.isAnimationFinished()) {
					inputCharacter.changeAction(CharacterAction.NONE);
					idleTime = 0;
				}
			}
		}
		else {
			idleTime = 0;
		}
	}
	
	private void readInputs() {
		resetInputFlags();
		if (inputContext.isStateActive(INPUT_MOVE_UP)) {
			moveUp = true;
		}
		if (inputContext.isStateActive(INPUT_MOVE_DOWN)) {
			moveDown = true;
		}
		if (inputContext.isStateActive(INPUT_MOVE_LEFT)) {
			moveLeft = true;
		}
		if (inputContext.isStateActive(INPUT_MOVE_RIGHT)) {
			moveRight = true;
		}
		if (inputContext.isStateActive(INPUT_JUMP)) {
			jump = true;
		}
		if (inputContext.isStateActive(INPUT_ATTACK)) {
			attack = true;
		}
		if (inputContext.isStateActive(INPUT_ATTACK_JUMP)) {
			attackJump = true;
		}
		if (inputContext.isStateActive(INPUT_SPRINT)) {
			if (!changeSprint) {
				sprint = !sprint;				
			}
			changeSprint = true;
		}
		else {
			changeSprint = false;
		}
		
		if (moveUp && moveDown) {
			moveUp = false;
			moveDown = false;
		}
		if (moveLeft && moveRight) {
			moveLeft = false;
			moveRight = false;
		}
	}
	
	private void resetInputFlags() {
		moveUp = false;
		moveDown = false;
		moveLeft = false;
		moveRight = false;
		jump = false;
		attack = false;
		attackJump = false;
		//sprint is not reset here, but in the handleInputs method (when idle)
	}
	
	/**
	 * Get the current input direction (where horizontal directions have a higher priority than vertical directions)
	 * 
	 * @return The current direction from the inputs.
	 */
	private MovingDirection getDirectionFromInputs() {
		if (moveUp && moveRight) {
			return MovingDirection.UP_RIGHT;
		}
		if (moveUp && moveLeft) {
			return MovingDirection.UP_LEFT;
		}
		if (moveDown && moveRight) {
			return MovingDirection.DOWN_RIGHT;
		}
		if (moveDown && moveLeft) {
			return MovingDirection.DOWN_LEFT;
		}
		if (moveLeft) {
			return MovingDirection.LEFT;
		}
		if (moveRight) {
			return MovingDirection.RIGHT;
		}
		if (moveUp) {
			return MovingDirection.UP;
		}
		if (moveDown) {
			return MovingDirection.DOWN;
		}
		return MovingDirection.NONE;
	}
	
	public void move(float delta) {
		if (!getAction().isMoveBlocking()) {
			//reduce the endurance for sprinting before requesting the movement speed
			if (sprint) {
				inputCharacter.reduceEnduranceForSprinting(delta);
				if (inputCharacter.isExhausted()) {
					sprint = false;
				}
			}
			
			float moveSpeedPerDirection = inputCharacter.getMovingSpeed(sprint);
			if ((moveUp || moveDown) && (moveLeft || moveRight)) {
				moveSpeedPerDirection = inputCharacter.getMovingSpeed(sprint) * SQRT_0_5;
			}
			
			float speedX = 0;
			float speedY = 0;
			
			if (moveUp) {
				speedY = moveSpeedPerDirection;
			}
			if (moveDown) {
				speedY = -moveSpeedPerDirection;
			}
			if (moveLeft) {
				speedX = -moveSpeedPerDirection;
			}
			if (moveRight) {
				speedX = moveSpeedPerDirection;
			}
			move(speedX, speedY, delta);
		}
		else if (getAction() == CharacterAction.JUMP || getAction() == CharacterAction.ATTACK_JUMP) {
			float movingSpeed = inputCharacter.getMovingSpeed(sprint);
			float speedX = 0;
			float speedY = 0;
			
			if (jumpDirection.isCombinedDirection()) {
				movingSpeed *= SQRT_0_5;
			}
			
			if (jumpDirection.containsDirection(MovingDirection.UP)) {
				speedY = movingSpeed;
			}
			if (jumpDirection.containsDirection(MovingDirection.DOWN)) {
				speedY = -movingSpeed;
			}
			if (jumpDirection.containsDirection(MovingDirection.LEFT)) {
				speedX = -movingSpeed;
			}
			if (jumpDirection.containsDirection(MovingDirection.RIGHT)) {
				speedX = movingSpeed;
			}
			move(speedX, speedY, delta);
		}
	}
	
	private void move(float speedX, float speedY, float delta) {
		inputCharacter.move(speedX * delta, speedY * delta);
	}
	
	private CharacterAction getAction() {
		return inputCharacter.getCurrentAction();
	}
	
	public boolean isDrawDirectionRight() {
		return lastMoveDirection.isDrawingDirectionRight();
	}
	
	public MovingDirection getMovingDirection() {
		return lastMoveDirection;
	}
	
	@Override
	public boolean onAction(String action, Type type, Parameters parameters) {
		//only states are used in this implementation
		return false;
	}
}
