package com.han.product.weather

import org.slf4j.{Logger, LoggerFactory}
import com.here.sdii.v3.{SdiiCommon, SdiiMessage}
import com.here.schema.sdii.v1.SdiiArchive

object CreateArchiveMessage {

  private  val log:Logger = LoggerFactory.getLogger(CreateArchiveMessage.getClass)

def createarchivemessage(tilemessages:(Long,List[SdiiMessage.Message])) :SdiiArchive.Tile = {
val archive = SdiiArchive.Tile.newBuilder.setTileId(tilemessages._1.toString)
tilemessages._2.foreach(message => {
val wrapper =SdiiArchive.MessageWrapper.newBuilder
  .setMessageId(message.getEnvelope.getTransientVehicleUUID)
  .setMessage(message)
  .build()
 archive.addMessages(wrapper)

})
log.info(s"===> Message Count:${archive.getMessagesCount}")
  archive.build()
}
}
