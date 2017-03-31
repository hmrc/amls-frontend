package models.renewal

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Writes._
import models.ValidationRule
import play.api.i18n.Messages
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbThroughput(throughput: String)

object MsbThroughput {

  case class FormSelection(value: String, label: String)

  val throughputValues = Seq(
    FormSelection("01", "renewal.msb.throughput.selection.1"),
    FormSelection("02", "renewal.msb.throughput.selection.2"),
    FormSelection("03", "renewal.msb.throughput.selection.3"),
    FormSelection("04", "renewal.msb.throughput.selection.4"),
    FormSelection("05", "renewal.msb.throughput.selection.5"),
    FormSelection("06", "renewal.msb.throughput.selection.6"),
    FormSelection("07", "renewal.msb.throughput.selection.7")
  )

  def labelFor(model: MsbThroughput)(implicit messages: Messages) = throughputValues.collectFirst {
    case FormSelection(model.throughput, label) => messages(label)
  }.getOrElse("")

  implicit val format = Json.format[MsbThroughput]

  private val validSelectionRule: ValidationRule[String] = Rule.fromMapping[String, String] {
    case input if throughputValues.exists(_.value == input) => Success(input)
    case _ => Invalid(Seq(ValidationError("renewal.msb.throughput.selection.invalid")))
  }.repath(_ => Path \ "throughput")

  implicit val formReader: Rule[UrlFormEncoded, MsbThroughput] = From[UrlFormEncoded] { __ =>
    val fieldReader = (__ \ "throughput").read[String].withMessage("renewal.msb.throughput.selection.required")

    fieldReader andThen validSelectionRule map MsbThroughput.apply
  }

  implicit val formWriter: Write[MsbThroughput, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "throughput").write[String] contramap(_.throughput)
  }

}


