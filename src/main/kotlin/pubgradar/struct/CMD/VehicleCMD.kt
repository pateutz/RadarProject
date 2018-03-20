package pubgradar.struct.CMD

import pubgradar.struct.*

object VehicleCMD {
  fun process(actor:Actor,bunch:Bunch,repObj:NetGuidCacheObject?,waitingHandle:Int,data:HashMap<String,Any?>):Boolean {
   try{ actor as Vehicle
    with(bunch) {
      when (waitingHandle) {
        16 -> {
          val (netguid)=propertyObject()
          actor.driverPlayerState=netguid
        }
        else -> return APawnCMD.process(actor,bunch,repObj,waitingHandle,data)
      }
      return true
    }
   }catch (e: Exception){ println("VehicleRep is throwing somewhere: $e ${e.stackTrace} ${e.message}") }
      return false
  }
}