package net.jfabricationgames.gdx.map;

import java.lang.annotation.Annotation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

import net.jfabricationgames.gdx.camera.CameraMovementHandler;
import net.jfabricationgames.gdx.character.animal.Animal;
import net.jfabricationgames.gdx.character.animal.AnimalFactory;
import net.jfabricationgames.gdx.character.enemy.Enemy;
import net.jfabricationgames.gdx.character.enemy.EnemyFactory;
import net.jfabricationgames.gdx.character.npc.NonPlayableCharacter;
import net.jfabricationgames.gdx.character.npc.NonPlayableCharacterFactory;
import net.jfabricationgames.gdx.character.player.PlayableCharacter;
import net.jfabricationgames.gdx.character.player.PlayerFactory;
import net.jfabricationgames.gdx.cutscene.CutsceneHandler;
import net.jfabricationgames.gdx.data.handler.CharacterPropertiesDataHandler;
import net.jfabricationgames.gdx.data.handler.MapDataHandler;
import net.jfabricationgames.gdx.data.handler.MapObjectDataHandler;
import net.jfabricationgames.gdx.data.properties.MapObjectStateProperties;
import net.jfabricationgames.gdx.data.state.BeforePersistState;
import net.jfabricationgames.gdx.data.state.StatefulMapObject;
import net.jfabricationgames.gdx.event.EventConfig;
import net.jfabricationgames.gdx.event.EventHandler;
import net.jfabricationgames.gdx.event.EventListener;
import net.jfabricationgames.gdx.event.EventType;
import net.jfabricationgames.gdx.item.Item;
import net.jfabricationgames.gdx.item.ItemFactory;
import net.jfabricationgames.gdx.object.GameObject;
import net.jfabricationgames.gdx.object.GameObjectFactory;
import net.jfabricationgames.gdx.object.movable.MovableObject;
import net.jfabricationgames.gdx.physics.AfterWorldStep;
import net.jfabricationgames.gdx.physics.BeforeWorldStep;
import net.jfabricationgames.gdx.physics.PhysicsWorld;
import net.jfabricationgames.gdx.projectile.Projectile;
import net.jfabricationgames.gdx.projectile.ProjectileFactory;
import net.jfabricationgames.gdx.screens.game.GameScreen;
import net.jfabricationgames.gdx.util.AnnotationUtil;

public class GameMap implements EventListener, Disposable {
	
	public enum GlobalMapPropertyKeys {
		
		MINI_MAP_CONFIG_PATH("mini_map_config_path"), //
		MAP_WIDTH_IN_TILE_DIMENSIONS("width"), //
		MAP_HEIGHT_IN_TILE_DIMENSIONS("height"), //
		MAP_TILE_WIDTH_IN_PIXELS("tilewidth"), // 
		MAP_TILE_HEIGHT_IN_PIXELS("tileheight");
		
		private final String key;
		
		private GlobalMapPropertyKeys(String key) {
			this.key = key;
		}
		
		public String getKey() {
			return key;
		}
	}
	
	public static final String MAP_KEY_BACKGROUND_LAYERS = "background_layers";
	public static final String MAP_KEY_ABOVE_PLAYER_LAYERS = "above_player_layers";
	public static final String MAP_KEY_SHADOW_LAYERS = "shadow_layers";
	public static final int[] BACKGROUND_LAYERS_DEFAULT = new int[] {0, 1};
	public static final int[] ABOVE_PLAYER_LAYERS_DEFAULT = new int[] {2};
	public static final int[] SHADOW_LAYERS_DEFAULT = new int[] {};
	
	public static final GameMapGroundType DEFAULT_GROUND_PROPERTIES = new GameMapGroundType();
	
	private static GameMap instance;
	
	public static GameMapGroundType getGroundTypeByName(String name) {
		return TiledMapPhysicsLoader.groundTypes.get(name);
	}
	
	public static synchronized GameMap getInstance() {
		if (instance == null) {
			throw new IllegalStateException("The GameMap was not yet created. Use the method createGameMap(OrthographicCamera) to create it.");
		}
		
		return instance;
	}
	
	public static synchronized void createGameMap(OrthographicCamera camera) {
		if (instance != null) {
			throw new IllegalStateException("The GameMap has already been created. Use the method getInstance() to get a reference to it.");
		}
		
		instance = new GameMap(camera);
	}
	
