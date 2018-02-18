#include <SoftwareSerial.h>


SoftwareSerial BTSerial(10, 11); // RX | TX 
//ecu data variables
const unsigned int COMMAND_SIZE = 5; //размер команды
unsigned int COMMAND_DELAY = 50; //задержка после отправки
// ecu commands
byte cmd_01[COMMAND_SIZE]={0x20,0x05,0x00,0x10,0xCB}; //ECU Command 01
byte cmd_02[COMMAND_SIZE]={0x20,0x05,0x10,0x10,0xBB}; //ECU Command 02
byte cmd_03[COMMAND_SIZE]={0x20,0x05,0x20,0x10,0xAB}; //ECU Command 03
//команды для получения ошибок
byte cmd_04[COMMAND_SIZE]={0x20,0x05,0x40,0x10,0x8B}; //ECU Command 04
byte cmd_05[COMMAND_SIZE]={0x20,0x05,0x50,0x10,0x7B}; //ECU Command 05

//буферы для хранения ответа от ecu
const unsigned int BUFFER_SIZE = 24;
byte buffer_01[BUFFER_SIZE];
byte buffer_02[BUFFER_SIZE];
byte buffer_03[BUFFER_SIZE];
byte buffer_04[BUFFER_SIZE];
byte buffer_05[BUFFER_SIZE];

//переменные для таймаутов
unsigned long startLoopTime = 0;
unsigned long deltaTime = 0;
unsigned long responceWaitingTime = 0;

//переменные температуры
float inTemp = 0;
float outTemp = 0;

//тип команды
unsigned int mode = 0; // default mode 0 = default, 1 = error check

void setup()
{
  Serial.begin(9600);
  BTSerial.begin(9600);
}

void loop()
{
  if (BTSerial.available()){
      mode = BTSerial.read();
  }
  
  startLoopTime = millis();
  BTSerial.print("");
  if (mode == 0){
     requestEcu();
     sendData();
  } else if (mode == 1){
        requestErrors();
        sendErrors();
        mode = 0;
  } 
}




//пакуем данные в удобный формат и шлем через БТ
void sendData(){ 
    String word1String = byteArrayToString(buffer_01);
    String word2String = byteArrayToString(buffer_02);
    String word3String = byteArrayToString(buffer_03);
    deltaTime = millis() - startLoopTime;
    String result = (String) "{\"mode\" : 0, \"word1\" : " + word1String + ", \"word2\" : " + word2String + ", \"word3\" : " + word3String + ", \"time\" : " + deltaTime + ", \"inTemp\" : " + inTemp + ", \"outTemp\" : " + outTemp + "}"; 
    BTSerial.println(result);
}

//пакуем ошибки в удобный формат и шлем чере БТ
void sendErrors(){
    String word4String = byteArrayToString(buffer_04);
    String word5String = byteArrayToString(buffer_05);
    deltaTime = millis() - startLoopTime;
    String result = (String) "{\"mode\" : 1, \"word4\" : " + word4String + ", \"word5\" : " + word5String + ", \"time\" : " + deltaTime + "}"; 
    BTSerial.println(result);
}

//шлем запрос на ошибки и сохраняем в буфер. 2 команды по очереди
void requestErrors(){
  //send command err 1 to Ecu
   for (int i=0; i < BUFFER_SIZE; i++)
    {
       buffer_04[i] = 0;
    }
  
    Serial.write(cmd_04,COMMAND_SIZE);
    Serial.flush();
    responceWaitingTime = millis();
    while(Serial.available() < BUFFER_SIZE && (millis() - responceWaitingTime < 3000)){}
    if(Serial.available() >= BUFFER_SIZE)
    {
      for (int i=0; i < BUFFER_SIZE; i++)
      {
         buffer_04[i] = Serial.read();
      }
    }
    delay(COMMAND_DELAY);

    //send command err 2 to Ecu
   for (int i=0; i < BUFFER_SIZE; i++)
    {
       buffer_05[i] = 0;
    }
  
    Serial.write(cmd_05,COMMAND_SIZE);
    Serial.flush();
    responceWaitingTime = millis();
    while(Serial.available() < BUFFER_SIZE && (millis() - responceWaitingTime < 3000)){}
    if(Serial.available() >= BUFFER_SIZE)
    {
      for (int i=0; i < BUFFER_SIZE; i++)
      {
         buffer_05[i] = Serial.read();
      }
    }
    delay(COMMAND_DELAY);
}

//трансформируем массив байтов в строку
String byteArrayToString(byte inputData[]){
  String result = "[";
  for (int i=0; i < BUFFER_SIZE; i++)
  {
      result +=String(inputData[i]);
      if (i != (BUFFER_SIZE -1)){
        result += ", ";
      }
  }
  result += "]";
  return result;
}

//шлем запрос на получение данных и сохраняем. 3 команды по очереди
void requestEcu(){

   //send command 1 to Ecu
   for (int i=0; i < BUFFER_SIZE; i++)
    {
       buffer_01[i] = 0;
    }
  
    Serial.write(cmd_01,COMMAND_SIZE);
    Serial.flush();
    responceWaitingTime = millis();
    while(Serial.available() < BUFFER_SIZE && (millis() - responceWaitingTime < 3000)){}
    if(Serial.available() >= BUFFER_SIZE)
    {
      for (int i=0; i < BUFFER_SIZE; i++)
      {
         buffer_01[i] = Serial.read();
      }
    }
    delay(COMMAND_DELAY);
    //send command 2 to Ecu
    for (int i=0; i < BUFFER_SIZE; i++)
    {
       buffer_02[i] = 0;
    }  
    Serial.write(cmd_02,COMMAND_SIZE);
    Serial.flush();
    responceWaitingTime = millis();
    while(Serial.available() < BUFFER_SIZE && (millis() - responceWaitingTime < 3000)){}
    if(Serial.available() >= BUFFER_SIZE)
    {
      for (int i=0; i < BUFFER_SIZE; i++)
      {
         buffer_02[i] = Serial.read();
      }
    }
    delay(COMMAND_DELAY);
    // send command 3 to Ecu
    for (int i=0; i < BUFFER_SIZE; i++)
    {
       buffer_03[i] = 0;
    }  
    Serial.write(cmd_03,COMMAND_SIZE);
    Serial.flush();
    responceWaitingTime = millis();
    while(Serial.available() < BUFFER_SIZE && (millis() - responceWaitingTime < 3000)){}
    if(Serial.available() >= BUFFER_SIZE)
    {
      for (int i=0; i < BUFFER_SIZE; i++)
      {
         buffer_03[i] = Serial.read();
      }
   }
   delay(COMMAND_DELAY);
}
