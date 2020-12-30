package net.jfabricationgames.gdx.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.JsonIterator;
import com.badlogic.gdx.utils.ObjectMap;

import net.jfabricationgames.gdx.physics.PhysicsBodyCreator;
import net.jfabricationgames.gdx.physics.PhysicsCollisionType;

/**
 * Populates the Box2D world with static bodies using data from a map object.
 * 
 * It uses a JSON formatted materials file to assign properties to the static bodies it creates. In case no material property is found, it'll use a
 * default one.
 */
public class TiledMapPhysicsLoader {
	
	public static final String MAP_MATERIALS_CONFIG_FILE = "config/map/materials.json";
	public static final String MAP_GROUND_TYPES_CONFIG_FILE = "config/map/ground_types.json";
	
	private static final String LAYER_NAME_SOLID_OBJECT_PHYSICS = "physics";
	private static final String LAYER_NAME_GROUND_PHYSICS = "ground_physics";
	
	private static final String MAP_PROPERTY_KEY_MATERIAL = "material";
	private static final String MAP_PROPERTY_KEY_GROUND_TYPE = "ground";
	
	private static final FixtureDef GROUND_FIXTURE_DEF = createGroundFixtureDef();
	
	private static FixtureDef createGroundFixtureDef() {
		FixtureDef groundFixtureDef = new FixtureDef();
		
		// the sensor flag is set to false, so the collision is detected in the preSolve method of the colliding objects
		// the real collision between the objects is to be avoided by disabling the contact (see https://stackoverflow.com/questions/31213746/box2d-sensors-contact-callback-in-each-step)
		// the colliding types are defined by the mask PhysicsCollisionType.MAP_GROUND.mask
		groundFixtureDef.isSensor = false;
		
		groundFixtureDef.filter.categoryBits = PhysicsCollisionType.MAP_GROUND.category;
		groundFixtureDef.filter.maskBits = PhysicsCollisionType.MAP_GROUND.mask;
		return groundFixtureDef;
	}
	
	private float worldUnitsPerPixel;
	private ObjectMap<String, FixtureDef> materials;
	private ObjectMap<String, GameMapGroundType> groundTypes;
	
	/**
	 * @param unitsPerPixel
	 *        conversion ratio from pixel units to box2D metres.
	 */
	public TiledMapPhysicsLoader(float unitsPerPixel) {
		this.worldUnitsPerPixel = unitsPerPixel;
		materials = new ObjectMap<String, FixtureDef>();
		
		Gdx.app.log(getClass().getSimpleName(), "--- Loading map physics objects ---------------------------------------------------");
		
		loadMaterials();
		loadGroundTypes();
	}
	
	public void createPhysics(Map map) {
		createSolidObjectPhysics(map);
		createGroundPhysics(map);
	}
	
	private void createSolidObjectPhysics(Map map) {
		MapLayer layer = map.getLayers().get(LAYER_NAME_SOLID_OBJECT_PHYSICS);
		
		if (layer == null) {
			Gdx.app.error(getClass().getSimpleName(), "layer " + LAYER_NAME_SOLID_OBJECT_PHYSICS + " does not exist");
			return;
		}
		
		for (MapObject object : layer.getObjects()) {
			
			if (object instanceof TextureMapObject) {
				continue;
			}
			
			Shape shape;
			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyDef.BodyType.StaticBody;
			
			if (object instanceof RectangleMapObject) {
				RectangleMapObject rectangle = (RectangleMapObject) object;
				shape = getRectangle(rectangle);
			}
			else if (object instanceof PolygonMapObject) {
				shape = getPolygon((PolygonMapObject) object);
			}
			else if (object instanceof PolylineMapObject) {
				shape = getPolyline((PolylineMapObject) object);
			}
			else if (object instanceof CircleMapObject) {
				shape = getCircle((CircleMapObject) object);
			}
			else {
				Gdx.app.error(getClass().getSimpleName(), "non suported shape " + object);
				continue;
			}
			
			MapProperties properties = object.getProperties();
			String material = properties.get(MAP_PROPERTY_KEY_MATERIAL, String.class);
			FixtureDef fixtureDef = null;
			if (material != null) {
				fixtureDef = materials.get(material);
			}
			
			if (fixtureDef == null) {
				fixtureDef = materials.get("default");
			}
			
			fixtureDef.shape = shape;
			
			Body body = PhysicsBodyCreator.createBody(bodyDef);
			body.createFixture(fixtureDef);
			
			fixtureDef.shape = null;
			shape.dispose();
		}
	}
	
