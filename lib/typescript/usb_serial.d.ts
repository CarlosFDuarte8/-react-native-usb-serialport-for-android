import type { EventEmitter } from 'react-native';

export interface EventData {
    deviceId: number;
    /**
     * hex format
     */
    data: string;
}

export interface ErrorEventData {
    deviceId: number;
    error: string;
}

export declare type Listener = (data: EventData) => void;
export declare type ErrorListener = (error: ErrorEventData) => void;

export default class UsbSerial {
    deviceId: number;
    private eventEmitter;
    private listeners;
    private errorListeners;
    private subscriptions;
    private result: any;

    constructor(deviceId: number, eventEmitter: EventEmitter, result: any);

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

    writeToUsbPort(base64: string): Promise<any>;

    /**
     * Send data with a command string.
     *
     * May return error with these codes:
     * * DEVICE_NOT_OPEN
     * * SEND_FAILED
     *
     * See {@link Codes}
     * @param command
     * @returns
     */
    sendCommand(command: string, addNewLine?: boolean): Promise<any>;

    /**
     * Listen to data received event.
     *
     * @param listener
     * @returns EventSubscription
     */
    onReceived(listener: Listener): import("react-native").EmitterSubscription;

    /**
     * Listen to error events.
     *
     * @param listener
     * @returns EventSubscription
     */
    onError(listener: ErrorListener): import("react-native").EmitterSubscription;

    /**
     * Monitor data received and error events.
     *
     * @param onDataReceived
     * @param onError
     * @returns { close: () => void }
     */
    monitor(onDataReceived: Listener, onError: ErrorListener): { close: () => void };

    /**
     * Close the connection and clean up resources.
     *
     * May return error with these codes:
     * * DEVICE_NOT_OPEN_OR_CLOSED
     *
     * See {@link Codes}
     * @returns Promise<null>
     */
    close(): Promise<any>;

    /**
     * Start listening for USB events.
     *
     * @param vendorId
     * @param productId
     * @returns Promise<any>
     */
    startListening(vendorId: number, productId: number): Promise<any>;
}
