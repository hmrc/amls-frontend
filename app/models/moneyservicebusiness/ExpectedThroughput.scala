package models.moneyservicebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ExpectedThroughput(throughput: String)

object ExpectedThroughput {

  implicit val format = Json.format[ExpectedThroughput]

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedThroughput] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "throughput").read[String].withMessage("error.required.msb.throughput") fmap ExpectedThroughput.apply
    }

  implicit val formWrites: Write[ExpectedThroughput, UrlFormEncoded] = Write {
      case ExpectedThroughput(b) =>
        Map("throughput" -> Seq(b))
    }
}
