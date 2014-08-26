package com.PoliMi.VideoPTest;

import android.app.Application;

public class GlobalVariables extends Application{
	   private int id;
	   
	   public int getID(){
	     return this.id;
	   }
	 
	   public void setID(int d){
	     this.id=d;
	   }

}
