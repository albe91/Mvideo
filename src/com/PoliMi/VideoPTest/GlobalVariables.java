package com.PoliMi.VideoPTest;

import android.app.Application;

public class GlobalVariables extends Application{
	   private int id;
	   private int startingBatteryLvl;
	   private int voltage_before;
	   
	   public int getID(){
	     return this.id;
	   }
	 
	   public void setID(int d){
	     this.id=d;
	   }

	   public int getStartingBatteryLvl() {
		   return startingBatteryLvl;
	   }

	   public void setStartingBatteryLvl(int startingBatteryLvl) {
		   this.startingBatteryLvl = startingBatteryLvl;
	   }

	public int getVoltage_before() {
		return voltage_before;
	}

	public void setVoltage_before(int voltage_before) {
		this.voltage_before = voltage_before;
	}

}
