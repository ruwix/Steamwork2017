package org.usfirst.frc.team2984.robot.subsystems;

import org.usfirst.frc.team2984.robot.RobotMap;
import org.usfirst.frc.team2984.robot.commands.RemoteJoystickDrive;
import org.usfirst.frc.team2984.robot.util.MathUtil;
import org.usfirst.frc.team2984.robot.util.Motion;

import com.ctre.CANTalon;
import com.ctre.CANTalon.FeedbackDevice;
import com.ctre.CANTalon.TalonControlMode;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Wheel orientation:
 * 
 *  grabber
 *  
 * \\\   ///
 * 
 * 
 * ///   \\\
 *
 *   winch
 */
public class DriveTrain extends Subsystem {
	private static DriveTrain instance;
	private Gyroscope gyro;
	
	private double speed = RobotMap.DRIVE_TRAIN_MAX_SPEED;
	private double ticksPerInchForward = RobotMap.DRIVE_TRAIN_TICK_TO_INCH_FORWARD;
	private double ticksPerInchRight = RobotMap.DRIVE_TRAIN_TICK_TO_INCH_RIGHT;
	private double ticksPerRadian = RobotMap.DRIVE_TRAIN_TICK_TO_RADIAN;
	private CANTalon frontLeft;
	private CANTalon frontRight;
	private CANTalon backLeft;
	private CANTalon backRight;
	
	private State driveState;
	
	public static enum State {
		SPEED_CONTROL,
		DISTANCE_CONTROL,
		VOLTAGE_CONTROL
	}
	
	public static DriveTrain getInstance() {
		if (instance == null) {
			CANTalon frontLeft = new CANTalon(RobotMap.FRONT_LEFT_MOTOR_ID);
			CANTalon frontRight = new CANTalon(RobotMap.FRONT_RIGHT_MOTOR_ID);
			CANTalon rearLeft = new CANTalon(RobotMap.REAR_LEFT_MOTOR_ID);
			CANTalon rearRight = new CANTalon(RobotMap.REAR_RIGHT_MOTOR_ID);
			
			instance = new DriveTrain(frontLeft, frontRight, rearLeft, rearRight, Gyroscope.getInstance());
		}
		return instance;
	}
	
	/**
	 * @param frontLeft
	 * @param frontRight
	 * @param backLeft
	 * @param backRight
	 */
	public DriveTrain(CANTalon frontLeft, CANTalon frontRight, CANTalon backLeft, CANTalon backRight, Gyroscope gyro) {
		super("drive-train");
		
		this.frontLeft = frontLeft;
		this.frontRight = frontRight;
		this.backLeft = backLeft;
		this.backRight = backRight;
		this.switchState(State.SPEED_CONTROL);
		
		this.gyro = gyro;
	}
	
	public void move(Motion motion) {
		this.switchState(State.SPEED_CONTROL);
		double fl = motion.getX() + motion.getY() + motion.getRotation();
		double fr = -motion.getX() + motion.getY() - motion.getRotation();
		double bl = -motion.getX() + motion.getY() + motion.getRotation();
		double br = motion.getX() + motion.getY() - motion.getRotation();
		double max = getMaximumValue(fl, fr, bl, br);
		
		if (max > 1) {
			fl = fl / max;
			fr = fr / max;
			bl = bl / max;
			br = br / max;
		}
		
		this.frontLeft.set(fl * this.speed);
		this.frontRight.set(fr * this.speed);
		this.backRight.set(br * this.speed);
		this.backLeft.set(bl * this.speed);
	}
	
	/**
	 * Moves at a field-centric angle, speed and rotates the robot to a desired angle
	 * @param angle the angle to move at, clockwise
	 * @param speed the speed [0, 1] to move at
	 * @param rotation the rotation to rotate at, this is a speed not an angle;
	 */
	public void moveAtAngle(double angle, double speed, double rotation){
		this.switchState(State.SPEED_CONTROL);
		double gyroAngle = this.gyro.getAngle();
		double angleRadian = -Math.toRadians(angle - gyroAngle);
		double x = 0;
		double y = speed;
		double xPrime = x * Math.cos(angleRadian) - y * Math.sin(angleRadian);
		double yPrime = x * Math.sin(angleRadian) + y * Math.cos(angleRadian);
		Motion motion = new Motion(xPrime, yPrime, rotation);
		this.move(motion);
	}
	
	/**
	 * Moves at a desired speed in the local x and y directions while holding the given angle.
	 * @param angle the angle to move at, clockwise
	 * @param speed the speed [0, 1] to move at
	 * @param rotation the rotation to rotate at, this is a speed not an angle;
	 */
	public void moveWithFixedAngle(double x, double y, double angle){
		this.switchState(State.SPEED_CONTROL);
		double gyroAngle = this.gyro.getAngle();
		double rotation = MathUtil.shortestDeltaAngle(gyroAngle, angle) * RobotMap.ROBOT_ANGLE_PROPORIONAL_SCALAR;
		Motion motion = new Motion(x, y, Math.max(Math.min(rotation, 1), -1));
		this.move(motion);
	}
	
	
	
