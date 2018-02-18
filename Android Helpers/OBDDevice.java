package com.obdhondascan.model;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by AlexFedorenko on 01.12.2017.
 */

//device data model
public class OBDDevice {


    private String address;
    private String name;

    private int mode;

    private long commandDelay;

    private int[] word1;
    private int[] word2;
    private int[] word3;

    private int[] errorWord1;
    private int[] errorWord2;

    private float inTemp;
    private float outTemp;

    private float fuelPerHourNow;
    private float distanceTotal;
    private float fuelSpended;
    private float fuelPerHKmNow;
    private float fuelPerHkmMed;

    private float hp;
    private int gear;

    private float temperature = -1f;
    private float pressure = -1f;


    public OBDDevice(String name, String address) {
        this.address = address;
        this.name = name;
    }

    public int getGear() {
        return gear;
    }

    public void setGear(int gear) {
        this.gear = gear;
    }

    public boolean isWeatherValid() {
        return temperature != -1f && pressure != -1f;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getHp() {
        return hp;
    }

    public void setHp(float hp) {
        this.hp = hp;
    }

    public float getFuelPerHourNow() {
        return fuelPerHourNow;
    }

    public void setFuelPerHourNow(float fuelPerHourNow) {
        this.fuelPerHourNow = fuelPerHourNow;
    }

    public float getDistanceTotal() {
        return distanceTotal;
    }

    public void setDistanceTotal(float distanceTotal) {
        this.distanceTotal = distanceTotal;
    }

    public float getFuelSpended() {
        return fuelSpended;
    }

    public void setFuelSpended(float fuelSpended) {
        this.fuelSpended = fuelSpended;
    }

    public float getFuelPerHKmNow() {
        return fuelPerHKmNow;
    }

    public void setFuelPerHKmNow(float fuelPerHKmNow) {
        this.fuelPerHKmNow = fuelPerHKmNow;
    }

    public float getFuelPerHkmMed() {
        if (fuelPerHkmMed >= 50f) {
            return 49.9f;
        }
        return fuelPerHkmMed;
    }

    public void setFuelPerHkmMed(float fuelPerHkmMed) {
        this.fuelPerHkmMed = fuelPerHkmMed;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getInTemp() {
        return inTemp;
    }

    public float getOutTemp() {
        return outTemp;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public long getCommandDelay() {
        return commandDelay;
    }

    public int[] getWord1() {
        return word1;
    }

    public int[] getWord2() {
        return word2;
    }

    public int[] getWord3() {
        return word3;
    }

    public int[] getErrorWord1() {
        return errorWord1;
    }

    public int[] getErrorWord2() {
        return errorWord2;
    }

    //set received from Arduino data and parse it
    public boolean setResponse(String response) {
        if (TextUtils.isEmpty(response)) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (!jsonObject.has("mode")) {
                return false;
            }
            mode = jsonObject.getInt("mode");
            commandDelay = jsonObject.getLong("time");
            if (mode == 0) {
                parseMainData(jsonObject);
                return true;
            } else if (mode == 1) {
                parseErrorData(jsonObject);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    //get data
    private void parseMainData(JSONObject response) {
        try {
            JSONArray word1String = response.getJSONArray("word1");
            word1 = new int[word1String.length()];
            for (int i = 0; i < word1String.length(); i++) {
                word1[i] = (byte) word1String.getInt(i);
            }
            JSONArray word2String = response.getJSONArray("word2");
            word2 = new int[word2String.length()];
            for (int i = 0; i < word2String.length(); i++) {
                word2[i] = word2String.getInt(i);
            }
            JSONArray word3String = response.getJSONArray("word3");
            word3 = new int[word3String.length()];
            for (int i = 0; i < word3String.length(); i++) {
                word3[i] = word3String.getInt(i);
            }
            inTemp = (float) response.getDouble("inTemp");
            outTemp = (float) response.getDouble("outTemp");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //get errors
    private void parseErrorData(JSONObject response) {
        try {
            JSONArray word4String = response.getJSONArray("word4");
            errorWord1 = new int[word4String.length()];
            for (int i = 0; i < word4String.length(); i++) {
                errorWord1[i] = word4String.getInt(i);
            }
            JSONArray word5String = response.getJSONArray("word5");
            errorWord2 = new int[word5String.length()];
            for (int i = 0; i < word5String.length(); i++) {
                errorWord2[i] = word5String.getInt(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testErrors(int[] word1, int[] word2) {
        errorWord1 = word1;
        errorWord2 = word2;
    }

    //get real error codes
    public List<OBDError> getParsedErrorsList() {
        if (errorWord1 == null || errorWord1.length == 0) {
            return new ArrayList<>();
        }
        if (errorWord2 == null || errorWord2.length == 0) {
            return new ArrayList<>();
        }
        //cut headers and footers
        int[] errorWord1NoHeaders = Arrays.copyOfRange(errorWord1, 7, 23);
        int[] errorWord2NoHeaders = Arrays.copyOfRange(errorWord2, 7, 23);

        List<OBDError> errorList = new ArrayList<>();
        for (int i = 0; i < errorWord1NoHeaders.length; i++) {
            int errorWordVal = errorWord1NoHeaders[i];
            int positionErrorId = (i * 2);
            boolean isError1Valid = (errorWordVal & 15) != 0;
            if (isError1Valid) {
                OBDError obdError1 = new OBDError();
                obdError1.setError(true);
                obdError1.setErrorId(positionErrorId);
                errorList.add(obdError1);
            }
            boolean isError2Valid = (errorWordVal & 240) != 0;
            if (isError2Valid) {
                OBDError obdError2 = new OBDError();
                obdError2.setError(true);
                obdError2.setErrorId(positionErrorId + 1);
                errorList.add(obdError2);
            }
        }
        for (int i = 0; i < errorWord2NoHeaders.length; i++) {
            int errorWordVal = errorWord2NoHeaders[i];
            int positionErrorId = (i * 2) + ((errorWord1NoHeaders.length * 2)); // offset of prev array
            boolean isError1Valid = (errorWordVal & 15) != 0;
            if (isError1Valid) {
                OBDError obdError1 = new OBDError();
                obdError1.setError(true);
                obdError1.setErrorId(positionErrorId);
                errorList.add(obdError1);
            }
            boolean isError2Valid = (errorWordVal & 240) != 0;
            if (isError2Valid) {
                OBDError obdError2 = new OBDError();
                obdError2.setError(true);
                obdError2.setErrorId(positionErrorId + 1);
                errorList.add(obdError2);
            }
        }
        return errorList;
    }

    //response parse functions
    public int getRPM(boolean userAlternative) {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        int rpmParam1 = word1[7];
        int rpmParam2 = word1[8];


        if (rpmParam1 > 0 || rpmParam2 > 0) {
            int rpm = 0;
            if (userAlternative) {
                rpm = (1875000 / (Math.abs(rpmParam1) << 8 | Math.abs(rpmParam2)));
            } else {
                rpm = (1875000 / (Math.abs(rpmParam1) * 256 + Math.abs(rpmParam2) + 1));
            }
            return rpm;
        } else {
            return 0;
        }
    }

    public int getSpeed() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return Math.abs(word1[9]);
    }

    public boolean getStarterFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[15] & 1) != 0;
    }

    public boolean getAirConditionerFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[15] & 2) != 0;
    }

    public boolean getPASPPressureFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[15] & 4) != 0;
    }

    public boolean getBrakeFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[15] & 8) != 0;
    }

    public boolean getP_NFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[15] & 16) != 0;
    }

