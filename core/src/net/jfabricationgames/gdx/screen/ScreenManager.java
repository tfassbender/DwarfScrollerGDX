package net.jfabricationgames.gdx.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

import net.jfabricationgames.gdx.screen.game.GameScreen;
import net.jfabricationgames.gdx.screen.menu.MainMenuScreen;

public class ScreenManager {
	
	public static final String ASSET_GROUP_NAME = "game";
	public static final String INPUT_CONTEXT_NAME = "game";
	
	private static ScreenManager instance;
	
	public static synchronized ScreenManager getInstance() {
		if (instance == null) {
			instance = new ScreenManager();
		}
		return instance;
	}
	
	private Game game;
	private Screen gameScreen;
	
	private ScreenManager() {}
	
	public void createGameScreen(Runnable afterCreatingGameScreen) {
		GameScreen.loadAndShowGameScreen(afterCreatingGameScreen);
	}
	
	public void changeToGameScreen() {
		if (gameScreen == null) {
			throw new IllegalStateException("backToGameScreen was called, but the gameScreen was not yet set.");
		}
		
		setScreen(gameScreen);
	}
	
	public void changeToMainMenuScreen() {
		setScreen(new MainMenuScreen());
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
	
	public void setScreen(Screen screen) {
		Gdx.app.debug(getClass().getSimpleName(), "Changing screen to: " + screen);
		game.setScreen(screen);
	}
	
	public void setGameScreen(Screen gameScreen) {
		this.gameScreen = gameScreen;
	}
}
