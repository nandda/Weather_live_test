package com.han.product.weather.Converters

import akka.actor.ActorSystem
import akka.stream.javadsl.Partition
//import akka.stream.scaladsl.Partition
import com.here.hrn.HRN
import com.here.platform.data.client.flink.scaladsl.{FlinkDataClient, FlinkReadEngine}
import com.here.platform.data.client.scaladsl.{DataClient, Partition}
import com.han.product.live.weather.v1.live_weather_han_schema.{ConfigItems, LiveWeatherHanMessage, WeatherLiveItem}
import com.han.product.weather.Main.log
import com.han.product.weather.{ReadMessage, ScalaConfig}
import com.here.platform.data.client.settings.{ConsumerSettings, LatestOffset}
import com.here.platform.pipeline.PipelineContext
import com.here.sdii.v3.SdiiMessage
import org.slf4j.Logger
import org.apache.flink.api.common.functions.{MapFunction, RichMapFunction}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.slf4j.LoggerFactory
import org.apache.flink.api.scala._
import org.apache.flink.util.Collector

class Read_catalog(inputcatalogHRN: HRN) extends RichMapFunction[Partition,(Partition,Array[Byte])] {
  //class Read_catalog(inputcatalogHRN: HRN) extends RichMapFunction[Partition,LiveWeatherHanMessage] {
private lazy val log: Logger = LoggerFactory.getLogger(classOf[Read_catalog])

  @transient var flinkDataClient : FlinkDataClient =_
  @transient var flinkReadEngine :FlinkReadEngine =_

  override def open(parameters: Configuration): Unit = {
 flinkDataClient = new FlinkDataClient
    flinkReadEngine = flinkDataClient.readEngine(inputcatalogHRN)
  }

  override def map(partition: Partition): (Partition,Array[Byte]) = {
 log.info(s"======>The partition value ${partition}")
    (partition,flinkReadEngine.getDataAsBytes(partition))
   // LiveWeatherHanMessage.parseFrom(flinkReadEngine.getDataAsBytes(partition))
  }

  override def close() :Unit = {
flinkDataClient.terminate()
  }
}

