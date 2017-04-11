package models.renewal

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.Country
import play.api.libs.json.Json

case class SendTheLargestAmountsOfMoney (
                       country_1: Country,
                       country_2: Option[Country] = None,
                       country_3: Option[Country] = None
                     ) {

  def countryList = {
    this.productIterator.collect {
      case Some(Country(name, code)) => Country(name, code)
      case x: Country => x
    }
  }

}

object SendTheLargestAmountsOfMoney {

  implicit val format = Json.format[SendTheLargestAmountsOfMoney]

  implicit val formRule: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import utils.MappingUtils.Implicits._
        ((__ \ "country_1").read[Country].withMessage("error.required.country.name") ~
          (__ \ "country_2").read[Option[Country]] ~
          (__ \ "country_3").read[Option[Country]]
          )(SendTheLargestAmountsOfMoney.apply _)
    }

  implicit val formWrites: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = Write {countries =>
      Map(
        "country_1" -> Seq(countries.country_1.code),
        "country_2" -> (countries.country_2.toSeq map { _.code }),
        "country_3" -> (countries.country_3.toSeq map { _.code })
      )
    }

  implicit def convert(model: SendTheLargestAmountsOfMoney): models.moneyservicebusiness.SendTheLargestAmountsOfMoney = {
    ???
  }
}
