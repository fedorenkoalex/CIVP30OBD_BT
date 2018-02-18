package com.obdhondascan.util;

import com.obdhondascan.Constants;
import com.obdhondascan.model.OBDDevice;

import java.util.Calendar;

/**
 * Created by fedor on 06.12.2017.
 */

public class FuelUtil {
    
    private static long prevTime;

    public static void calculateFuel(OBDDevice device, float injection_cc) {
        boolean userAlternative = new PreferencesHelper().getBoolean(Constants.ALTERNATE_RPM, false);
        if (device.getRPM(userAlternative) > 0 && device.getInjectionTime() > 0f) {

            float fuelPerHourNow = (float) (injection_cc * (float) device.getRPM(userAlternative) * device.getInjectionTime() * 0.000002f);                     //current fuel per hour
            device.setFuelPerHourNow(fuelPerHourNow);
        } else {
            device.setFuelPerHourNow(0);
        }
        if (prevTime == 0) {
            prevTime = Calendar.getInstance().getTimeInMillis();
        }
        long allTime = Calendar.getInstance().getTimeInMillis() - prevTime;

        if (device.getSpeed() > 0) {
            float distanceTmp = ((float) device.getSpeed() * ((float) allTime / 3600000.0f));
            float distanceTotal = device.getDistanceTotal() + distanceTmp;
            device.setDistanceTotal(distanceTotal);
        }

        if (device.getFuelPerHourNow() > 0f) {
            float fuelPerHourTemp = (device.getFuelPerHourNow() * ((float) allTime / 3600000.0f));
            float fuelTotalSpended = device.getFuelSpended() + fuelPerHourTemp;                      //all spended fuel
            device.setFuelSpended(fuelTotalSpended);
        } else {
            device.setFuelPerHourNow(0);
        }
        if ((device.getFuelPerHourNow() > 0) && (device.getSpeed() > 0)) {
            float fuelPerKmNow = (float) (device.getFuelPerHourNow() * 100.0f / ((float) device.getSpeed() + 1.0f));             // current fuel per hKm
            device.setFuelPerHKmNow(fuelPerKmNow);
        } else {
            device.setFuelPerHKmNow(0);
        }
        if (device.getDistanceTotal() > 0) {
            float fuelPerKmMed = (100.0f * device.getFuelSpended()) / device.getDistanceTotal();  // medium fuel per hKm
            device.setFuelPerHkmMed(fuelPerKmMed);
        } else {
            device.setFuelPerHkmMed(0);
        }

        prevTime = Calendar.getInstance().getTimeInMillis();
    }

}
