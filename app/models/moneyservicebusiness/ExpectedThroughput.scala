package models.moneyservicebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json.Json

case class ExpectedThroughput(throughput: String)

object ExpectedThroughput {

  implicit val format = Json.format[ExpectedThroughput]

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedThroughput] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "throughput").read[String].withMessage("error.required.msb.throughput") flatMap {x =>
        x match {
          case "01" | "02" | "03" | "04" | "05" | "06" | "07" => ExpectedThroughput.apply(x)
          case _ =>  (Path \ "throughput") -> Seq(ValidationError("error.invalid"))
        }
      }
    }

  implicit val formWrites: Write[ExpectedThroughput, UrlFormEncoded] = Write {
      case ExpectedThroughput(b) =>
        Map("throughput" -> Seq(b))
    }
}
