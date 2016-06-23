package models.moneyservicebusiness

import models.Country
import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Success, Write}
import play.api.data.mapping._
import play.api.libs.json.{Json, Reads, Writes}

case class SendTheLargestAmountsOfMoney (
                       country_1: Country,
                       country_2: Option[Country] = None,
                       country_3: Option[Country] = None
                     ) {

  def countryList = {
    this.productIterator.collect {
      case Some(x: Country) => x
      case x: Country => x
    }
  }
}

object SendTheLargestAmountsOfMoney {

  implicit val format = Json.format[SendTheLargestAmountsOfMoney]

  implicit val formRule: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = From[UrlFormEncoded] { __ =>
    import utils.MappingUtils.Implicits._
    import play.api.data.mapping.forms.Rules._
        ((__ \ "country_1").read[Country].withMessage("error.required.country.name") and
          (__ \ "country_2").read[Option[Country]] and
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
}
