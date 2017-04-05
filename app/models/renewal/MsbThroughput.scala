package models.renewal

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Writes._
import models.ValidationRule
import models.moneyservicebusiness.ExpectedThroughput
import play.api.i18n.Messages
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbThroughput(throughput: String)

object MsbThroughput {
  private[renewal] case class Mapping(value: String, label: String, submissionModel: ExpectedThroughput)

  val throughputValues = Seq(
    Mapping("01", "renewal.msb.throughput.selection.1", ExpectedThroughput.First),
    Mapping("02", "renewal.msb.throughput.selection.2", ExpectedThroughput.Second),
    Mapping("03", "renewal.msb.throughput.selection.3", ExpectedThroughput.Third),
    Mapping("04", "renewal.msb.throughput.selection.4", ExpectedThroughput.Fourth),
    Mapping("05", "renewal.msb.throughput.selection.5", ExpectedThroughput.Fifth),
    Mapping("06", "renewal.msb.throughput.selection.6", ExpectedThroughput.Sixth),
    Mapping("07", "renewal.msb.throughput.selection.7", ExpectedThroughput.Seventh)
  )

  def labelFor(model: MsbThroughput)(implicit messages: Messages) = throughputValues.collectFirst {
    case Mapping(model.throughput, label, _) => messages(label)
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

  implicit def convert(model: MsbThroughput): ExpectedThroughput = {
    throughputValues.collectFirst {
      case x if x.value == model.throughput => x.submissionModel
    }.getOrElse(throw new Exception("Invalid MSB throughput value"))
  }

}


