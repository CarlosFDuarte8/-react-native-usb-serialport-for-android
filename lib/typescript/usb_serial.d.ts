import type { EventEmitter } from 'react-native';
export interface EventData {
    deviceId: number;
    /**
     * hex format
     */
    data: string;
}
export declare type Listener = (data: EventData) => void;
export default class UsbSerial {
    deviceId: number;
    private eventEmitter;
    private listeners;
    private subscriptions;
    constructor(deviceId: number, eventEmitter: EventEmitter);
    /**
     * Send data with hex string.
     *
     * May return error with these codes:
     * * DEVICE_NOT_OPEN
     * * SEND_FAILED
     *
     * See {@link Codes}
     * @param hexStr
     * @returns
     */
    send(hexStr: string): Promise<null>;
    whiteToUsbPort(deviceId: number, base64: string): Promise<any>
    /**
     * Listen to data received event.
     *
     * @param listener
     * @returns EventSubscription
     */
    onReceived(listener: Listener): import("react-native").EmitterSubscription;
    /**
     *
     * May return error with these codes:
     * * DEVICE_NOT_OPEN_OR_CLOSED
     *
     * See {@link Codes}
     * @returns Promise<null>
     */
    close(): Promise<any>;
    startListening(vendorId: number, productId: number): any
    sendCommand(command: string): Promise<any>
}
