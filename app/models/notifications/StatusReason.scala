package models.notifications

import play.api.libs.json._

trait StatusReason

case object IgnoreThis extends StatusReason

object StatusReason {

  implicit val jsonWrites: Writes[StatusReason] = {
    import play.api.libs.json._
    Writes[StatusReason] {
      case a: RejectedReason =>
        RejectedReason.jsonWrites.writes(a)
      case a: RevokedReason =>
        RevokedReason.jsonWrites.writes(a)
      case a: DeregisteredReason =>
        DeregisteredReason.jsonWrites.writes(a)
    }
  }
}
