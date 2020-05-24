package edu.cg.scene.lightSources;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.objects.Surface;

public class CutoffSpotlight extends PointLight {
	private Vec direction;
	private double cutoffAngle;

	public CutoffSpotlight(Vec dirVec, double cutoffAngle) {
		this.direction = dirVec;
		this.cutoffAngle = cutoffAngle;
	}

	public CutoffSpotlight initDirection(Vec direction) {
		this.direction = direction;
		return this;
	}

	public CutoffSpotlight initCutoffAngle(double cutoffAngle) {
		this.cutoffAngle = cutoffAngle;
		return this;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Spotlight: " + endl + description() + "Direction: " + direction + endl;
	}

	@Override
	public CutoffSpotlight initPosition(Point position) {
		return (CutoffSpotlight) super.initPosition(position);
	}

	@Override
	public CutoffSpotlight initIntensity(Vec intensity) {
		return (CutoffSpotlight) super.initIntensity(intensity);
	}

	@Override
	public CutoffSpotlight initDecayFactors(double q, double l, double c) {
		return (CutoffSpotlight) super.initDecayFactors(q, l, c);
	}

	@Override
	public boolean isOccludedBy(Surface surface, Ray rayToLight) {
		double cosineAngle = this.cosGamma(rayToLight);
		return cosineAngle < Ops.epsilon || super.isOccludedBy(surface, rayToLight);
	}

	@Override
	public Vec intensity(Point hittingPoint, Ray rayToLight) {
		// Extract the angle in degrees from the cos(gamma) value obtained
		// from the dot product of the direction of the light with the given ray.
		double cosineAngle = this.cosGamma(rayToLight);
		double degAngle = Math.toDegrees(Math.acos(cosineAngle));

		if (cosineAngle < Ops.epsilon || degAngle > this.cutoffAngle) {
			return new Vec(0);
		}

		Vec i = super.intensity(hittingPoint, rayToLight);
		return i.mult(cosineAngle);
	}

	/**
	 * Calculate the cosine of the angle between the given ray and the
	 * direction of ths spotlight.
	 * @param rayToLight
	 * @return
	 */
	public double cosGamma(Ray rayToLight) {
		Vec Vd = this.direction.normalize().neg();
		Vec V = rayToLight.direction().normalize();
		return V.dot(Vd);
	}
}
