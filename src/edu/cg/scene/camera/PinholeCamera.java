package edu.cg.scene.camera;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;

public class PinholeCamera {
	Point cameraPosition;
	Vec towardsVec;
	Vec upVec;
	double distanceToPlain;
	Vec rightVec;
	Point centerPoint;
	int plainHeight;
	int plainWidth;
	double pixelWidth;

	/**
	 * Initializes a pinhole camera model with default resolution 200X200 (RxXRy)
	 * and View Angle 90.
	 * 
	 * @param cameraPosition  - The position of the camera.
	 * @param towardsVec      - The towards vector of the camera (not necessarily
	 *                        normalized).
	 * @param upVec           - The up vector of the camera.
	 * @param distanceToPlain - The distance of the camera (position) to the center
	 *                        point of the image-plain.
	 * 
	 */
	public PinholeCamera(Point cameraPosition, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.cameraPosition = cameraPosition;
		this.towardsVec = towardsVec.normalize();
		this.upVec = upVec.normalize();
		this.distanceToPlain = distanceToPlain;
		this.rightVec = towardsVec.cross(upVec).normalize();
		this.centerPoint = cameraPosition.add(towardsVec.normalize().mult(distanceToPlain));

	}

	/**
	 * Initializes the resolution and width of the image.
	 * 
	 * @param height    - the number of pixels in the y direction.
	 * @param width     - the number of pixels in the x direction.
	 * @param viewAngle - the view Angle.
	 */
	public void initResolution(int height, int width, double viewAngle) {
		this.plainHeight = height;
		this.plainWidth = width;
		this.pixelWidth = this.plainWidth / viewAngle;

	}

	/**
	 * Transforms from pixel coordinates to the center point of the corresponding
	 * pixel in model coordinates.
	 * 
	 * @param x - the pixel index in the x direction.
	 * @param y - the pixel index in the y direction.
	 * @return the middle point of the pixel (x,y) in the model coordinates.
	 */
	public Point transform(int x, int y) {
		double rightFactor = pixelWidth * (x - (this.plainWidth / 2.0));
		// TODO: check upFactor - heightAngle, and either we need (int) casting
		double upFactor = pixelWidth * ((this.plainHeight / 2.0) - y);

		return centerPoint
				.add(upVec.mult(upFactor))
				.add(rightVec.mult(rightFactor));
	}

	/**
	 * Returns the camera position
	 * 
	 * @return a new point representing the camera position.
	 */
	public Point getCameraPosition() {
		return new Point(cameraPosition.x, cameraPosition.y, cameraPosition.z);
	}
}