	protected TiledMap map;
	protected Vector2 playerStartingPosition;
	
	private int[] backgroundLayers;
	private int[] abovePlayerLayers;
	private int[] shadowLayers;
	
	private String currentMapIdentifier;
	
	//the lists are initialized in the factories
	protected Array<Item> items;
	protected Array<Item> itemsAboveGameObjects;
	protected Array<GameObject> objects;
	protected Array<Enemy> enemies;
	protected Array<NonPlayableCharacter> nonPlayableCharacters;
	protected Array<Animal> animals;
	protected Array<Projectile> projectiles;
	
	protected ItemFactory itemFactory;
	protected GameObjectFactory objectFactory;
	protected EnemyFactory enemyFactory;
	protected NonPlayableCharacterFactory npcFactory;
	protected AnimalFactory animalFactory;
	
	private OrthographicCamera camera;
	private OrthogonalTiledMapRenderer renderer;
	
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	
	private PlayableCharacter player;
	
	private CutsceneHandler cutsceneHandler;
	
	private GameMap(OrthographicCamera camera) {
		this.camera = camera;
		
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		itemsAboveGameObjects = new Array<>();
		projectiles = new Array<>();
		ProjectileFactory.createInstance();
		
		itemFactory = new ItemFactory();
		objectFactory = new GameObjectFactory();
		enemyFactory = new EnemyFactory();
		npcFactory = new NonPlayableCharacterFactory();
		animalFactory = new AnimalFactory();
		
		cutsceneHandler = CutsceneHandler.getInstance();
		
		// create the player before other map objects, because it contains event listeners that listen for events that are fired when these objects are created
		player = PlayerFactory.createPlayer();
		
		EventHandler.getInstance().registerEventListener(this);
	}
	
	@BeforePersistState
	public void updateMapData() {
		MapDataHandler.getInstance().setMapIdentifier(currentMapIdentifier);
	}
	
	public void updateAfterLoadingGameState() {
		updateMap();
		updatePlayerPosition();
		updateCameraPosition();
	}
	
	private void updateMap() {
		String mapIdentifier = MapDataHandler.getInstance().getMapIdentifier();
		showMap(mapIdentifier);
	}
	
	private void updatePlayerPosition() {
		CharacterPropertiesDataHandler characterDataHandler = CharacterPropertiesDataHandler.getInstance();
		Vector2 playerPosition = characterDataHandler.getPlayerPosition();
		player.setPosition(playerPosition.x, playerPosition.y);
	}
	
	private void updateCameraPosition() {
		CameraMovementHandler.getInstance().centerCameraOnPlayer();
	}
	
	public String getCurrentMapIdentifier() {
		return currentMapIdentifier;
	}
	
	public void showMap(String mapIdentifier) {
		currentMapIdentifier = mapIdentifier;
		String mapAsset = GameMapManager.getInstance().getMapFilePath(mapIdentifier);
		
		removeCurrentMapIfPresent();
		
		player.reAddToWorld();
		
		TiledMapLoader loader = new TiledMapLoader(mapAsset);
		loader.loadMap();
		
		renderer = new OrthogonalTiledMapRenderer(map, GameScreen.WORLD_TO_SCREEN, batch);
		renderer.setView(camera);
		
		loadLayersFromMapProperties();
		
		TiledMapPhysicsLoader mapPhysicsLoader = new TiledMapPhysicsLoader(GameScreen.SCREEN_TO_WORLD);
		mapPhysicsLoader.createPhysics(map);
		
		player.setPosition(playerStartingPosition.x, playerStartingPosition.y);
		
		updateMapObjectStates();
		
		EventHandler.getInstance().fireEvent(new EventConfig().setEventType(EventType.MAP_ENTERED).setStringValue(mapIdentifier));
	}
	
	private void removeCurrentMapIfPresent() {
		if (isMapInitialized()) {
			removeGameObjects();
			removeBodiesFromWorld();
			clearObjectLists();
		}
	}
	
	/**
	 * Remove the game objects first, to avoid using their bodies again (what would lead to segmentation faults in the native box2d methods)
	 */
	private void removeGameObjects() {
		for (Item item : items) {
			item.removeFromMap();
		}
		for (Item item : itemsAboveGameObjects) {
			item.removeFromMap();
		}
		for (GameObject object : objects) {
			object.removeFromMap();
		}
		for (Enemy enemy : enemies) {
			enemy.removeFromMap();
		}
		for (NonPlayableCharacter npc : nonPlayableCharacters) {
			npc.removeFromMap();
		}
		for (Animal animal : animals) {
			animal.removeFromMap();
		}
		for (Projectile projectile : projectiles) {
			projectile.removeFromMap();
		}
		player.removeFromMap();
	}
	
