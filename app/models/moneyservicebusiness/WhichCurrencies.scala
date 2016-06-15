package models.moneyservicebusiness

import javassist.runtime.Inner

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.mapping.GenericRules._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, JsValue, Reads, Format}
import utils.OptionValidators._
import utils.TraversableValidators
import utils.MappingUtils.Implicits._


case class WhichCurrencies(currencies : Seq[String]
                           , bankMoneySource : Option[BankMoneySource]
                           , wholesalerMoneySource : Option[WholesalerMoneySource]
                           , customerMoneySource : Boolean)

private sealed trait WhichCurrencies0 {

  private val nameType = maxLength(140)
  private val currencyType = TraversableValidators.minLength[Seq[String]](1)

  private implicit def rule[A]
    (implicit
      a : Path => RuleLike[A, Seq[String]],
      b: Path => RuleLike[A, Option[String]],
      c: Path => RuleLike[A, Boolean]
    ) : Rule[A, WhichCurrencies] = From[A] {__ =>

        val currencies = (__ \ "currencies").read(currencyType).withMessage("error.invalid.msb.wc.currencies")

        val bankMoneySource =
          (
            (__ \ "bankMoneySource").read[Option[String]] ~
            (__ \ "bankNames").read[Option[String], Option[String]](ifPresent(nameType)).withMessage("error.invalid.msb.wc.bankNames")
          ).apply {(a,b) => (a,b) match {
              case (Some("Yes"), Some(names)) => Some(BankMoneySource(names))
              case (Some("Yes"), None) => Some(BankMoneySource(""))
              case _ => None
            }}

        val wholesalerMoneySource =
          (
            (__ \ "wholesalerMoneySource").read[Option[String]] ~
            (__ \ "wholesalerNames").read[Option[String], Option[String]](ifPresent(nameType)).withMessage("error.invalid.msb.wc.wholesalerNames")
          ).apply {(a,b) => (a,b) match {
            case (Some("Yes"), Some(names)) => Some(WholesalerMoneySource(names))
            case (Some("Yes"), None) => Some(WholesalerMoneySource(""))
            case _ => None
          }}

          val customerMoneySource = (__ \ "customerMoneySource").read[Option[String]] fmap {
            case Some("Yes") => true
            case _ => false
          }

        (currencies ~
          bankMoneySource ~
          wholesalerMoneySource ~
          customerMoneySource)(WhichCurrencies.apply(_,_,_,_))
    }

    private implicit def write[A]
    (implicit
    m: Monoid[A],
    a: Path => WriteLike[Seq[String], A],
    b: Path => WriteLike[String, A],
    c: Path => WriteLike[Option[String], A]
    ) : Write[WhichCurrencies, A] = To[A] { __ =>
      (
        (__ \ "currencies").write[Seq[String]] ~
        (__ \ "bankMoneySource").write[Option[String]] ~
        (__ \ "bankNames").write[Option[String]] ~
        (__ \ "wholesalerMoneySource").write[Option[String]] ~
        (__ \ "wholesalerNames").write[Option[String]] ~
        (__ \ "customerMoneySource").write[Option[String]]
      ).apply(wc => (wc.currencies,
                      wc.bankMoneySource.map(_ => "Yes"),
                      wc.bankMoneySource.map(bms => bms.bankNames),
                      wc.wholesalerMoneySource.map(_ => "Yes"),
                      wc.wholesalerMoneySource.map(bms => bms.wholesalerNames),
                      if (wc.customerMoneySource) Some("Yes") else None
                      ))
    }

  val formR: Rule[UrlFormEncoded, WhichCurrencies] = {
    import play.api.data.mapping.forms.Rules._
    implicitly
  }

  val formW: Write[WhichCurrencies, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    implicitly
  }

  val jsonR: Reads[WhichCurrencies] = {
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    import utils.JsonMapping._
    implicitly[Reads[WhichCurrencies]]
  }


  val jsonW: Writes[WhichCurrencies] = {
    import play.api.data.mapping.json.Writes._
    import utils.JsonMapping._
    implicitly[Writes[WhichCurrencies]]
  }
}

object WhichCurrencies {
  private object Cache extends WhichCurrencies0

  implicit val formW: Write[WhichCurrencies, UrlFormEncoded] = Cache.formW
  implicit val formR: Rule[UrlFormEncoded, WhichCurrencies] = Cache.formR
  implicit val jsonR: Reads[WhichCurrencies] = Cache.jsonR
  implicit val jsonW: Writes[WhichCurrencies] = Cache.jsonW
}
