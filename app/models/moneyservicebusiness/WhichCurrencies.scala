package models.moneyservicebusiness

import javassist.runtime.Inner

import play.api.data.mapping
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.mapping.GenericRules._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, JsValue, Reads, Format}
import utils.OptionValidators._
import utils.{GenericValidators, TraversableValidators}
import utils.MappingUtils.Implicits._
import models._


case class WhichCurrencies(currencies: Seq[String],
                           usesForeignCurrencies: Boolean,
                           bankMoneySource: Option[BankMoneySource],
                           wholesalerMoneySource: Option[WholesalerMoneySource],
                           customerMoneySource: Boolean)

private sealed trait WhichCurrencies0 {

  val emptyToNone: String => Option[String] = { x =>
    x.trim() match {
      case "" => None
      case s => Some(s)
    }
  }

  private def nameType(fieldName: String) = {
    minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") compose
      maxLength(140).withMessage(s"error.invalid.msb.wc.$fieldName.too-long")
  }

  private val currencyListType = TraversableValidators.seqToOptionSeq(emptyToNone) compose
    TraversableValidators.flattenR[String] compose
    TraversableValidators.minLengthR[Seq[String]](1) compose
    GenericRules.traversableR(GenericValidators.inList(currencies))

  private val validateMoneySources: ValidationRule[(Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)] =
    Rule[(Option[BankMoneySource], Option[WholesalerMoneySource], Boolean),
      (Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)] {
      case x@(Some(_), _, _) => Success(x)
      case x@(_, Some(_), _) => Success(x)
      case x@(_, _, true) => Success(x)
      case _ => Failure(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
    }

  private implicit def rule[A]
  (implicit
   a: Path => RuleLike[A, Seq[String]],
   b: Path => RuleLike[A, Option[String]],
   d: Path => RuleLike[A, String],
   c: Path => RuleLike[A, Boolean]
  ): Rule[A, WhichCurrencies] = From[A] { __ =>

    val currencies = (__ \ "currencies").read(currencyListType).withMessage("error.invalid.msb.wc.currencies")

    val foreignCurrencyToggle = (__ \ "foreignCurrencyToggle").read[String] withMessage "error.required.msb.wc.foreignCurrencyToggle" fmap {
      case "Yes" => true
      case _ => false
    }

    val bankMoneySource: Rule[A, Option[BankMoneySource]] =
      (__ \ "bankMoneySource").read[Option[String]] flatMap {
        case Some("Yes") => (__ \ "bankNames")
          .read(nameType("bankNames"))
          .fmap(names => Some(BankMoneySource(names)))
        case _ => Rule[A, Option[BankMoneySource]](_ => Success(None))
      }

    val wholesalerMoneySource: Rule[A, Option[WholesalerMoneySource]] =
      (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
        case Some("Yes") => (__ \ "wholesalerNames")
          .read(nameType("wholesalerNames"))
          .fmap(names => Some(WholesalerMoneySource(names)))
        case _ => Rule[A, Option[WholesalerMoneySource]](_ => Success(None))
      }

    val customerMoneySource = (__ \ "customerMoneySource").read[Option[String]] fmap {
      case Some("Yes") => true
      case _ => false
    }

    foreignCurrencyToggle flatMap {
      case true =>
        (currencies ~ ((bankMoneySource ~ wholesalerMoneySource ~ customerMoneySource).tupled compose validateMoneySources))
          .apply { (a: Traversable[String], b: (Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)) =>
            (a, b) match {
              case (c, (bms, wms, cms)) => WhichCurrencies(c.toSeq, usesForeignCurrencies = true, bms, wms, customerMoneySource = cms)
            }
          }
      case _ =>
        currencies compose Rule.fromMapping[Traversable[String], WhichCurrencies] { c =>
          Success(WhichCurrencies(c.toSeq, usesForeignCurrencies = false, None, None, customerMoneySource = false))
        }

    }


  }

  private implicit def write[A]
  (implicit
   m: Monoid[A],
   a: Path => WriteLike[Seq[String], A],
   b: Path => WriteLike[String, A],
   c: Path => WriteLike[Option[String], A]
  ): Write[WhichCurrencies, A] = To[A] { __ =>
    (
      (__ \ "currencies").write[Seq[String]] ~
        (__ \ "bankMoneySource").write[Option[String]] ~
        (__ \ "bankNames").write[Option[String]] ~
        (__ \ "wholesalerMoneySource").write[Option[String]] ~
        (__ \ "wholesalerNames").write[Option[String]] ~
        (__ \ "customerMoneySource").write[Option[String]] ~
        (__ \ "foreignCurrencyToggle").write[Option[String]]
      ).apply(wc => (wc.currencies,
      wc.bankMoneySource.map(_ => "Yes"),
      wc.bankMoneySource.map(bms => bms.bankNames),
      wc.wholesalerMoneySource.map(_ => "Yes"),
      wc.wholesalerMoneySource.map(bms => bms.wholesalerNames),
      if (wc.customerMoneySource) Some("Yes") else None,
      if (wc.usesForeignCurrencies) Some("Yes") else None
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
