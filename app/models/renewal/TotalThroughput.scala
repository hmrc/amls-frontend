/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

case class TotalThroughput(throughput: String)

object TotalThroughput {
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

  def labelFor(model: TotalThroughput)(implicit messages: Messages) = throughputValues.collectFirst {
    case Mapping(model.throughput, label, _) => messages(label)
  }.getOrElse("")

  implicit val format = Json.format[TotalThroughput]

  private val validSelectionRule: ValidationRule[String] = Rule.fromMapping[String, String] {
    case input if throughputValues.exists(_.value == input) => Success(input)
    case _ => Invalid(Seq(ValidationError("renewal.msb.throughput.selection.invalid")))
  }.repath(_ => Path \ "throughput")

  implicit val formReader: Rule[UrlFormEncoded, TotalThroughput] = From[UrlFormEncoded] { __ =>
    val fieldReader = (__ \ "throughput").read[String].withMessage("renewal.msb.throughput.selection.required")

    fieldReader andThen validSelectionRule map TotalThroughput.apply
  }

  implicit val formWriter: Write[TotalThroughput, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "throughput").write[String] contramap(_.throughput)
  }

  implicit def convert(model: TotalThroughput): ExpectedThroughput = {
    throughputValues.collectFirst {
      case x if x.value == model.throughput => x.submissionModel
    }.getOrElse(throw new Exception("Invalid MSB throughput value"))
  }

}


