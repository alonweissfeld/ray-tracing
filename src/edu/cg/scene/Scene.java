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
		// TODO: decide whether we want to improve finding intersection - fast reject
		// base case
		if (recursionLevel >= maxRecursionLevel) {
			return new Vec();
		}

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

		return colorVec;
	}

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
	 * for the given point with the given light source.
	 * @param point
	 * @param light
	 * @return
	 */
	private Vec calcColorByLightSource(Ray rayFromCamera, Hit hit, Light light) {
		Vec color = new Vec(0);
		Point hitPoint = rayFromCamera.getHittingPoint(hit);
		Ray rayToLight = light.rayToLight(hitPoint);

		if (!isLightOccluded(light, rayToLight)) {
			Vec intensity = light.intensity(hitPoint, rayToLight);
			color = color.add(this.getDiffuse(hit, rayToLight, intensity));
			color = color.add(this.getSpecular(hit, rayToLight, rayFromCamera, intensity));
		}

		return color;
	}

	private boolean isLightOccluded(Light light, Ray ray){
		for (Surface surface : this.surfaces) {
			if (light.isOccludedBy(surface, ray)){
				return true;
			}
		}
		return false;
	}

	private Vec getDiffuse(Hit hit, Ray rayToLight, Vec intensity) {
		Vec normal = hit.getNormalToSurface();
		Vec L = rayToLight.direction();
		Vec Kd = hit.getSurface().Kd();
		return Kd.mult(intensity).mult(normal.dot(L));
	}

	private Vec getSpecular(Hit hit, Ray rayToLight, Ray rayFromCamera, Vec intensity) {
		Surface s =  hit.getSurface();
		Vec Ks = s.Ks();
		int n = s.shininess();

		Vec V = rayFromCamera.direction();
		Vec Lc = Ops.reflect(rayToLight.direction().neg(), hit.getNormalToSurface());

		return Ks.mult(intensity).mult(Math.pow(V.dot(Lc), n));
	}
}
