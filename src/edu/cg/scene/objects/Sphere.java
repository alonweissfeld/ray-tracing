package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.*;

public class Sphere extends Shape {
	private Point center;
	private double radius;
	
	public Sphere(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}
	
	public Sphere() {
		this(new Point(0, -0.5, -6), 0.5);
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Sphere:" + endl + 
				"Center: " + center + endl +
				"Radius: " + radius + endl;
	}
	
	public Sphere initCenter(Point center) {
		this.center = center;
		return this;
	}
	
	public Sphere initRadius(double radius) {
		this.radius = radius;
		return this;
	}
	
	@Override
	public Hit intersect(Ray ray) {
		double b = ray.direction().mult(2.0).dot(ray.source().sub(this.center));
		double c = ray.source().distSqr(this.center) - Math.pow(this.radius, 2);
		// a = 1
		double delta = Math.pow(b, 2) - 4 * c;
		if (delta >= 0) {
			double t0 = ( - b - Math.sqrt(delta)) / 2.0;
			double t1 = ( - b + Math.sqrt(delta)) / 2.0;
			double minT = t0;

			if (t0 < Ops.epsilon && t1 < Ops.epsilon){
				// no intersection found
				return null;
			}

			Vec normal; // The normal to the sphere at the intersection point.
			boolean isWithinSphere = false;

			if (t0 < Ops.epsilon && t1 >= Ops.epsilon){
				// the ray source point is within the sphere
				minT = t1;
				isWithinSphere = true;
				normal = this.normalize(ray.add(t1)).neg();

			} else {
				// we found two intersection points, we take the closest one = t0.
				normal = this.normalize(ray.add(t0));
			}

			Hit hit = new Hit(minT, normal);
			hit.setIsWithin(isWithinSphere);
			return hit;
		}
		return null;
	}

	/**
	 * Calculates the normal at a given point on the sphere.
	 * @param p - given Point p
	 * @return normalized vector.
	 */
	private Vec normalize(Point p){
		return Ops.normalize(p.sub(this.center));
	}
}
