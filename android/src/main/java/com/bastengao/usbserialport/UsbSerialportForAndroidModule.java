package com.bastengao.usbserialport;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = UsbSerialportForAndroidModule.NAME)
public class UsbSerialportForAndroidModule extends ReactContextBaseJavaModule {
  public static final String NAME = "UsbSerialportForAndroid";
  private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";

  public static final String CODE_DEVICE_NOT_FOND = "device_not_found";
  public static final String CODE_DRIVER_NOT_FOND = "driver_not_found";
  public static final String CODE_NOT_ENOUGH_PORTS = "not_enough_ports";
  public static final String CODE_PERMISSION_DENIED = "permission_denied";
  public static final String CODE_OPEN_FAILED = "open_failed";
  public static final String CODE_DEVICE_NOT_OPEN = "device_not_open";
  public static final String CODE_SEND_FAILED = "send_failed";
  public static final String CODE_DEVICE_NOT_OPEN_OR_CLOSED = "device_not_open_or_closed";

  private final ReactApplicationContext reactContext;
  private final Map<Integer, UsbSerialPortWrapper> usbSerialPorts = new HashMap<>();

  public UsbSerialportForAndroidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("CODE_DEVICE_NOT_FOND", CODE_DEVICE_NOT_FOND);
    constants.put("CODE_DRIVER_NOT_FOND", CODE_DRIVER_NOT_FOND);
    constants.put("CODE_NOT_ENOUGH_PORTS", CODE_NOT_ENOUGH_PORTS);
    constants.put("CODE_PERMISSION_DENIED", CODE_PERMISSION_DENIED);
    constants.put("CODE_OPEN_FAILED", CODE_OPEN_FAILED);
    constants.put("CODE_DEVICE_NOT_OPEN", CODE_DEVICE_NOT_OPEN);
    constants.put("CODE_SEND_FAILED", CODE_SEND_FAILED);
    constants.put("CODE_DEVICE_NOT_OPEN_OR_CLOSED", CODE_DEVICE_NOT_OPEN_OR_CLOSED);
    return constants;
  }

  @ReactMethod
  public void list(Promise promise) {
    WritableArray devices = Arguments.createArray();
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    for (UsbDevice device : usbManager.getDeviceList().values()) {
      WritableMap d = Arguments.createMap();
      d.putInt("deviceId", device.getDeviceId());
      d.putInt("vendorId", device.getVendorId());
      d.putInt("productId", device.getProductId());
      d.putString("name", device.getProductName());
      d.putString("deviceName", device.getDeviceName());
      d.putString("productName", device.getProductName());
      d.putString("version", device.getVersion());
      d.putString("manufacturerName", device.getManufacturerName());
      devices.pushMap(d);
    }
    promise.resolve(devices);
  }

  @ReactMethod
  public void tryRequestPermission(int deviceId, Promise promise) {
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    UsbDevice device = findDevice(deviceId);
    if (device == null) {
      promise.reject(CODE_DEVICE_NOT_FOND, "device not found");
      return;
    }

    if (usbManager.hasPermission(device)) {
      promise.resolve(1);
      return;
    }

    PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getCurrentActivity(), 0,
        new Intent(INTENT_ACTION_GRANT_USB), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    usbManager.requestPermission(device, usbPermissionIntent);
    promise.resolve(0);
  }

  @ReactMethod
  public void hasPermission(int deviceId, Promise promise) {
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    UsbDevice device = findDevice(deviceId);
    if (device == null) {
      promise.reject(CODE_DEVICE_NOT_FOND, "device not found");
      return;
    }

    promise.resolve(usbManager.hasPermission(device));
  }

  @ReactMethod
  public void open(int deviceId, int baudRate, int dataBits, int stopBits, int parity, Promise promise) {
    UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
    if (wrapper != null) {
      WritableMap result = Arguments.createMap();
      result.putInt("deviceId", deviceId);
      result.putString("productName", wrapper.getPort().getDriver().getDevice().getProductName());
      promise.resolve(result);
      return;
    }

    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    UsbDevice device = findDevice(deviceId);
    if (device == null) {
      promise.reject("DEVICE_NOT_FOUND", "Device not found");
      return;
    }

    ProbeTable customTable = new ProbeTable();
    customTable.addProduct(0x2341, 0x0043, CdcAcmSerialDriver.class); // Arduino Uno
    customTable.addProduct(0x1a86, 0x7523, Ch34xSerialDriver.class); // CH340
    customTable.addProduct(0x10c4, 0xea60, Cp21xxSerialDriver.class); // CP210x
    customTable.addProduct(0x0403, 0x6001, FtdiSerialDriver.class); // FTDI

    UsbSerialProber prober = new UsbSerialProber(customTable);
    UsbSerialDriver driver = prober.probeDevice(device);

    if (driver == null) {
      driver = UsbSerialProber.getDefaultProber().probeDevice(device);
      if (driver == null) {
        promise.reject("DRIVER_NOT_FOUND", "No driver for device");
        return;
      }
    }

    if (driver.getPorts().size() < 1) {
      promise.reject("NOT_ENOUGH_PORTS", "Not enough ports at device");
      return;
    }

    UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
    if (connection == null) {
      if (!usbManager.hasPermission(driver.getDevice())) {
        promise.reject("PERMISSION_DENIED", "Connection failed: permission denied");
      } else {
        promise.reject("OPEN_FAILED", "Connection failed: open failed");
      }
      return;
    }

    UsbSerialPort port = driver.getPorts().get(0);
    try {
      port.open(connection);
      port.setParameters(baudRate, dataBits, stopBits, parity);
    } catch (IOException e) {
      try {
        port.close();
      } catch (IOException ignored) {
      }
      promise.reject("OPEN_FAILED", "Connection failed", e);
      return;
    }

    wrapper = new UsbSerialPortWrapper(deviceId, port, this);
    usbSerialPorts.put(deviceId, wrapper);
    startReading(wrapper); // Start reading data after opening the port

    WritableMap result = Arguments.createMap();
    result.putInt("deviceId", deviceId);
    result.putString("productName", driver.getDevice().getProductName());
    promise.resolve(result);
  }

  private void startReading(UsbSerialPortWrapper wrapper) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        byte[] buffer = new byte[1024];
        while (true) {
          try {
            int len = wrapper.getPort().read(buffer, 1000);
            if (len > 0) {
              String data = new String(buffer, 0, len, StandardCharsets.UTF_8);
              sendEventToJs("onReceivedData", data);
            }
          } catch (IOException e) {
            sendEventToJs("onError", e.getMessage());
            break;
          }
        }
      }
    }).start();
  }

  private void sendEventToJs(String eventName, String eventData) {
    WritableMap params = Arguments.createMap();
    params.putString("data", eventData);
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
  }

  private UsbDevice findDevice(int deviceId) {
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    for (UsbDevice device : usbManager.getDeviceList().values()) {
      if (device.getDeviceId() == deviceId) {
        return device;
      }
    }
    return null;
  }

  @ReactMethod
  public void send(int deviceId, String data, Promise promise) {
    UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
    if (wrapper == null) {
      promise.reject(CODE_DEVICE_NOT_OPEN, "device not open");
      return;
    }

    byte[] buffer = Base64.decode(data, Base64.DEFAULT);

    try {
      wrapper.getPort().write(buffer, 1000);
      promise.resolve(null);
    } catch (IOException e) {
      promise.reject(CODE_SEND_FAILED, "send failed", e);
    }
  }

  @ReactMethod
  public void close(int deviceId, Promise promise) {
    UsbSerialPortWrapper wrapper = usbSerialPorts.get(deviceId);
    if (wrapper == null) {
      promise.resolve(null);
      return;
    }

    try {
      wrapper.getPort().close();
      usbSerialPorts.remove(deviceId);
      promise.resolve(null);
    } catch (IOException e) {
      promise.reject(CODE_DEVICE_NOT_OPEN_OR_CLOSED, "close failed", e);
    }
  }

  private static class UsbSerialPortWrapper {
    private final int deviceId;
    private final UsbSerialPort port;
    private final UsbSerialportForAndroidModule module;

    UsbSerialPortWrapper(int deviceId, UsbSerialPort port, UsbSerialportForAndroidModule module) {
      this.deviceId = deviceId;
      this.port = port;
      this.module = module;
    }

    int getDeviceId() {
      return deviceId;
    }

    UsbSerialPort getPort() {
      return port;
    }
  }
}
