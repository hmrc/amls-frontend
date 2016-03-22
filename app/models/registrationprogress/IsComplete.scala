package models.registrationprogress

import play.api.libs.json.{Json, Reads}

case class IsComplete(isComplete: Boolean)

object IsComplete {

  implicit val writes = Json.writes[IsComplete]

  implicit val reads: Reads[IsComplete] = {
    import play.api.libs.json._
    ((__ \ "isComplete").read[Option[Boolean]] map (_.getOrElse(false)) map IsComplete.apply) orElse Reads(_ => JsSuccess(IsComplete(false)))
  }
}
