package wumo.pubg.struct.CMD

import pubgradar.deserializer.channel.ActorChannel.Companion.airDropLocation
import pubgradar.struct.*
import pubgradar.struct.CMD.ActorCMD
import pubgradar.struct.CMD.updateItemBag
import wumo.pubg.struct.*

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
  }catch (e: Exception){ println("AirDropReplicator is throwing somewhere: $e ${e.stackTrace} ${e.message}") }
  return false
  }
}