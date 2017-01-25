package models.hvd

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class LinkedCashPayments(linkedCashPayments: Boolean)

object LinkedCashPayments {

  implicit val format = Json.format[LinkedCashPayments]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, LinkedCashPayments] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "linkedCashPayments").read[Boolean].withMessage("error.required.hvd.linked.cash.payment") fmap LinkedCashPayments.apply
  }

  implicit val formWrites: Write[LinkedCashPayments, UrlFormEncoded] = Write {
    case LinkedCashPayments(registered) => Map("linkedCashPayments" -> Seq(registered.toString))
  }

}
