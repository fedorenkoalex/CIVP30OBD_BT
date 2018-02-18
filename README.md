
#CIVP30OBD_BT


Adriuno sketch file and Android helper classes for reading sensors data and error codes Honda OBD1 ECU (tested on UDSM P30 installed on Civic '92). 

## Navigation
1. [Arduino sketch file](https://www.google.com.ua)
2. [Android helper classes](https://www.google.com.ua)
3. [Android demo application](https://www.google.com.ua)
1. [Resources](https://www.google.com.ua)

## Installation

At first you need:
- Arduino Nano (or other model)
- HC-05 Bluetooth module
- Some wires
- Power stabilizer L7809CV3 (or analogue)

Ñonnection according to scheme:

![Connection](CIVP30OBD_BT/Images/arduino_scheme.jpg "Connection")

- TX and RX from Arduino connect together and connect to K-Line pin on ECU
- Power supply connect to VIN and GND pins of Arduino (30 and 29) using L7809CV3 stabilizer
- Arduino GND connect to ECU GND
- HC-05 Bluetooth module connect with 4 wires. VCC to Arduino +5V; GND to Arduino GND; TX to Arduino D11 pin; RX to D10 pin;

How to connect to ECU. 
Under the glove box you can find 2-pin and 3-pin connectors.
Take the 3-pin connector so that the latch of the connector is on top.
GND connect to right pin. RX and TX from Arduino connect to left pin.

Next, use demo application for Android or write your own =)

Pair your Android device with Arduino, open app, choose your Arduino's Bluetooth and connect. Thats all. 

## About requests and responses

**Read sensors data**

We have 3 commands for reading data from ECU:
{0x20,0x05,0x00,0x10,0xCB}; //read first 16 bytes
{0x20,0x05,0x10,0x10,0xBB}; //read second 16 bytes
{0x20,0x05,0x20,0x10,0xAB};  //read third 16 bytes
Let's analyze
- cell 1: command
- cell 2: command length
- cell 3: reading offset 
- cell 4: count of bytes to read
- cell 5: CRC. CRC = 0x0100-(byte[0]+byte[1] +byte[2] +byte[3]);

After each command, we get a response of 24 bytes array, where:
- first 5 bytes - copy of response. Same values but in DEC
- next 2 bytes  - response length after it
- next 16 bytes - data from ecu
- last 2 bytes - CRC of response

Details for each response can be seen on the photo:


![Data ](CIVP30OBD_BT/Images/data.png "Data ")

Here are the some formulas: 

**First data response**

RPM
buffer_01[7]  è buffer_01[8]

rpm = 1875000/( buffer_01[7] *256 + buffer_01[8]+1);
or
rpm = (1875000 / (buffer_01[7] << 8 | buffer_01[8]));


Speed in kph
buffer_01[9]
take value as is

Different flags:
buffer_01[15] - flags word 1;
buffer_01[16] - flags word 2;
buffer_01[17] - flags word 3;
buffer_01[18] - flags word4;
buffer_01[19] - flags word 5;
buffer_01[20] - flags word 6;
buffer_01[21] - flags word 7;
buffer_01[22] - flags word 8;

For details look at picture:

![Flags](CIVP30OBD_BT/Images/flags.png "Flags")

For example take 4th flag word and 6th bit of it. (from right to left). At picture we see that this is Check Engine light indicator. If 6th bit in this word be 1 it means that Check Engine light is on.
In code this look like: 
boolean checkEngine = (word1[18] & 00100000) != 0

**Second data response**

Coolant temperature
buffer_02 [7]

int engineTemp = (int) (155.04149f - (buffer_02 [7] * 3.0414878f) + Math.pow(buffer_02 [7], 2) * 0.03952185f - Math.pow(buffer_02 [7], 3) * 0.00029383913f + Math.pow(buffer_02 [7], 4) * 0.0000010792568f - Math.pow(buffer_02 [7], 5) * 0.0000000015618437f);

Intake air temperature
buffer_02 [8]

int airTemp = (int) (155.04149f - (buffer_02 [8] * 3.0414878f) + Math.pow(buffer_02 [8], 2) * 0.03952185f - Math.pow(buffer_02 [8], 3) * 0.00029383913f + Math.pow(buffer_02 [8], 4) * 0.0000010792568f - Math.pow(buffer_02 [8], 5) * 0.0000000015618437f);

MAP
buffer_02 [9]

int map = (int) (buffer_02 [9] * 0.716f - 5.0f);

Atmospheric pressure
buffer_02 [10]

float atmPressure = (float) (buffer_02 [10] * 0.716f - 5.0f);

Throttle Position
buffer_02 [11]

int droccelOpen = ((buffer_02 [11] - 24) / 2);

O2 Sensor voltage
buffer_02 [12]

float o2 = (float) (buffer_02 [12] / 51.3f)

Second O2 Sensor voltage
íàïðÿæåíèå 
buffer_02 [14]

float batteryVoltage = (float) buffer_02 [14]/ 10.45f

Generator load or something like this 
buffer_02 [15]

float generatorVoltage = (float) (buffer_02 [15] / 2.55f)

ELD Load
buffer_02 [16]

float eldLoad = 77.06f - (float) (buffer_02 [16] / 2.5371f)

EGR Position
buffer_02 [18]

float egrPos = (float) (buffer_02 [18] / 51.3f)

**Third data response**

STFT
buffer_03[7]

int stCor = (int) ((buffer_03[7] / 128.0f - 1.0f) * 100.0f)

LTFT
buffer_03[9]

int ltCor = (int) ((buffer_03[9] / 128.0f - 1.0f) * 100.0f)

Injection time 2 bytes
buffer_03[11] è buffer_03[12]

double injectionTime = ((buffer_03[11] * 256.0) + buffer_03[12]) / 250.0;

ING 
buffer_03[13]

int ign = (int) ((buffer_03[13] - 128.0f) / 2.0f)

ING Limit
buffer_03[14]

int ignLimit = (int) ((buffer_03[14] - 24.0f) / 4.0f);

Valve Idle
buffer_03[15]

float valve = (float) (buffer_03[15] / 2.55f)

Valve EGR
buffer_03[18]

float valve = (float) (buffer_03[18] / 2.55f)

Position Valve EGR
buffer_03[19]

float valve = (float) (valveParam / 2.55f)

**Read errors**
Here we have two commands:
{0x20,0x05,0x40,0x10,0x8B}
and 
{0x20,0x05,0x50,0x10,0x7B}
The structure of commands and responses is the same as before


In each received byte of 16 contains the status of two errors at once.
First error is half a byte. The second half of the byte is the next error.

For example we take the first byte from first response. It contains information about 0 and 1 error codes; further in the second about 2 and 3 error codes, the third about 4 and 5 error codes and so on.

For details look at picture:

![Errors data](CIVP30OBD_BT/Images/errors.png "Errors data")





