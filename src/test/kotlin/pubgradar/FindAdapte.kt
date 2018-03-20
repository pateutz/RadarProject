package pubgradar

import javazoom.jl.decoder.LayerIIIDecoder.io
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.PppSelector
import org.pcap4j.util.NifSelector

fun main(args: Array<String>) {
  val result=NifSelector().selectNetworkInterface()
  for (findAllDev in Pcaps.findAllDevs()) {
    println(findAllDev)
  }
}