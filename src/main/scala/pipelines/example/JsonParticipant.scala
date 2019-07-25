package pipelines.example

import spray.json._

case object JsonParticipant extends DefaultJsonProtocol {
  implicit val crFormat = jsonFormat(Participant.apply, "name", "guess")
}
