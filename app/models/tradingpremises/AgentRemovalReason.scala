package models.tradingpremises

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, ValidationError, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class AgentRemovalReason(removalReason: String, removalReasonOther: Option[String] = None)

object AgentRemovalReason {

  import utils.MappingUtils.Implicits._

  implicit val formats = Json.format[AgentRemovalReason]

  val otherDetailsLength = 255
  val otherDetailsType = notEmptyStrip andThen notEmpty andThen maxLength(otherDetailsLength).
    withMessage("tradingpremises.remove_reasons.agent.other.too_long")

  implicit val formReader: Rule[UrlFormEncoded, AgentRemovalReason] = From[UrlFormEncoded] { __ =>
    (__ \ "removalReason").read[String] flatMap { reason =>
      if (reason == "Other") {
        (__ \ "removalReasonOther").read(otherDetailsType) map { o =>
          AgentRemovalReason(reason, Some(o))
        }
      } else
        AgentRemovalReason(reason)
    }
  }

  implicit val formWriter: Write[AgentRemovalReason, UrlFormEncoded] = Write { a =>
    Map(
      "removalReason" -> Seq(a.removalReason),
      "removalReasonOther" -> Seq(a.removalReasonOther.getOrElse(""))
    )
  }

}