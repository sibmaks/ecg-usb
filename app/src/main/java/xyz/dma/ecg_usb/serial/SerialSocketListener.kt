package xyz.dma.ecg_usb.serial

/**
 * Created by maksim.drobyshev on 07-Mar-21.
 */
interface SerialSocketListener {
    fun onConnect(serialSocket: SerialSocket)

    fun onDisconnect(serialSocket: SerialSocket)
}