package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms.Writes._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, To, Write}
import models.FormTypes.regexWithMsg
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbMoneyTransfers(transfers: Int)

object MsbMoneyTransfers {

  implicit val format = Json.format[MsbMoneyTransfers]

  implicit val formReader: Rule[UrlFormEncoded, MsbMoneyTransfers] = From[UrlFormEncoded] { __ =>
    (__ \ "transfers").read[Int].withMessage("renewal.msb.transfers.value.invalid") map MsbMoneyTransfers.apply
  }

  implicit val formWriter: Write[MsbMoneyTransfers, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "transfers").write[Int] contramap(_.transfers)
  }

}
