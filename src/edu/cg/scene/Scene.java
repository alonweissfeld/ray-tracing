package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.*;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; // gets the values of 1, 2 and 3
	private boolean renderRefarctions = false;
	private boolean renderReflections = false;

	private PinholeCamera camera;
	private Vec ambient = new Vec(1, 1, 1); // white
	private Vec backgroundColor = new Vec(0, 0.5, 1); // blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();

	// MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec, distanceToPlain);
		return this;
	}

	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}

	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}

	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}

	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}

	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}

	public Scene initName(String name) {
		this.name = name;
		return this;
	}

	public Scene initRenderRefarctions(boolean renderRefarctions) {
		this.renderRefarctions = renderRefarctions;
		return this;
	}

	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}

	// MARK: getters
	public String getName() {
		return name;
	}

	public int getFactor() {
		return antiAliasingFactor;
	}

	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}

	public boolean getRenderRefarctions() {
		return renderRefarctions;
	}

	public boolean getRenderReflections() {
		return renderReflections;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Camera: " + camera + endl + "Ambient: " + ambient + endl + "Background Color: " + backgroundColor + endl
				+ "Max recursion level: " + maxRecursionLevel + endl + "Anti aliasing factor: " + antiAliasingFactor
				+ endl + "Light sources:" + endl + lightSources + endl + "Surfaces:" + endl + surfaces;
	}

	private transient ExecutorService executor = null;
	private transient Logger logger = null;

	private void initSomeFields(int imgWidth, int imgHeight, Logger logger) {
		this.logger = logger;
		// TODO: initialize your additional field here.
	}

	public BufferedImage render(int imgWidth, int imgHeight, double viewAngle, Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {

		initSomeFields(imgWidth, imgHeight, logger);

		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, viewAngle);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Intitialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);

		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][]) (new Future[imgHeight][imgWidth]);

		this.logger.log("Starting to shoot " + (imgHeight * imgWidth * antiAliasingFactor * antiAliasingFactor)
				+ " rays over " + name);

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);

		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}

		executor.shutdown();

		this.logger.log("Ray tracing of " + name + " has been completed.");

		executor = null;
		this.logger = null;

		return img;
	}

	private Future<Color> calcColor(int x, int y) {
		return executor.submit(() -> {
			// TODO: You need to re-implement this method if you want to handle
			// super-sampling. You're also free to change the given implementation if you
			// want.
			Point centerPoint = camera.transform(x, y);
			Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
			Vec color = calcColor(ray, 0);
			return color.toColor();
		});
	}

	private Vec calcColor(Ray ray, int recursionLevel) {
		if (recursionLevel >= maxRecursionLevel) {
			// Base case
			return new Vec();
		}

		// Get the nearest intersection with the current ray
		Hit minHit = this.findMinIntersection(ray);

		if (minHit == null){
			return this.backgroundColor;
		}

		// Defines the ambient reflections of the surface within the scene.
		Vec colorVec = minHit.getSurface().Ka().mult(this.ambient);

		// Calculates the intersections of the current intersection point
		// with the different light sources. This is done by emitting
		// rays from the current point to the light sources.
		for (Light light : this.lightSources) {
			Vec colorBySource = this.calcColorByLightSource(ray, minHit, light);
			colorVec = colorVec.add(colorBySource);
		}

		if (this.renderReflections) {
			colorVec = colorVec.add(this.calcReflections(minHit, ray, recursionLevel));
		}

		return colorVec;
	}

	/**
	 * Defines the recursive calculations for tracing reflections.
	 * Using the "Law of Reflection", we create the reflected ray from
	 * the given hit point with a surface to further color the digested pixel.
	 * @param hit - given hit
	 * @param ray - given ray
	 * @param recLevel - recursive calls limit
	 * @return a color vector.
	 */
	private Vec calcReflections(Hit hit, Ray ray, int recLevel) {
		Vec R = Ops.reflect(ray.direction(), hit.getNormalToSurface()); // reflection ray
		Vec w = new Vec(hit.getSurface().reflectionIntensity()); // reflection intensity weight

		Point sourcePoint = ray.getHittingPoint(hit);
		Vec color = this.calcColor(new Ray(sourcePoint, R), recLevel + 1);
		return color.mult(w); // Apply the surface weight
	}

	/**
	 * Iterates through all surfaces in the Scene, and returns the
	 * nearest intersection with a given ray.
	 * @param ray - given ray
	 * @return the nearest hit
	 */
	private Hit findMinIntersection(Ray ray){
		Hit minIntersection = null;
		for (Surface surface : this.surfaces){
			Hit tempHit = surface.intersect(ray);
			if (minIntersection == null || (tempHit != null && tempHit.compareTo(minIntersection) < 0 )){
				minIntersection = tempHit;
			}
		}
		return minIntersection;
	}

	/**
	 * Calculate the diffuse and specular attributes
	 * for the given intersection with the given light source.
	 * @param rayFromCamera - the ray from the camera's view
	 * @param hit - the intersection hit
	 * @param light - the light source
	 * @return a color vector.
	 */
	private Vec calcColorByLightSource(Ray rayFromCamera, Hit hit, Light light) {
		Vec color = new Vec(0);
		Point hitPoint = rayFromCamera.getHittingPoint(hit);
		Ray rayToLight = light.rayToLight(hitPoint);

		if (!isLightOccluded(light, rayToLight)) {
			// Calculate the Diffuse ans Specular color attributes
			Vec diffuseCol = this.getDiffuse(hit, rayToLight);
			Vec specularCol = this.getSpecular(hit, rayToLight, rayFromCamera);

			// Apply the light intensity weight to the physics additions.
			Vec intensity = light.intensity(hitPoint, rayToLight);
			color = color.add(diffuseCol.add(specularCol).mult(intensity));
		}

		return color;
	}

	/**
	 * Given a ray and a light source, determine if some surface
	 * in the scene blocks the ray to the light.
	 * @param light - given light
	 * @param ray - given ray to light
	 * @return boolean that answers the criteria
	 */
	private boolean isLightOccluded(Light light, Ray ray){
		for (Surface surface : this.surfaces) {
			if (light.isOccludedBy(surface, ray)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculates the diffuse color of a hit point with the ray
	 * to a light source.
	 * @param hit - given hit
	 * @param rayToLight - given ray
	 * @return a color vector.
	 */
	private Vec getDiffuse(Hit hit, Ray rayToLight) {
		Vec normal = hit.getNormalToSurface();
		Vec L = rayToLight.direction();
		Vec Kd = hit.getSurface().Kd();

		double dot = normal.dot(L);
		return (dot < 0) ? new Vec() : Kd.mult(dot);
	}

	/**
	 * Calculates the specular color of a hit point as seen from the
	 * camera point of view with a reflected light, using shininess modeling
	 * to calculate highlights on the surface at the intersection point.
	 * @param hit - given hit
	 * @param rayToLight - given ray to the light
	 * @param rayFromCamera - given ray from the camera
	 * @return a color vector.
	 */
	private Vec getSpecular(Hit hit, Ray rayToLight, Ray rayFromCamera) {
		Surface surface =  hit.getSurface();
		Vec Ks = surface.Ks();
		int n = surface.shininess();

		Vec V = rayFromCamera.direction();
		Vec Lc = Ops.reflect(rayToLight.direction().neg(), hit.getNormalToSurface());

		double dot = Lc.dot(V.neg());
		return (dot < 0) ? new Vec() : Ks.mult(Math.pow(dot, n));
	}
}
