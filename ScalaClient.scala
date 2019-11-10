package example

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.ws._
import play.api.libs.ws.ahc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import scala.concurrent.Future

case class IOI(symbol: String, ioiqty: Double, ioiQualifiers: String)

object ScalaClient {
  import DefaultBodyReadables._

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val ioiWrites: Writes[IOI] =
    (JsPath \ "symbol").write[String].and((JsPath \ "ioiqty").write[Double])
      .and((JsPath \ "ioiqualifier").write[String])(unlift(IOI.unapply))

  implicit val ioiReads: Reads[IOI] =
    (JsPath \ "symbol").read[String].and((JsPath \ "ioiqty").read[Double])
      .and((JsPath \ "ioiqualifier").read[String])(IOI.apply _)


  def main(args: Array[String]): Unit = {
    // Create Akka system for thread and streaming management
    implicit val system = ActorSystem()
    system.registerOnTermination {
      System.exit(0)
    }
    implicit val materializer = Materializer.matFromSystem

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
    val wsClient = StandaloneAhcWSClient()

    call(wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def call(wsClient: StandaloneWSClient): Future[Unit] = {
    val json: JsValue = Json.parse("""
      {
        "name":"Nuthanger Farm",
        "location":{
                      "lat" : 51.244031,
                      "long" : -1.263224
                    }
      }
      """)
    val ioiJson = Json.toJson(List(IOI("6758", 1000,"ABC"), IOI("6502", 2000,"DEF")))
    //val ioiJson = Json.toJson(IOI("6758", 1000,"ABC"))
    println(Json.prettyPrint(ioiJson))
    wsClient.url("http://localhost:9000/new-ioi").post(ioiJson).map { response ⇒
      val statusText: String = response.statusText
      val body = response.body[JsValue]
      println(s"Got a response $statusText")
      //println(body)
      println(Json.prettyPrint(body))
    }
  }
}
