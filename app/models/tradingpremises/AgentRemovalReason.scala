package models.tradingpremises

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class AgentRemovalReason(removalReason: String, removalReasonOther: Option[String] = None)

object AgentRemovalReason {

  import utils.MappingUtils.Implicits._

  implicit val formats = Json.format[AgentRemovalReason]

  val otherDetailsLength = 255

  val otherDetailsRule = notEmptyStrip andThen
    notEmpty.withMessage("tradingpremises.remove_reasons.agent.other.missing") andThen maxLength(otherDetailsLength).
    withMessage("error.invalid.maxlength.255")

  implicit val formReader: Rule[UrlFormEncoded, AgentRemovalReason] = From[UrlFormEncoded] { __ =>
    (__ \ "removalReason").read[String] flatMap { reason =>
      if (reason == "Other") {
        (__ \ "removalReasonOther").read(otherDetailsRule) map { o =>
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