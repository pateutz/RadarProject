package pubgradar.struct.CMD

import pubgradar.util.debugln
import pubgradar.deserializer.channel.ActorChannel.Companion.droppedItemToItem
import pubgradar.struct.*

object DroppedItemCMD {
  
  fun process(actor:Actor,bunch:Bunch,repObj:NetGuidCacheObject?,waitingHandle:Int,data:HashMap<String,Any?>):Boolean {
   try{ with(bunch) {
      when (waitingHandle) {
        16 -> {
          val (itemguid,item)=readObject()
          droppedItemToItem[actor.netGUID]=itemguid
        }
        else -> ActorCMD.process(actor,bunch,repObj,waitingHandle,data)
      }
      return true
    }
   }catch (e: Exception){ debugln{("DroppedItemCMD is throwing somewhere: $e ${e.stackTrace} ${e.message} ${e.cause}")}
   }
      return false
  }
}