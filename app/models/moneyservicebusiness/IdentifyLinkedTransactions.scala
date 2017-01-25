package models.moneyservicebusiness

import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class IdentifyLinkedTransactions (linkedTxn: Boolean)

object IdentifyLinkedTransactions {

  import utils.MappingUtils.Implicits._

  implicit val format =  Json.format[IdentifyLinkedTransactions]

  implicit val formRule: Rule[UrlFormEncoded, IdentifyLinkedTransactions] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "linkedTxn").read[Boolean].withMessage("error.required.msb.linked.txn") fmap IdentifyLinkedTransactions.apply
  }

  implicit val formWrites: Write[IdentifyLinkedTransactions, UrlFormEncoded] = Write {x =>
    "linkedTxn" -> x.linkedTxn.toString
  }
}
