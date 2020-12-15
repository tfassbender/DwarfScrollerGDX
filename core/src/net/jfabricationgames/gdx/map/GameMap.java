package net.jfabricationgames.gdx.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import net.jfabricationgames.gdx.character.PlayableCharacter;
import net.jfabricationgames.gdx.cutscene.CutsceneHandler;
import net.jfabricationgames.gdx.enemy.Enemy;
import net.jfabricationgames.gdx.enemy.EnemyFactory;
import net.jfabricationgames.gdx.item.Item;
import net.jfabricationgames.gdx.item.ItemFactory;
import net.jfabricationgames.gdx.object.GameObject;
import net.jfabricationgames.gdx.object.ObjectFactory;
import net.jfabricationgames.gdx.projectile.Projectile;
import net.jfabricationgames.gdx.projectile.ProjectileFactory;
import net.jfabricationgames.gdx.screens.game.GameScreen;

public class GameMap implements Disposable {
	
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
	
	public static final String MAP_MATERIALS_CONFIG_FILE = "config/map/materials.json";
	public static final int[] BACKGROUND_LAYERS = new int[] {0, 1};
	public static final int[] TERRAIN_LAYERS = new int[] {2};
	
	protected TiledMap map;
	protected Vector2 playerStartingPosition;
	
	//the lists are initialized in the factories
	protected Array<Item> items;
	protected Array<Item> itemsAboveGameObjects;
	protected Array<GameObject> objects;
	protected Array<Enemy> enemies;
	protected Array<Projectile> projectiles;
	
	protected ItemFactory itemFactory;
	protected ObjectFactory objectFactory;
	protected EnemyFactory enemyFactory;
	
	private OrthographicCamera camera;
	private OrthogonalTiledMapRenderer renderer;
	
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	
	private PlayableCharacter player;
	
	private CutsceneHandler cutsceneHandler;
	
	public GameMap(String mapAsset, OrthographicCamera camera) {
		this.camera = camera;
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		itemsAboveGameObjects = new Array<>();
		projectiles = new Array<>();
		ProjectileFactory.createInstance(this);
		
		itemFactory = new ItemFactory(this);
		objectFactory = new ObjectFactory(this);
		enemyFactory = new EnemyFactory(this);
		
		TiledMapLoader loader = new TiledMapLoader(mapAsset, this);
		loader.load();//initializes the map
		renderer = new OrthogonalTiledMapRenderer(map, GameScreen.WORLD_TO_SCREEN, batch);
		
		TiledMapPhysicsLoader mapPhysicsLoader = new TiledMapPhysicsLoader(GameScreen.SCREEN_TO_WORLD, Gdx.files.internal(MAP_MATERIALS_CONFIG_FILE));
		mapPhysicsLoader.createPhysics(map);
		
		cutsceneHandler = CutsceneHandler.getInstance();
		cutsceneHandler.setGameMap(this);
	}
	
	public void renderBackground() {
		renderer.setView(camera);
		renderer.render(BACKGROUND_LAYERS);
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
		processProjectiles(delta);
		renderProjectiles();
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
	
	private void processProjectiles(float delta) {
		for (Projectile projectile : projectiles) {
			projectile.update(delta);
		}
	}
	private void renderProjectiles() {
		for (Projectile projectile : projectiles) {
			projectile.draw(batch);
		}
	}
	
	private void renderEnemyHealthBars() {
		for (Enemy enemy : enemies) {
			enemy.drawHealthBar(shapeRenderer);
		}
	}
	
	public void renderTerrain() {
		renderer.setView(camera);
		renderer.render(TERRAIN_LAYERS);
	}
	
	public Vector2 getPlayerStartingPosition() {
		return playerStartingPosition;
	}
	
	public PlayableCharacter getPlayer() {
		return player;
	}
	
	public void setPlayer(PlayableCharacter player) {
		this.player = player;
	}
	
	public void addItem(Item item) {
		items.add(item);
	}
	
	public void addItemAboveGameObjects(Item item) {
		itemsAboveGameObjects.add(item);
	}
	
	public void removeItem(Item item) {
		items.removeValue(item, false);
		itemsAboveGameObjects.removeValue(item, false);
	}
	
	public void addObject(GameObject object) {
		objects.add(object);
		object.postAddToGameMap();
	}
	
	public void removeObject(GameObject gameObject) {
		objects.removeValue(gameObject, false);
	}
	
	public void addEnemy(Enemy enemy) {
		enemies.add(enemy);
	}
	
	public void removeEnemy(Enemy enemy) {
		enemies.removeValue(enemy, false);
	}
	
	public void addProjectile(Projectile projectile) {
		projectiles.add(projectile);
	}
	
	public void removeProjectile(Projectile projectile) {
		projectiles.removeValue(projectile, false);
	}
	
	public ItemFactory getItemFactory() {
		return itemFactory;
	}
	
	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}
	
	public EnemyFactory getEnemyFactory() {
		return enemyFactory;
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
	public void dispose() {
		renderer.dispose();
		map.dispose();
		batch.dispose();
	}
}
