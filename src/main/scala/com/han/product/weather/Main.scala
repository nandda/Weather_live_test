package com.han.product.weather


import com.here.hrn.HRN
import org.slf4j.{Logger, LoggerFactory}
import com.here.platform.pipeline.PipelineContext
import com.here.platform.data.client.engine.model.BlobIdGenerator
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import com.here.platform.data.client.flink.scaladsl.{FlinkDataClient, FlinkReadEngine, FlinkWriteEngine}
import com.here.platform.data.client.scaladsl.{NewPartition, Partition}
import com.here.platform.data.client.settings.{ConsumerSettings, LatestOffset}
import com.here.schema.sdii.v1.SdiiArchive
import com.here.sdii.v3.SdiiMessage
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import com.han.product.weather.CreateTileIDtoMessageTuple.createtileidtomessagetuple
import com.han.product.weather.CreateArchiveMessage.createarchivemessage
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector
import com.twitter.chill.protobuf.ProtobufSerializer
import org.apache.flink.api.scala._
object Main extends App {

  private val log: Logger = LoggerFactory.getLogger(Main.getClass)

  override def main(args: Array[String]) = {
    try {
      run()
    } catch {
      case e: java.lang.Exception => log.error("Failed", e)
    }
  }


  def run(): Unit ={
    val scalaConfig: ScalaConfig = new ScalaConfig()
    val inputcatalogHRN = scalaConfig.inputcatalogHRN
    log.info("===> Input catalog HRN: {}", inputcatalogHRN)
    val outputcatalogHRN = scalaConfig.outputcatalogHRN
    log.info("===> Output catalog HRN: {}", outputcatalogHRN)

    val inputLayerName = "sample-streaming-layer"
    val outputLayerName = "sample-volatile-layer"

    val NUM_RESTART_ATTEMPTS = 10
    val JOB_RESTART_DELAY_IN_MIN = 5L

val env: StreamExecutionEnvironment =StreamExecutionEnvironment.getExecutionEnvironment
env.setParallelism(3)
env.enableCheckpointing(6000)

    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(NUM_RESTART_ATTEMPTS,
      org.apache.flink.api.common.time.Time.minutes(JOB_RESTART_DELAY_IN_MIN)))


    val flinkdataclient = new FlinkDataClient()
   val queryApi =flinkdataclient.queryApi(inputcatalogHRN)

    registerTypesWithKryoSerializer(env)

   env
     .addSource(queryApi.subscribe(inputLayerName, ConsumerSettings("Consumer-1", LatestOffset)))
      .uid("Sdii-source")
      .map(new ReadMessage(inputcatalogHRN))
      .map(message => createtileidtomessagetuple(message))
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .apply((key, _, input, collector: Collector[(Long, List[SdiiMessage.Message])]) => {
     log.debug(s"===> tileId=${key.getField(0)}")
     collector.collect((key.getField(0), input.map(_._2).toList))
   })
      .uid("tile-grouped-message")
      .map(tileMessages =>createarchivemessage(tileMessages))
      .addSink(new OutputToVolatile(outputcatalogHRN, outputLayerName))
    env.execute("SDII Streaming App Example")

    flinkdataclient.terminate()

  }

  def registerTypesWithKryoSerializer(env: StreamExecutionEnvironment): Unit = {
    env.getConfig.registerTypeWithKryoSerializer(classOf[SdiiMessage.Message], classOf[ProtobufSerializer])
    env.getConfig.registerTypeWithKryoSerializer(classOf[SdiiArchive.Tile], classOf[ProtobufSerializer])
  }
}



class ReadMessage(inputCatalogHRN: HRN) extends RichMapFunction[Partition,SdiiMessage.Message] {

  private lazy val log: Logger = LoggerFactory.getLogger(classOf[ReadMessage])

@transient var flinkDataClient :FlinkDataClient = _
  @transient var readEngine :FlinkReadEngine = _

  override def open(parameters: Configuration): Unit = {
log.info("open")
flinkDataClient = new FlinkDataClient
readEngine =flinkDataClient.readEngine(inputCatalogHRN)
  }

  override def close(): Unit = {
flinkDataClient.terminate()
  }

  override def map(partition: Partition): SdiiMessage.Message = {
log.info (s"Reading data from partition ${partition.partition}")
    SdiiMessage.Message.parseFrom(readEngine.getDataAsBytes(partition))
  }

}


class OutputToVolatile(outputcatalogHRN: HRN, outputLayername:String) extends RichSinkFunction[SdiiArchive.Tile] {

  private lazy  val log :Logger = LoggerFactory.getLogger(classOf[OutputToVolatile])

  @transient var flinkDataClient: FlinkDataClient = _
  @transient var writeEngine: FlinkWriteEngine = _


  private def dataHandleIdGenerator = new BlobIdGenerator {
    override def generateBlobId(partition: NewPartition): String = partition.partition
  }

  override def open(parameters: Configuration): Unit = {
    log.info("===> open OutputToVolatile")
    flinkDataClient = new FlinkDataClient
    writeEngine = flinkDataClient.writeEngine(outputcatalogHRN, dataHandleIdGenerator)
  }

  override def invoke(archiveMessage: SdiiArchive.Tile): Unit = {
    log.info(s"===> sink tile=${archiveMessage.getTileId} msg-count=${archiveMessage.getMessagesCount}")
    val newPartition = NewPartition(
      partition = archiveMessage.getTileId,
      layer = outputLayername,
      data = NewPartition.ByteArrayData(archiveMessage.toByteArray)
    )
    writeEngine.put(newPartition)
  }
}


class ScalaConfig extends Serializable {
  private val pipelineContext: PipelineContext = new PipelineContext()

  val inputcatalogHRN:HRN =pipelineContext.config.inputCatalogs.head._2
  val outputcatalogHRN: HRN = pipelineContext.config.outputCatalog
}