	private void removeBodiesFromWorld() {
		Gdx.app.debug(getClass().getSimpleName(), "removeCurrentMap - world locked: " + PhysicsWorld.getInstance().isInWorldStepExecution());
		PhysicsWorld.getInstance().removeBodiesFromWorld();
	}
	
	private void clearObjectLists() {
		items.clear();
		itemsAboveGameObjects.clear();
		objects.clear();
		enemies.clear();
		nonPlayableCharacters.clear();
		animals.clear();
		projectiles.clear();
	}
	
	private void loadLayersFromMapProperties() {
		String backgroundLayersJson = map.getProperties().get(MAP_KEY_BACKGROUND_LAYERS, String.class);
		String abovePlayerLayersJson = map.getProperties().get(MAP_KEY_ABOVE_PLAYER_LAYERS, String.class);
		String shadowLayersJson = map.getProperties().get(MAP_KEY_SHADOW_LAYERS, String.class);
		
		Json json = new Json();
		if (backgroundLayersJson != null) {
			backgroundLayers = json.fromJson(int[].class, backgroundLayersJson);
		}
		else {
			backgroundLayers = BACKGROUND_LAYERS_DEFAULT;
		}
		if (abovePlayerLayersJson != null) {
			abovePlayerLayers = json.fromJson(int[].class, abovePlayerLayersJson);
		}
		else {
			abovePlayerLayers = ABOVE_PLAYER_LAYERS_DEFAULT;
		}
		if (shadowLayersJson != null) {
			shadowLayers = json.fromJson(int[].class, shadowLayersJson);
		}
		else {
			shadowLayers = SHADOW_LAYERS_DEFAULT;
		}
	}
	
	private boolean isMapInitialized() {
		return items != null && itemsAboveGameObjects != null && objects != null && enemies != null && nonPlayableCharacters != null
				&& animals != null && projectiles != null;
	}
	
	public void executeBeforeWorldStep() {
		executeAnnotatedMethodsOnAllObjects(BeforeWorldStep.class);
	}
	
	public void executeAfterWorldStep() {
		executeAnnotatedMethodsOnAllObjects(AfterWorldStep.class);
	}
	
	private void executeAnnotatedMethodsOnAllObjects(Class<? extends Annotation> annotation) {
		AnnotationUtil.executeAnnotatedMethods(annotation, player);
		
		executeAnnotatedMethods(annotation, items);
		executeAnnotatedMethods(annotation, itemsAboveGameObjects);
		executeAnnotatedMethods(annotation, objects);
		executeAnnotatedMethods(annotation, enemies);
		executeAnnotatedMethods(annotation, nonPlayableCharacters);
		executeAnnotatedMethods(annotation, animals);
		executeAnnotatedMethods(annotation, projectiles);
	}
	
	private void executeAnnotatedMethods(Class<? extends Annotation> annotation, Array<?> mapObjects) {
		for (Object mapObject : mapObjects) {
			AnnotationUtil.executeAnnotatedMethods(annotation, mapObject);
		}
	}
	
	public void updateCameraToRenderer() {
		renderer.setView(camera);
	}
	
	public void renderBackground() {
		renderer.render(backgroundLayers);
	}
	
	public void processPlayer(float delta) {
		player.process(delta);
	}
	