	/**
	 * moves the given distance in x and y
	 * @param x in inches how far right
	 * @param y in inches how far forward
	 */
	public void moveToDistance(double x, double y){
		this.switchState(State.SPEED_CONTROL);
		double xTicks = x*this.ticksPerInchForward;
		double yTicks = y*this.ticksPerInchRight;
		double fl = xTicks + yTicks;
		double fr = -xTicks + yTicks;
		double bl = -xTicks + yTicks;
		double br = xTicks + yTicks;
		double flD = fl - this.frontLeft.getEncPosition();
		double frD = fr + this.frontRight.getEncPosition();
		double blD = bl - this.backLeft.getEncPosition();
		double brD = br + this.backRight.getEncPosition();
		flD *= RobotMap.DISTANCE_P;
		frD *= RobotMap.DISTANCE_P;
		blD *= RobotMap.DISTANCE_P;
		brD *= RobotMap.DISTANCE_P;
		flD = cap(flD, RobotMap.MAX_SPEED_DITANCE/12);
		frD = cap(frD, RobotMap.MAX_SPEED_DITANCE/12);
		blD = cap(blD, RobotMap.MAX_SPEED_DITANCE/12);
		brD = cap(brD, RobotMap.MAX_SPEED_DITANCE/12);
		this.frontLeft.set(flD * RobotMap.DRIVE_TRAIN_MAX_SPEED);
		this.frontRight.set(frD * RobotMap.DRIVE_TRAIN_MAX_SPEED);
		this.backRight.set(brD * RobotMap.DRIVE_TRAIN_MAX_SPEED);
		this.backLeft.set(blD * RobotMap.DRIVE_TRAIN_MAX_SPEED);
		SmartDashboard.putString("WTF", flD + "," + frD + "," + blD + "," + brD + " " + System.currentTimeMillis() );
	}
	
	private double cap(double x, double mag){
		return Math.min(Math.max(x, -mag), mag);
	}
	
	public void rotate(double angle){
		this.switchState(State.DISTANCE_CONTROL);
		double ticks = angle*this.ticksPerRadian;
		double fl = ticks;
		double fr = -ticks;
		double bl = ticks;
		double br = -ticks;
		this.frontLeft.set(fl);
		this.frontRight.set(fr);
		this.backRight.set(br);
		this.backLeft.set(bl);
	}
	
	public void resetOrigin(){
		this.frontLeft.setEncPosition(0);
		this.frontRight.setEncPosition(0);
		this.backLeft.setEncPosition(0);
		this.backRight.setEncPosition(0);
	}
	
	public boolean isThere(double epsilon, double x, double y){
		double xTicks = x*this.ticksPerInchForward;
		double yTicks = y*this.ticksPerInchRight;
		double flEnc = xTicks + yTicks;
		double frEnc = -xTicks + yTicks;
		double blEnc = -xTicks + yTicks;
		double brEnc = xTicks + yTicks;
		double flEncDelta = Math.abs(flEnc - this.frontLeft.getEncPosition());
		double frEncDelta = Math.abs(frEnc + this.frontRight.getEncPosition());
		double blEncDelta = Math.abs(blEnc - this.backLeft.getEncPosition());
		double brEncDelta = Math.abs(brEnc + this.backRight.getEncPosition());
		int fl = this.frontLeft.getEncVelocity();
		int fr = this.frontRight.getEncVelocity();
		int bl = this.backLeft.getEncVelocity();
		int br = this.backRight.getEncVelocity();
		fl = Math.abs(fl);
		fr = Math.abs(fr);
		bl = Math.abs(bl);
		br = Math.abs(br);
		double max = getMaximumValue(fl, fr, bl, br);
		double maxDist = getMaximumValue(flEncDelta, frEncDelta, blEncDelta, brEncDelta);
		return max < epsilon && maxDist < epsilon;
	}
	
	public boolean isThereAtAll(double epsilon){
		int fl = this.frontLeft.getEncVelocity();
		int fr = this.frontRight.getEncVelocity();
		int bl = this.backLeft.getEncVelocity();
		int br = this.backRight.getEncVelocity();
		fl = Math.abs(fl);
		fr = Math.abs(fr);
		bl = Math.abs(bl);
		br = Math.abs(br);
		double max = Math.min(Math.min(fl, fr), Math.min(bl, br));
		return max < epsilon;
	}
	
	@Override
	protected void initDefaultCommand() {
		setDefaultCommand(new RemoteJoystickDrive());
	}
	
	private double getMaximumValue(double first, double second, double third, double fourth) {
		return Math.max(Math.max(first, second), Math.max(third, fourth));
	}
	
	private void configureTalonsSpeed(){
		double f = RobotMap.SPEED_F;
		double p = RobotMap.SPEED_P;
		double i = RobotMap.SPEED_I;
		double d = RobotMap.SPEED_D;

		updatePID(f, p, i, d);
		this.frontLeft.changeControlMode(TalonControlMode.Speed);
		this.frontRight.changeControlMode(TalonControlMode.Speed);
		this.backLeft.changeControlMode(TalonControlMode.Speed);
		this.backRight.changeControlMode(TalonControlMode.Speed);
		this.frontLeft.configPeakOutputVoltage(+12.0f, -12.0f);
		this.frontRight.configPeakOutputVoltage(+12.0f, -12.0f);
		this.backLeft.configPeakOutputVoltage(+12.0f, -12.0f);
		this.backRight.configPeakOutputVoltage(+12.0f, -12.0f);

	}
	
