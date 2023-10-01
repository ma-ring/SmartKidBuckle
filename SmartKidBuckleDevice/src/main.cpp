#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

#define SERVICE_UUID "19B10000-E8F2-537E-4F6C-D104768A1214"
#define CHARACTERISTIC_UUID "0fc10cb8-0518-40dd-b5c3-c4637815de40"
#define BUCKLE_CONNECT_PIN 7

BLEServer* pServer = NULL;
BLECharacteristic* pChara = NULL;

bool deviceConnected = false;
bool oldDeviceConnected = false;

bool mBuckle_prestate = LOW; //HIGH:close LOW:open

class ServerCallback : public BLEServerCallbacks{
  void onConnect(BLEServer* pServer){
    deviceConnected = true;
    USBSerial.println("Device Connected");

  }
  
  void onDisconnect(BLEServer* pServer){
    deviceConnected = false;
    USBSerial.println("Device disconnected");

  }
};

void setup() {
  // put your setup code here, to run once:
  //M5.begin();
  //M5.Power.begin();
  USBSerial.begin(9600);
  USBSerial.println("Start device");
  pinMode(BUCKLE_CONNECT_PIN, INPUT);

  BLEDevice::init("StartKidBuckle_device");
  
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallback());

  BLEService* pService = pServer->createService(SERVICE_UUID);

  pChara = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    //BLECharacteristic::PROPERTY_READ |
    //BLECharacteristic::PROPERTY_WRITE |
    BLECharacteristic::PROPERTY_NOTIFY //|
    //BLECharacteristic::PROPERTY_INDICATE
    );

  pChara->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising* pAdvertising = pServer->getAdvertising();
  pAdvertising->start();

}

void loop() {
  // put your main code here, to run repeatedly:
  if(deviceConnected){
    
    if(digitalRead(BUCKLE_CONNECT_PIN) != mBuckle_prestate){
      mBuckle_prestate = digitalRead(BUCKLE_CONNECT_PIN);
      uint8_t num = mBuckle_prestate ? 1:0;
      pChara->setValue(&num,1);
      pChara->notify();

      USBSerial.println("Send MSG");

    }
  }
  //if(digitalRead(BUCKLE_CONNECT_PIN) == HIGH){
  //   USBSerial.println("HIGH");

  //}
  if(deviceConnected != oldDeviceConnected){
    if(deviceConnected){
      //Device Connected

    }else{
      //Device disconnected

    }
    oldDeviceConnected = deviceConnected;
  }
}