	private void createGroundPhysics(Map map) {
		MapLayer layer = map.getLayers().get(LAYER_NAME_GROUND_PHYSICS);
		
		if (layer == null) {
			Gdx.app.error(getClass().getSimpleName(), "layer " + LAYER_NAME_GROUND_PHYSICS + " does not exist");
			return;
		}
		
		for (MapObject object : layer.getObjects()) {
			
			if (object instanceof TextureMapObject) {
				continue;
			}
			
			Shape shape;
			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyDef.BodyType.StaticBody;
			
			if (object instanceof RectangleMapObject) {
				RectangleMapObject rectangle = (RectangleMapObject) object;
				shape = getRectangle(rectangle);
			}
			else if (object instanceof PolygonMapObject) {
				shape = getPolygon((PolygonMapObject) object);
			}
			else if (object instanceof PolylineMapObject) {
				shape = getPolyline((PolylineMapObject) object);
			}
			else if (object instanceof CircleMapObject) {
				shape = getCircle((CircleMapObject) object);
			}
			else {
				Gdx.app.error(getClass().getSimpleName(), "non suported shape " + object);
				continue;
			}
			
			MapProperties properties = object.getProperties();
			String groundType = properties.get(MAP_PROPERTY_KEY_GROUND_TYPE, String.class);
			
			GROUND_FIXTURE_DEF.shape = shape;
			Body body = PhysicsBodyCreator.createBody(bodyDef);
			body.createFixture(GROUND_FIXTURE_DEF);
			body.setUserData(groundTypes.get(groundType));
			GROUND_FIXTURE_DEF.shape = null;
			
			shape.dispose();
		}
	}
	
	private Shape getRectangle(RectangleMapObject rectangleObject) {
		Rectangle rectangle = rectangleObject.getRectangle();
		PolygonShape polygon = new PolygonShape();
		Vector2 size = new Vector2((rectangle.x + rectangle.width * 0.5f) / worldUnitsPerPixel,
				(rectangle.y + rectangle.height * 0.5f) / worldUnitsPerPixel);
		polygon.setAsBox(rectangle.width * 0.5f / worldUnitsPerPixel, rectangle.height * 0.5f / worldUnitsPerPixel, size, 0.0f);
		return polygon;
	}
	
	private Shape getCircle(CircleMapObject circleObject) {
		Circle circle = circleObject.getCircle();
		CircleShape circleShape = new CircleShape();
		circleShape.setRadius(circle.radius / worldUnitsPerPixel);
		circleShape.setPosition(new Vector2(circle.x / worldUnitsPerPixel, circle.y / worldUnitsPerPixel));
		return circleShape;
	}
	
	private Shape getPolygon(PolygonMapObject polygonObject) {
		PolygonShape polygon = new PolygonShape();
		float[] vertices = polygonObject.getPolygon().getTransformedVertices();
		
		float[] worldVertices = new float[vertices.length];
		
		for (int i = 0; i < vertices.length; ++i) {
			worldVertices[i] = vertices[i] / worldUnitsPerPixel;
		}
		
		polygon.set(worldVertices);
		return polygon;
	}
	
	private Shape getPolyline(PolylineMapObject polylineObject) {
		float[] vertices = polylineObject.getPolyline().getTransformedVertices();
		Vector2[] worldVertices = new Vector2[vertices.length / 2];
		
		for (int i = 0; i < vertices.length / 2; ++i) {
			worldVertices[i] = new Vector2();
			worldVertices[i].x = vertices[i * 2] / worldUnitsPerPixel;
			worldVertices[i].y = vertices[i * 2 + 1] / worldUnitsPerPixel;
		}
		
		ChainShape chain = new ChainShape();
		chain.createChain(worldVertices);
		return chain;
	}
	
	private void loadMaterials() {
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 1.0f;
		fixtureDef.restitution = 0.0f;
		fixtureDef.filter.categoryBits = PhysicsCollisionType.MAP_OBJECT.category;
		fixtureDef.filter.maskBits = PhysicsCollisionType.MAP_OBJECT.mask;
		materials.put("default", fixtureDef);
		
		Gdx.app.log(getClass().getSimpleName(), "loading materials file: " + MAP_MATERIALS_CONFIG_FILE);
		
		try {
			JsonReader reader = new JsonReader();
			JsonValue root = reader.parse(Gdx.files.internal(MAP_MATERIALS_CONFIG_FILE));
			JsonIterator materialIterator = root.iterator();
			
			while (materialIterator.hasNext()) {
				JsonValue materialValue = materialIterator.next();
				
				if (!materialValue.has("name")) {
					Gdx.app.error(getClass().getSimpleName(), "found a material without a name");
					continue;
				}
				
				String name = materialValue.getString("name");
				
				fixtureDef = new FixtureDef();
				fixtureDef.density = materialValue.getFloat("density", 1.0f);
				fixtureDef.friction = materialValue.getFloat("friction", 1.0f);
				fixtureDef.restitution = materialValue.getFloat("restitution", 0.0f);
				fixtureDef.filter.categoryBits = PhysicsCollisionType.MAP_OBJECT.category;
				fixtureDef.filter.maskBits = PhysicsCollisionType.MAP_OBJECT.mask;
				
				Gdx.app.debug(getClass().getSimpleName(), "adding material: " + name);
				materials.put(name, fixtureDef);
			}
			
		}
		catch (Exception e) {
			Gdx.app.error(getClass().getSimpleName(), "error loading config file '" + MAP_MATERIALS_CONFIG_FILE + "': " + e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadGroundTypes() {
		Gdx.app.log(getClass().getSimpleName(), "loading ground types file: " + MAP_GROUND_TYPES_CONFIG_FILE);
		
		try {
			Json json = new Json();
			groundTypes = json.fromJson(ObjectMap.class, GameMapGroundType.class, Gdx.files.internal(MAP_GROUND_TYPES_CONFIG_FILE));
		}
		catch (Exception e) {
			Gdx.app.error(getClass().getSimpleName(), "error loading config file '" + MAP_GROUND_TYPES_CONFIG_FILE + "': " + e.getMessage());
		}
	}
}