	private void configureTalonsVoltage(){
		this.configureTalonVoltage(this.frontLeft);
		this.configureTalonVoltage(this.frontRight);
		this.configureTalonVoltage(this.backLeft);
		this.configureTalonVoltage(this.backRight);
	}
	
	private void configureTalonVoltage(CANTalon talon){
		//Limit the max current, this case to [+12, -12]
		talon.configNominalOutputVoltage(+0.0f, -0.0f);
        talon.configPeakOutputVoltage(+12.0f, -12.0f);
        talon.changeControlMode(TalonControlMode.PercentVbus);
	}
	
	private void configureTalonsDistance(){
		double f = RobotMap.DISTANCE_F;
		double p = RobotMap.DISTANCE_P;
		double i = RobotMap.DISTANCE_I;
		double d = RobotMap.DISTANCE_D;
		
		updatePID(f, p, i, d);
		SmartDashboard.putString("WMode", "DIST" + p + "," + f);
		this.frontLeft.changeControlMode(TalonControlMode.Position);
		this.frontRight.changeControlMode(TalonControlMode.Position);
		this.backLeft.changeControlMode(TalonControlMode.Position);
		this.backRight.changeControlMode(TalonControlMode.Position);
		this.frontLeft.configPeakOutputVoltage(RobotMap.MAX_SPEED_DITANCE, -RobotMap.MAX_SPEED_DITANCE);
		this.frontRight.configPeakOutputVoltage(RobotMap.MAX_SPEED_DITANCE, -RobotMap.MAX_SPEED_DITANCE);
		this.backLeft.configPeakOutputVoltage(RobotMap.MAX_SPEED_DITANCE, -RobotMap.MAX_SPEED_DITANCE);
		this.backRight.configPeakOutputVoltage(RobotMap.MAX_SPEED_DITANCE, -RobotMap.MAX_SPEED_DITANCE);
	}
	
	private void setupEncoderAndPID(CANTalon talon, boolean reversed, double f, double p, double i, double d){
		//Setup Sensor
		talon.setFeedbackDevice(FeedbackDevice.QuadEncoder); //CRT Mag Encoder Relative if 1 turn
		talon.reverseSensor(reversed);
		talon.configEncoderCodesPerRev(1000); //number of revs per turn, 1000
		talon.enableForwardSoftLimit(false);
		talon.enableReverseSoftLimit(false);
		talon.enableLimitSwitch(false, false);
		
		//Limit the max current, this case to [+12, -12]
		talon.configNominalOutputVoltage(+0.0f, -0.0f);
        talon.configPeakOutputVoltage(+12.0f, -12.0f);
		talon.setCloseLoopRampRate(38);
        
        //Set up the PID values
        talon.setProfile(0);
        talon.setF(f); // 0.1597
        talon.setP(p); // 0.42
        talon.setI(i); 
        talon.setD(d);
	}
	
	public void switchState(State state){
		if(state == this.driveState){
			return;
		}
		if(state == null){
			state = this.driveState;
		}
		switch(state){
			case VOLTAGE_CONTROL:
				this.configureTalonsVoltage();
				break;
			case SPEED_CONTROL:
				this.configureTalonsSpeed();
				break;
			case DISTANCE_CONTROL:
				this.configureTalonsDistance();
		}
		this.driveState = state;
	}
	
	/**
	 * Returns the displacement in the Y direction based on the encoder position in inches.
	 * @return The displacement in the Y in inches.
	 */
	public double getDisplacementY(){
		double fl = this.frontLeft.getEncPosition();
		double fr = this.frontRight.getEncPosition();
		double bl = this.backLeft.getEncPosition();
		double br = this.backRight.getEncPosition();
		return (fl + fr + bl + br)/4D/RobotMap.DRIVE_TRAIN_TICK_TO_INCH_FORWARD;
	}
	
	/**
	 * Returns the displacement in the X direction based on the encoder position in inches.
	 * @return The displacement in the X in inches.
	 */
	public double getDisplacementX(){
		double fl = this.frontLeft.getEncPosition();
		double fr = this.frontRight.getEncPosition();
		double bl = this.backLeft.getEncPosition();
		double br = this.backRight.getEncPosition();
		return (fl - fr - bl + br)/4D/RobotMap.DRIVE_TRAIN_TICK_TO_INCH_FORWARD;
	}
	
	public void updatePID(double f, double p, double i, double d){
		this.setupEncoderAndPID(this.frontLeft, false, f, p, i, d);
		this.setupEncoderAndPID(this.frontRight, true, f, p, i, d);
		this.setupEncoderAndPID(this.backLeft, false, f, p, i, d);
		this.setupEncoderAndPID(this.backRight, true, f, p, i, d);
	}
	
	public State getState(){
		return this.driveState;
	}
}