package models.moneyservicebusiness

import javassist.runtime.Inner

import jto.validation
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.GenericRules._
import play.api.libs.functional.Monoid
import play.api.libs.json.{Writes, JsValue, Reads, Format}
import utils.OptionValidators._
import utils.{GenericValidators, TraversableValidators}
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

  private def nameType(fieldName : String) = {
    minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage(s"error.invalid.msb.wc.$fieldName.too-long")
  }

  private val currencyListType = TraversableValidators.seqToOptionSeq(emptyToNone) andThen
                              TraversableValidators.flattenR[String] andThen
                              TraversableValidators.minLengthR[Seq[String]](1) andThen
                              GenericRules.traversableR(GenericValidators.inList(currencies))

  private val validateMoneySources : ValidationRule[(Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)] =
    Rule[(Option[BankMoneySource], Option[WholesalerMoneySource], Boolean),
        (Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)] {
    case x@(Some(_), _, _) => Success(x)
    case x@( _, Some(_), _) => Success(x)
    case x@( _, _, true) => Success(x)
    case _ => Failure(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
  }

  private implicit def rule[A]
    (implicit
      a : Path => RuleLike[A, Seq[String]],
      b: Path => RuleLike[A, Option[String]],
      d: Path => RuleLike[A, String],
      c: Path => RuleLike[A, Boolean]
    ) : Rule[A, WhichCurrencies] = From[A] {__ =>

        val currencies = (__ \ "currencies").read(currencyListType).withMessage("error.invalid.msb.wc.currencies")

        val bankMoneySource : Rule[A, Option[BankMoneySource]]=
            (__ \ "bankMoneySource").read[Option[String]] flatMap {
              case Some("Yes") => (__ \ "bankNames")
                                    .read(nameType("bankNames"))
                                    .map(names => Some(BankMoneySource(names)))
              case _ => Rule[A, Option[BankMoneySource]](_ => Success(None))
            }

        val wholesalerMoneySource : Rule[A, Option[WholesalerMoneySource]]=
          (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
            case Some("Yes") => (__ \ "wholesalerNames")
                                  .read(nameType("wholesalerNames"))
                                  .map(names => Some(WholesalerMoneySource(names)))
            case _ => Rule[A, Option[WholesalerMoneySource]](_ => Success(None))
          }

          val customerMoneySource = (__ \ "customerMoneySource").read[Option[String]] map {
            case Some("Yes") => true
            case _ => false
          }

      (currencies ~ ((bankMoneySource ~ wholesalerMoneySource ~ customerMoneySource).tupled andThen validateMoneySources))
        .apply {(a:Traversable[String], b:(Option[BankMoneySource], Option[WholesalerMoneySource], Boolean)) =>
          (a, b) match {
            case (c, (bms, wms, cms)) => WhichCurrencies(c.toSeq, bms, wms, cms)
          }
    }
}

    private implicit def write[A]
    (implicit
    m: cats.Monoid[A],
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
    import jto.validation.forms.Rules._
    implicitly
  }

  val formW: Write[WhichCurrencies, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    implicitly
  }

  val jsonR: Reads[WhichCurrencies] = {
    import utils.JsonMapping._
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}

    implicitly[Reads[WhichCurrencies]]
  }


  val jsonW: Writes[WhichCurrencies] = {
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
