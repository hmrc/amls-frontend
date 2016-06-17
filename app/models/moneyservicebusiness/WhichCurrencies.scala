package models.moneyservicebusiness

import javassist.runtime.Inner

import play.api.data.mapping
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.mapping.GenericRules._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, JsValue, Reads, Format}
import utils.OptionValidators._
import utils.TraversableValidators
import utils.MappingUtils.Implicits._
import models._


case class WhichCurrencies(currencies : Seq[String]
                           , bankMoneySource : Option[BankMoneySource]
                           , wholesalerMoneySource : Option[WholesalerMoneySource]
                           , customerMoneySource : Boolean)

private sealed trait WhichCurrencies0 {

  val emptyToNone: String => Option[String] = {x =>
    x.trim() match {
      case "" => None
      case s => Some(s)
    }
  }

  private val nameType = minLength(4) compose maxLength(140)

  private val currencyType = TraversableValidators.seqToOptionSeq(emptyToNone) compose
                              TraversableValidators.flattenR[String] compose
                              TraversableValidators.minLengthR[Seq[String]](1)

  private val validateMoneySources : ValidationRule[WhichCurrencies] = Rule[WhichCurrencies, WhichCurrencies] {
    case x@WhichCurrencies(_, Some(_), _, _) => Success(x)
    case x@WhichCurrencies(_, _, Some(_), _) => Success(x)
    case x@WhichCurrencies(_, _, _, true) => Success(x)
    case _ => Failure(Seq((Path \ "") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
  }

  private implicit def rule[A]
    (implicit
      a : Path => RuleLike[A, Seq[String]],
      b: Path => RuleLike[A, Option[String]],
      d: Path => RuleLike[A, String],
      c: Path => RuleLike[A, Boolean]
    ) : Rule[A, WhichCurrencies] = From[A] {__ =>

        val currencies = (__ \ "currencies").read(currencyType).withMessage("error.invalid.msb.wc.currencies")

        val bankMoneySource : Rule[A, Option[BankMoneySource]]=
            (__ \ "bankMoneySource").read[Option[String]] flatMap {
              case Some("Yes") => (__ \ "bankNames")
                                    .read(nameType)
                                    .withMessage("error.invalid.msb.wc.bankNames")
                                    .fmap(names => Some(BankMoneySource(names)))
              case _ => Rule[A, Option[BankMoneySource]](_ => Success(None))
            }


        val wholesalerMoneySource : Rule[A, Option[WholesalerMoneySource]]=
          (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
            case Some("Yes") => (__ \ "wholesalerNames")
                                  .read(nameType)
                                  .withMessage("error.invalid.msb.wc.wholesalerNames")
                                  .fmap(names => Some(WholesalerMoneySource(names)))
            case _ => Rule[A, Option[WholesalerMoneySource]](_ => Success(None))
          }

          val customerMoneySource = (__ \ "customerMoneySource").read[Option[String]] fmap {
            case Some("Yes") => true
            case _ => false
          }

        (currencies ~
          bankMoneySource ~
          wholesalerMoneySource ~
          customerMoneySource)(WhichCurrencies.apply(_,_,_,_)) compose validateMoneySources
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
