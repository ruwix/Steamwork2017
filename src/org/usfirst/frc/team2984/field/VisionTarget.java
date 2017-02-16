package org.usfirst.frc.team2984.field;

import java.awt.geom.Point2D;

import org.usfirst.frc.team2984.robot.Camera;
import org.usfirst.frc.team2984.robot.util.Dimension;

/**
 * A rectangular vision target, representing either an angled or head-on orientation. Use
 * this class to determine distance and internal/external angles to a physical target.
 * 
 * @author rich
 */
public class VisionTarget {
	private double offset;
	private double width;
	private double height;
	
	/**
	 * A rectangular vision target, representing either an angled or head-on orientation.
	 * Units are pixels.
	 * 
	 * @param offset distance from horizontal center; may be positive, negative, or zero
	 * @param width positive value indicates left side larger than right, negative
	 * value indicates right side larger than left
	 * @param height positive value only
	 */
	public VisionTarget(double offset, double width, double height) {
		this.offset = offset;
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Points use Cartesian coordinate system.
	 * 
	 * @param topLeft
	 * @param topRight
	 * @param bottomLeft
	 * @param bottomRight
	 */
	public VisionTarget(Point2D topLeft, Point2D topRight, Point2D bottomLeft, Point2D bottomRight) {
		double averageLeft = (topLeft.getX() + bottomLeft.getX()) / 2;
		double averageRight = (topRight.getX() + bottomRight.getX()) / 2;
		double averageTop = (topLeft.getY() + topRight.getY()) / 2;
		double averageBottom = (bottomLeft.getY() + bottomRight.getY()) / 2;
		
		this.offset = averageLeft + ((averageRight - averageLeft) / 2);
		this.width = averageRight - averageLeft;
		this.height = averageTop - averageBottom;
	}
	
	/**
	 * See: http://wpilib.screenstepslive.com/s/4485/m/24194/l/288985-identifying-and-processing-the-targets
	 * 
	 * @param camera
	 * @param physicalTargetSize the size of the physical target, expressed in inches
	 * @return the distance to the target in inches
	 */
	public double getDistance(Camera camera, Dimension physicalTargetSize) {
		double theta = Math.toRadians(camera.angleOfView.height) / 2;
		
		return this.getVerticalFieldOfView(camera, physicalTargetSize) / (2 * Math.tan(theta));
	}
	
	/**
	 * Returns the apparent rotation of the camera about the target, in radians.
	 * 
	 * @param camera
	 * @param physicalTargetSize the dimensions of the physical target, in inches
	 * @return the apparent rotation of the camera about the target, in radians
	 */
	public double getInternalRotation(Camera camera, Dimension physicalTargetSize) {
		double distance = this.getDistance(camera,  physicalTargetSize);
		
		return Math.asin(this.offset / distance);
	}
	
	/**
	 * Returns the apparent rotation of the target, in radians and within the range
	 * 0 to π. Values from 0 to π/2 indicate the target has turned right.
	 * Values from 0 to π indicate the target has turned left.
	 * 
	 * @param camera
	 * @param physicalTargetSize the dimensions of the physical vision target, in inches
	 * @return the external rotation of the camera, in radians
	 */
	public double getExternalRotation(Camera camera, Dimension physicalTargetSize) {
		double expectedWidth = this.height * (physicalTargetSize.width / physicalTargetSize.height);
		double cosine = this.width / expectedWidth;
		
		return Math.acos(cosine);
	}
	
	/**
	 * @return center of this VisionTarget, in pixels
	 */
	public double getCenter() {
		return this.offset;
	}
	
	/**
	 * 
	 * @return width of this VisionTarget, in pixels
	 */
	public double getWidth() {
		return this.width;
	}
	
	/**
	 * @return height of this VisionTarget, in pixels
	 */
	public double getHeight() {
		return this.height;
	}
	
	/**
	 * @return vertical field of view, in inches
	 */
	private double getVerticalFieldOfView(Camera camera, Dimension objectSize) {
		return camera.resolution.height * objectSize.height / this.height;
	}
}