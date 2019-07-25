package pipelines.example

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import JsonParticipant._
import pipelines.streamlets.avro._
import pipelines.streamlets._
import pipelines.akkastream._
import pipelines.akkastream.util.scaladsl.HttpServerLogic
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object VoteIngress extends AkkaServerStreamlet {
  val out = AvroOutlet[Participant]("out", p ⇒ p.name)

  final override val shape = StreamletShape.withOutlets(out)
  final override def createLogic = HttpServerLogic.default(this, out)

  val route =
    path("/") {
      getFromResource("vote.html")
    }

}
