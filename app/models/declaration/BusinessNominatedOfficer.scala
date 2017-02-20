package models.declaration

import jto.validation.{From, Rule, To, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class BusinessNominatedOfficer(value: String)

object BusinessNominatedOfficer {

  import utils.MappingUtils.Implicits._

  val key = "business-nominated-officer"

  implicit val formRule: Rule[UrlFormEncoded, BusinessNominatedOfficer] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "value").read[String].withMessage("error.required.declaration.nominated.officer") map BusinessNominatedOfficer.apply
    }
  implicit val formWrites: Write[BusinessNominatedOfficer, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (__ \ "value").write[String] contramap{x =>x.value}
  }

  implicit val format = Json.format[BusinessNominatedOfficer]

}
