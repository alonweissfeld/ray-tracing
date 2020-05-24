package edu.cg.scene.camera;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;

public class PinholeCamera {
	private Point cameraPosition;
	private double distanceToPlain;

	private Vec upVec;
	private Vec rightVec;

	private double plainWidth;
	private Point plainCenterPoint;

	private int resolutionX;
	private int resolutionY;

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

		this.upVec = upVec.normalize();
		this.rightVec = towardsVec.normalize().cross(upVec).normalize();

		this.distanceToPlain = distanceToPlain;
		this.plainCenterPoint = cameraPosition.add(towardsVec.normalize().mult(distanceToPlain));
	}

	/**
	 * Initializes the resolution and width of the image.
	 * 
	 * @param height    - the number of pixels in the y direction.
	 * @param width     - the number of pixels in the x direction.
	 * @param viewAngle - the view Angle.
	 */
	public void initResolution(int height, int width, double viewAngle) {
		this.resolutionX = width;
		this.resolutionY = height;
		this.plainWidth = 2.0 * Math.tan(Math.toRadians(viewAngle / 2.0)) * this.distanceToPlain;
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
		double pixelWidth = this.plainWidth / this.resolutionX;

		double rightFactor = pixelWidth * (x - (this.resolutionX / 2.0));
		double upFactor = pixelWidth * ((this.resolutionY / 2.0) - y);

		return plainCenterPoint
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
