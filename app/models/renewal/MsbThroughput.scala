package models.renewal

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Writes._
import models.ValidationRule
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbThroughput(throughputSelection: String)

object MsbThroughput {

  case class Selection(code: String, text: String)

  val values = Seq(
    Selection("01", "renewal.msb.throughput.selection.1"),
    Selection("02", "renewal.msb.throughput.selection.2"),
    Selection("03", "renewal.msb.throughput.selection.3"),
    Selection("04", "renewal.msb.throughput.selection.4"),
    Selection("05", "renewal.msb.throughput.selection.5"),
    Selection("06", "renewal.msb.throughput.selection.6"),
    Selection("07", "renewal.msb.throughput.selection.7")
  )

  implicit val format = Json.format[MsbThroughput]

  val validSelectionRule: ValidationRule[String] = Rule.fromMapping[String, String] {
    case input if values.exists(_.code == input) => Success(input)
    case _ => Invalid(Seq(ValidationError("renewal.msb.throughput.selection.invalid")))
  }.repath(_ => Path \ "throughputSelection")

  implicit val formReader: Rule[UrlFormEncoded, MsbThroughput] = From[UrlFormEncoded] { __ =>
    val fieldReader = (__ \ "throughputSelection").read[String].withMessage("renewal.msb.throughput.selection.required")

    fieldReader andThen validSelectionRule map MsbThroughput.apply
  }

  implicit val formWriter: Write[MsbThroughput, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "throughputSelection").write[String] contramap(_.throughputSelection)
  }

}


