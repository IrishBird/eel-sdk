package io.eels.component.elasticsearch

import java.util.concurrent.atomic.AtomicBoolean

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchIterator
import io.eels.datastream.{Publisher, Subscriber, Subscription}
import io.eels.schema.{Field, StructType}
import io.eels.{Row, Source}

import scala.concurrent.duration._

class ElasticsearchSource(index: String)(implicit client: HttpClient) extends Source {

  import com.sksamuel.elastic4s.http.ElasticDsl._

  implicit val duration = 5.minutes

  override def schema: StructType = {
    val resp = client.execute {
      getMapping(index)
    }.await.head.mappings
    val fields = resp.head._2.keys.map { key => Field(key) }.toSeq
    StructType(fields)
  }

  override def parts(): Seq[Publisher[Seq[Row]]] = Seq(new ElasticsearchPublisher(index))
}

class ElasticsearchPublisher(index: String)(implicit client: HttpClient) extends Publisher[Seq[Row]] {

  import com.sksamuel.elastic4s.http.ElasticDsl._

  implicit val duration = 5.minutes

  override def subscribe(subscriber: Subscriber[Seq[Row]]): Unit = {
    try {
      val running = new AtomicBoolean(true)
      subscriber.subscribed(Subscription.fromRunning(running))
      SearchIterator.hits(client, search(index).matchAllQuery.keepAlive("1m").size(50))
        .takeWhile(_ => running.get)
        .grouped(50)
        .foreach { batch =>
          val chunk = batch.map { hit =>
            val schema = StructType(hit.fields.keys.map { key => Field(key) }.toSeq)
            Row(schema, hit.fields.values.toVector)
          }
          subscriber.next(chunk)
        }
      subscriber.completed()
    } catch {
      case t: Throwable => subscriber.error(t)
    }
  }
}