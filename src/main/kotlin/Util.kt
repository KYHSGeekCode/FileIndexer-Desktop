import java.net.InetAddress

fun getLocalDeviceName(): String = InetAddress.getLocalHost().hostName
//LocalDevice.getLocalDevice().friendlyName