	public void processAndRenderGameObject(float delta) {
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		renderItems(delta);
		renderObjects(delta);
		renderItemsAboveGameObjects(delta);
		
		processCutscene(delta);
		processEnemies(delta);
		renderEnemies(delta);
		processNpcs(delta);
		renderNpcs(delta);
		processAnimals(delta);
		renderAnimals(delta);
		processProjectiles(delta);
		renderProjectiles(delta);
		
		renderPlayer(delta);
		batch.end();
		
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Filled);
		renderEnemyHealthBars();
		shapeRenderer.end();
	}
	
	private void renderItems(float delta) {
		for (Item item : items) {
			item.draw(delta, batch);
		}
	}
	
	private void renderObjects(float delta) {
		for (GameObject object : objects) {
			object.draw(delta, batch);
		}
	}
	
	private void renderItemsAboveGameObjects(float delta) {
		for (Item item : itemsAboveGameObjects) {
			item.draw(delta, batch);
		}
	}
	
	private void processCutscene(float delta) {
		cutsceneHandler.act(delta);
	}
	
	private void processEnemies(float delta) {
		for (Enemy enemy : enemies) {
			enemy.act(delta);
		}
	}
	private void renderEnemies(float delta) {
		for (Enemy enemy : enemies) {
			enemy.draw(delta, batch);
		}
	}
	
	private void processNpcs(float delta) {
		for (NonPlayableCharacter npc : nonPlayableCharacters) {
			npc.act(delta);
		}
	}
	private void renderNpcs(float delta) {
		for (NonPlayableCharacter npc : nonPlayableCharacters) {
			npc.draw(delta, batch);
		}
	}
	
	private void processAnimals(float delta) {
		for (Animal animal : animals) {
			animal.act(delta);
		}
	}
	private void renderAnimals(float delta) {
		for (Animal animal : animals) {
			animal.draw(delta, batch);
		}
	}
	
	private void processProjectiles(float delta) {
		for (Projectile projectile : projectiles) {
			projectile.update(delta);
		}
	}
	private void renderProjectiles(float delta) {
		for (Projectile projectile : projectiles) {
			projectile.draw(delta, batch);
		}
	}
	
	private void renderPlayer(float delta) {
		player.render(delta, batch);
	}
	
	private void renderEnemyHealthBars() {
		for (Enemy enemy : enemies) {
			enemy.drawHealthBar(shapeRenderer);
		}
	}
	
	public void renderAbovePlayer() {
		renderer.render(abovePlayerLayers);
	}
	
	public void renderShalows() {
		if (shadowLayers.length > 0) {
			renderer.render(shadowLayers);
		}
	}
	
	public Vector2 getPlayerStartingPosition() {
		return playerStartingPosition;
	}
	
	public PlayableCharacter getPlayer() {
		return player;
	}
	
	public void addItem(Item item) {
		items.add(item);
	}
	
	public void addItemAboveGameObjects(Item item) {
		itemsAboveGameObjects.add(item);
	}
	
	public void removeItem(Item item, Body body) {
		items.removeValue(item, false);
		itemsAboveGameObjects.removeValue(item, false);
		removePhysicsBody(body);
	}
	
	public void addObject(GameObject object) {
		objects.add(object);
		object.postAddToGameMap();
		
		sortMovableGameObjectsLast();
	}
	
	/**
	 * Sort movable game objects to the end of the list, to make them drawn on top of other objects.
	 */
	private void sortMovableGameObjectsLast() {
		int listSize = objects.size;
		for (int i = 0; i < listSize; i++) {
			GameObject object = objects.get(i);
			if (object instanceof MovableObject) {
				objects.removeIndex(i);
				listSize--;
				i--;
				objects.add(object);
			}
		}
	}
	
	public void removeObject(GameObject gameObject, Body body) {
		objects.removeValue(gameObject, false);
		removePhysicsBody(body);
	}
	
	public void addEnemy(Enemy enemy) {
		enemies.add(enemy);
	}
	
	public void removeEnemy(Enemy enemy, Body body) {
		enemies.removeValue(enemy, false);
		removePhysicsBody(body);
	}
	
	public void addNpc(NonPlayableCharacter npc) {
		nonPlayableCharacters.add(npc);
	}
	
	public void removeNpc(NonPlayableCharacter npc, Body body) {
		nonPlayableCharacters.removeValue(npc, false);
		removePhysicsBody(body);
	}
	
	public void addAnimal(Animal animal) {
		animals.add(animal);
	}
	
	public void removeAnimal(Animal animal, Body body) {
		animals.removeValue(animal, false);
		removePhysicsBody(body);
	}
	
	public void addProjectile(Projectile projectile) {
		projectiles.add(projectile);
	}
	
	public void removeProjectile(Projectile projectile, Body body) {
		projectiles.removeValue(projectile, false);
		removePhysicsBody(body);
	}
	
	private void removePhysicsBody(Body body) {
		PhysicsWorld.getInstance().removeBodyWhenPossible(body);
	}
	
	public ItemFactory getItemFactory() {
		return itemFactory;
	}
	
	public GameObjectFactory getObjectFactory() {
		return objectFactory;
	}
	
	public EnemyFactory getEnemyFactory() {
		return enemyFactory;
	}
	
	public NonPlayableCharacterFactory getNpcFactory() {
		return npcFactory;
	}
	
	public MapProperties getGlobalMapProperties() {
		return map.getProperties();
	}
	
	public float getMapWidth() {
		return map.getProperties().get(GlobalMapPropertyKeys.MAP_WIDTH_IN_TILE_DIMENSIONS.getKey(), Integer.class) //
				* map.getProperties().get(GlobalMapPropertyKeys.MAP_TILE_WIDTH_IN_PIXELS.getKey(), Integer.class);
	}
	public float getMapHeight() {
		return map.getProperties().get(GlobalMapPropertyKeys.MAP_HEIGHT_IN_TILE_DIMENSIONS.getKey(), Integer.class) //
				* map.getProperties().get(GlobalMapPropertyKeys.MAP_TILE_HEIGHT_IN_PIXELS.getKey(), Integer.class);
	}
	
	public Object getUnitById(String unitId) {
		for (Enemy enemy : enemies) {
			if (unitId.equals(enemy.getUnitId())) {
				return enemy;
			}
		}
		
		for (NonPlayableCharacter npc : nonPlayableCharacters) {
			if (unitId.equals(npc.getUnitId())) {
				return npc;
			}
		}
		
		for (Animal animal : animals) {
			if (unitId.equals(animal.getUnitId())) {
				return animal;
			}
		}
		
		for (GameObject object : objects) {
			if (unitId.equals(object.getUnitId())) {
				return object;
			}
		}
		
		for (Item item : items) {
			if (unitId.equals(item.getUnitId())) {
				return item;
			}
		}
		
		return null;
	}
	
	@Override
	public void handleEvent(EventConfig event) {
		if (event.eventType == EventType.UPDATE_MAP_OBJECT_STATES) {
			updateMapObjectStates();
		}
	}
	
	private void updateMapObjectStates() {
		MapObjectDataHandler dataHandler = MapObjectDataHandler.getInstance();
		
		applyStatesToMapObjects(dataHandler, objects);
		applyStatesToMapObjects(dataHandler, items);
		applyStatesToMapObjects(dataHandler, enemies);
		
		addNotConfiguredObjects(dataHandler);
	}
	
	private void applyStatesToMapObjects(MapObjectDataHandler dataHandler, Array<? extends StatefulMapObject> mapObjects) {
		//create a copy of the list before executing to avoid a concurrent modification (which would not throw an exception)
		Array<StatefulMapObject> immutableObjectList = new Array<>(mapObjects);
		for (StatefulMapObject object : immutableObjectList) {
			applyStateIfPresent(dataHandler, object);
		}
	}
	
	private void applyStateIfPresent(MapObjectDataHandler dataHandler, StatefulMapObject object) {
		ObjectMap<String, String> state = dataHandler.getStateById(object.getMapObjectId());
		if (state != null) {
			object.applyState(state);
		}
	}
	
	private void addNotConfiguredObjects(MapObjectDataHandler dataHandler) {
		ObjectMap<String, MapObjectStateProperties> currentMapStates = dataHandler.getCurrentMapStates();
		
		if (currentMapStates != null) {
			for (String mapStateKey : currentMapStates.keys()) {
				if (isUnconfiguredObjectKey(mapStateKey)) {
					addUnconfiguredObjectToMap(currentMapStates.get(mapStateKey));
				}
			}
		}
	}
	
	private boolean isUnconfiguredObjectKey(String mapStateKey) {
		return mapStateKey.startsWith(MapObjectDataHandler.OBJECT_NOT_CONFIGURED_IN_MAP_PREFIX);
	}
	
	private void addUnconfiguredObjectToMap(MapObjectStateProperties mapObjectStateProperties) {
		String objectType = mapObjectStateProperties.state.get(MapObjectDataHandler.TYPE_DESCRIPTION_MAP_KEY);
		switch (objectType) {
			case "Item":
				itemFactory.addItemFromSavedState(mapObjectStateProperties.state);
				break;
			default:
				throw new IllegalStateException("Unexpected type: " + objectType);
		}
	}
	
	@Override
	public void dispose() {
		renderer.dispose();
		map.dispose();
		batch.dispose();
	}
}
