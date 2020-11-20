//package com.han.product.weather
//
//import com.here.hrn.HRN
//import com.here.platform.data.client.flink.scaladsl.{FlinkDataClient, FlinkPublishApi, FlinkWriteEngine}
//import com.here.platform.data.client.model.PendingPartition
//import com.here.platform.data.client.scaladsl.CommitPartition
//import org.apache.flink.api.common.functions.RichMapFunction
//import org.apache.flink.streaming.api.functions.windowing.RichAllWindowFunction
//import org.apache.flink.streaming.api.windowing.windows.TimeWindow
//import org.apache.flink.util.Collector
//
/////** Map function that upload data and return metadata */
////class UploadDataFunction(hrn: HRN) extends RichMapFunction[PendingPartition, CommitPartition]
////    with Serializable {
////  // initialize DataClient
////  @transient
////  private lazy val dataClient: FlinkDataClient =
////  new FlinkDataClient()
////
////  @transient
////  private lazy val writeEngine: FlinkWriteEngine =
////    dataClient.writeEngine(hrn)
////
////  // terminate DataClient
////  override def close(): Unit =
////    dataClient.terminate()
////
////  // read data and publish a tuple with partition and data
////  override def map(pendingPartition: PendingPartition): CommitPartition =
////    writeEngine.put(pendingPartition)
////}
//
///** Window function that publish an index for all CommitPartition in the window*/
//class PublishIndexWindowFunction(hrn: HRN, indexLayer: String)
//  extends RichAllWindowFunction[CommitPartition, String, TimeWindow]
//    with Serializable {
//  // initialize DataClient
//  @transient
//  private lazy val flinkDataClient: FlinkDataClient =
//  new FlinkDataClient()
//
//  @transient
//  private lazy val publishApi: FlinkPublishApi =
//    flinkDataClient.publishApi(hrn)
//
//  // terminate DataClient
//  override def close(): Unit =
//    flinkDataClient.terminate()
//
//  override def apply(window: TimeWindow,
//                     partitions: Iterable[CommitPartition],
//                     out: Collector[String]): Unit = {
//    publishApi.publishIndex(indexLayer, partitions.iterator)
//    out.collect(s"indexing a new partition of $indexLayer layer is a success")
//  }
//}
//
