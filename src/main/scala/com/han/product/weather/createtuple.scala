package com.han.product.weather


import com.here.platform.location.core.geospatial.GeoCoordinate
import com.here.platform.location.integration.herecommons.geospatial.{HereTileLevel, HereTileResolver}
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import com.here.sdii.v3.{SdiiCommon,SdiiMessage}

import scala.collection.JavaConverters._

object CreateTileIDtoMessageTuple {

  private val log: Logger = LoggerFactory.getLogger(CreateTileIDtoMessageTuple.getClass)
  private final val Level = 14
  private final val Here_Tile_Resolver = new HereTileResolver(new HereTileLevel(Level))

  def createtileidtomessagetuple(message: SdiiMessage.Message): (Long, SdiiMessage.Message) = {
    var earliestPositionEstimate: SdiiCommon.PositionEstimate = message.getPath.getPositionEstimateList.asScala
      .minBy(_.getTimeStampUTCMs)
    val level = 14
    val lat = earliestPositionEstimate.getLatitudeDeg
    val lon = earliestPositionEstimate.getLongitudeDeg
    val Tileid = Here_Tile_Resolver.fromCoordinate(GeoCoordinate(lat, lon)).value
    log.info(s"===> Position Estimate's TileId ${Tileid}")
    (Tileid, message)
  }
}
