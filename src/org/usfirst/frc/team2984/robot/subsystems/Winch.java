package org.usfirst.frc.team2984.robot.subsystems;

import org.usfirst.frc.team2984.robot.RobotMap;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Winch extends Subsystem {
	private static Winch instance;
	
	private CANTalon talon;
	
	public static Winch getInstance() {
		if (instance == null) {
			CANTalon talon = new CANTalon(RobotMap.WINCH_MOTOR_ID);
			instance = new Winch(talon);
		}
		
		return instance;
	}
	
	public Winch(CANTalon talon) {
		this.talon = talon;
	}
    
    public void grab(){
    	talon.set(0.2);
    }
    
    public void climb(){
    	talon.set(1);
    }
    
    public void backDown(){
    	talon.set(-1);
    }

	@Override
	protected void initDefaultCommand() {
		// TODO Auto-generated method stub
	}
}