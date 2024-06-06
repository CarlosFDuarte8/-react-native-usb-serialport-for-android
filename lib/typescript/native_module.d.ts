export interface Device {
    readonly deviceId: number;
    readonly vendorId: number;
    readonly productId: number;
    readonly name: string;
    readonly deviceName: string;
    readonly productName: string;

}
interface UsbSerialportForAndroidAPI {
    list(): Promise<Device[]>;
    tryRequestPermission(deviceId: number): Promise<number>;
    hasPermission(deviceId: number): Promise<boolean>;
    open(deviceId: number, baudRate: number, dataBits: number, stopBits: number, parity: number): Promise<object>;
    send(deviceId: number, hexStr: string): Promise<null>;
    whiteToUsbPort(deviceId: number, base64: string): Promise<any>;
    sendCommand(command: string): Promise<any>
    close(deviceId: number): Promise<null>;
    startListening(vendorId: number, productId: number): any
}
declare const UsbSerialportForAndroid: UsbSerialportForAndroidAPI;
export default UsbSerialportForAndroid;