    public boolean getVtecPressureFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (Math.abs(word1[15]) & 128) != 0;
    }

    public boolean getSCSFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[16] & 8) != 0;
    }

    public boolean getVtecFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (Math.abs(word1[17]) & 4) != 0;
    }

    public boolean getMainRelayFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[18] & 1) != 0;
    }

    public boolean getAccClutchFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[18] & 2) != 0;
    }

    public boolean getO2HeatFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[18] & 4) != 0;
    }

    public boolean getCheckEngineFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (Math.abs(word1[18]) & 32) != 0;
    }

    public boolean getClosedLoopFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[22] & 1) != 0;
    }

    public boolean getAltCFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[19] & 1) != 0;
    }

    public boolean getFanFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[19] & 2) != 0;
    }

    public boolean getIABFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[19] & 4) != 0;
    }

    public boolean getVtec_EFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[19] & 8) != 0;
    }

    public boolean getEconoFlag() {
        if (word1 == null || word1.length == 0) {
            return false;
        }
        return (word1[19] & 128) != 0;
    }

    public int getFlagsWord1() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return word1[15];
    }


    public int getFlagsWord2() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return word1[16];
    }

    public int getFlagsWord3() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return word1[17];
    }

    public int getFlagsWord4() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return word1[18];
    }

    public int getFlagsWord5() {
        if (word1 == null || word1.length == 0) {
            return 0;
        }
        return word1[19];
    }

    public int getEngineCoolantTemp() {
        if (word2 == null || word2.length == 0) {
            return 0;
        }
        int coolantParam = word2[7];
        if (coolantParam > 0) {
            int engineTemp = (int) (155.04149f - (coolantParam * 3.0414878f) + Math.pow(coolantParam, 2) * 0.03952185f - Math.pow(coolantParam, 3) * 0.00029383913f + Math.pow(coolantParam, 4) * 0.0000010792568f - Math.pow(coolantParam, 5) * 0.0000000015618437f);
            return engineTemp;
        } else {
            return 0;
        }
    }

    public int getAirIntakeTemp() {
        if (word2 == null || word2.length == 0) {
            return 0;
        }
        int intakeParam = word2[8];
        if (intakeParam > 0) {
            int airTemp = (int) (155.04149f - (intakeParam * 3.0414878f) + Math.pow(intakeParam, 2) * 0.03952185f - Math.pow(intakeParam, 3) * 0.00029383913f + Math.pow(intakeParam, 4) * 0.0000010792568f - Math.pow(intakeParam, 5) * 0.0000000015618437f);
            return airTemp;
        } else {
            return 0;
        }
    }

    public float getAtmospherePressure() {
        if (word2 == null || word2.length == 0) {
            return 0;
        }
        int pressureParam = word2[10];
        float atmPressure = (pressureParam * 0.716f - 5.0f); // atm pressure im kPa
        return atmPressure;
    }

    public int getThrottleOpening() {
        if (word2 == null || word2.length == 0) {
            return 0;
        }
        int droccelParam = word2[11];
        int droccelOpen = (int) ((droccelParam - 24.0f) / 2.0f);
        return droccelOpen;
    }

    public double getInjectionTime() {
        if (word3 == null || word3.length == 0) {
            return 0f;
        }
        int injParam1 = word3[11];
        int injParam2 = word3[12];
        if (injParam1 > 0 || injParam2 > 0) {
            double injectionTime = ((injParam1 * 256.0) + injParam2) / 250.0;
            return injectionTime;
        } else {
            return 0f;
        }
    }

    public float getBatteryVoltage() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int batteryParam = word2[14];
        if (batteryParam > 0) {
            float batteryVoltage = (float) batteryParam / 10.45f;
            return batteryVoltage;
        } else {
            return 0f;
        }
    }

    public float getGeneratorLoad() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int generatorParam = word2[15];
        if (generatorParam > 0) {
            float generatorVoltage = generatorParam / 2.55f;
            return generatorVoltage;
        } else {
            return 0f;
        }
    }

    public float getELDLoad() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int eldParam = word2[16];
        if (eldParam > 0) {
            float eldLoad = 77.06f - (eldParam / 2.5371f);
            return eldLoad;
        } else {
            return 0f;
        }
    }

    public float getEgrPosition() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int egrPosParam = word2[18];
        if (egrPosParam > 0) {
            float egrPos = egrPosParam / 51.3f;
            return egrPos;
        } else {
            return 0f;
        }
    }

    public float getMapPressure() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int mapParam = word2[9];
        if (mapParam > 0) {
            float map = mapParam * 0.716f - 5.0f;
            return map;
        } else {
            return 0f;
        }
    }

    public int getMapPressureInt() {
        if (word2 == null || word2.length == 0) {
            return 0;
        }
        int mapParam = word2[9];
        if (mapParam > 0) {
            int map = (int) (mapParam * 0.716f - 5.0f);
            return map;
        } else {
            return 0;
        }
    }

    public float getFirstO2Voltage() {
        if (word2 == null || word2.length == 0) {
            return 0f;
        }
        int o2VParam = word2[12];
        if (o2VParam > 0) {
            float o2 = o2VParam / 51.3f;
            return o2;
        } else {
            return 0f;
        }
    }

    public float getShortCorrection() {
        if (word3 == null || word3.length == 0) {
            return 0;
        }
        int stCorParam = word3[7];
        if (stCorParam > 0) {
            int stCor = (int) ((stCorParam / 128.0f - 1.0f) * 100.0f);
            return stCor;
        } else {
            return 0;
        }
    }

    public float getLongCorrection() {
        if (word3 == null || word3.length == 0) {
            return 0;
        }
        int ltCorParam = word3[9];
        if (ltCorParam > 0) {
            int ltCor = (int) ((ltCorParam / 128.0f - 1.0f) * 100.0f);
            return ltCor;
        } else {
            return 0;
        }
    }

    public int getIGN() {
        if (word3 == null || word3.length == 0) {
            return 0;
        }
        int ignParam = word3[13];
        if (ignParam > 0) {
            int ign = (int) ((ignParam - 128.0f) / 2.0f);
            return ign;
        } else {
            return 0;
        }
    }

    public int getIGNLimit() {
        if (word3 == null || word3.length == 0) {
            return 0;
        }
        int ignParam = word3[14];
        if (ignParam > 0) {
            int ignLimit = (int) ((ignParam - 24.0f) / 4.0f);
            return ignLimit;
        } else {
            return 0;
        }
    }

    public float getValveIdle() {
        if (word3 == null || word3.length == 0) {
            return 0f;
        }
        int valveParam = word3[15];
        if (valveParam > 0) {
            float valve = valveParam / 2.55f;
            return valve;
        } else {
            return 0f;
        }
    }

    public float getValveEGR() {
        if (word3 == null || word3.length == 0) {
            return 0f;
        }
        int valveParam = word3[18];
        if (valveParam > 0) {
            float valve = valveParam / 2.55f;
            return valve;
        } else {
            return 0f;
        }
    }

    public float getPositionValveEGR() {
        if (word3 == null || word3.length == 0) {
            return 0f;
        }
        int valveParam = word3[19];
        if (valveParam > 0) {
            float valve = valveParam / 2.55f;
            return valve;
        } else {
            return 0f;
        }
    }

    //not sure
    public float getAirFuelRatio() {
        float o2Voltage = getFirstO2Voltage();
        return o2Voltage * 29.4f;//14.7f;
    }


    //logging functions

    private File createDirectory() {
        String logDirectory = Environment.getExternalStorageDirectory().toString() + File.separator + "HondaOBD_logs";
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            try {
                directory.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory;
    }

    public void saveToLogFile() {
        StringBuilder stringBuilder = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        File directory = createDirectory();
        boolean isCheckEngine = getCheckEngineFlag();
        String fileName = "logs_" + calendar.get(Calendar.DAY_OF_MONTH) + "_" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.YEAR) + ".txt";
        if (isCheckEngine) {
            fileName = "logsEngineLight_" + calendar.get(Calendar.DAY_OF_MONTH) + "_" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.YEAR) + ".txt";
        }
        File logFile = new File(directory, fileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                stringBuilder.append("File: " + fileName);
                stringBuilder.append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (word1 == null || word1.length == 0) {
            return;
        }
        if (word2 == null || word2.length == 0) {
            return;
        }
        if (word3 == null || word3.length == 0) {
            return;
        }
        stringBuilder.append("[");
        for (int i = 0; i < word1.length; i++) {
            stringBuilder.append("" + word1[i]);
            if (i != word1.length - 1) {
                stringBuilder.append(",");
            }
        }
        stringBuilder.append("]");
        stringBuilder.append("\n");
        stringBuilder.append("[");
        for (int i = 0; i < word2.length; i++) {
            stringBuilder.append("" + word2[i]);
            if (i != word2.length - 1) {
                stringBuilder.append(",");
            }
        }
        stringBuilder.append("]");
        stringBuilder.append("\n");
        stringBuilder.append("[");
        for (int i = 0; i < word3.length; i++) {
            stringBuilder.append("" + word3[i]);
            if (i != word3.length - 1) {
                stringBuilder.append(",");
            }
        }
        stringBuilder.append("]");
        stringBuilder.append("\n");
        stringBuilder.append("\n");

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(stringBuilder.toString());
            buf.newLine();
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void saveErrorsToLogFile() {
        StringBuilder stringBuilder = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        File directory = createDirectory();
        String fileName = "error_logs_" + calendar.get(Calendar.DAY_OF_MONTH) + "_" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.YEAR) + ".txt";
        File logFile = new File(directory, fileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                //add header
                stringBuilder.append("File: " + fileName);
                stringBuilder.append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stringBuilder.append("Error word 1:");
        stringBuilder.append("\n");
        if (errorWord1 != null && errorWord1.length > 0) {
            for (int i = 0; i < errorWord1.length; i++) {
                stringBuilder.append("" + errorWord1[i]);
                stringBuilder.append(",");
            }
        } else {
            stringBuilder.append("NO_DATA");
        }

        stringBuilder.append("\n");
        stringBuilder.append("Error word 2:");
        stringBuilder.append("\n");
        if (errorWord2 != null && errorWord2.length > 0) {
            for (int i = 0; i < errorWord2.length; i++) {
                stringBuilder.append("" + errorWord2[i]);
                stringBuilder.append(",");
            }
        } else {
            stringBuilder.append("NO_DATA");
        }

        stringBuilder.append("\n");
        stringBuilder.append("\n");

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(stringBuilder.toString());
            buf.newLine();
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
