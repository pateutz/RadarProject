package pubgradar.struct.CMD

import pubgradar.util.debugln
import pubgradar.deserializer.channel.ActorChannel.Companion.airDropLocation
import pubgradar.struct.*

object AirDropCMD {
  fun process(actor:Actor,bunch:Bunch,repObj:NetGuidCacheObject?,waitingHandle:Int,data:HashMap<String,Any?>):Boolean {
   try{ with(bunch) {
      when (waitingHandle) {
        6 -> {
          repMovement(actor)
          airDropLocation[actor.netGUID]=actor.location
        }
        16 -> updateItemBag(actor)
        else -> return ActorCMD.process(actor,bunch,repObj,waitingHandle,data)
      }
      return true
    }
   }catch (e: Exception){ debugln{("AirDropCMD is throwing somewhere: $e ${e.stackTrace} ${e.message} ${e.cause}")} }
  return false
  }
}