/*
 * Copyright 2024 HM Revenue & Customs
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

package models.asp

import models.{DateOfChange, Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

case class ServicesOfBusiness(services: Set[Service], dateOfChange: Option[DateOfChange] = None)

sealed trait Service {

  import Service._

  val value: String

  val message                                         = "asp.service.lbl."
  def getMessage(implicit messages: Messages): String =
    this match {
      case Accountancy          => messages(s"${message}01")
      case PayrollServices      => messages(s"${message}02")
      case BookKeeping          => messages(s"${message}03")
      case Auditing             => messages(s"${message}04")
      case FinancialOrTaxAdvice => messages(s"${message}05")
    }
}

object Service extends Enumerable.Implicits {

  case object Accountancy extends WithName("accountancy") with Service {
    override val value = "01"
  }

  case object PayrollServices extends WithName("payrollServices") with Service {
    override val value = "02"
  }

  case object BookKeeping extends WithName("bookKeeping") with Service {
    override val value = "03"
  }

  case object Auditing extends WithName("auditing") with Service {
    override val value = "04"
  }

  case object FinancialOrTaxAdvice extends WithName("financialOrTaxAdvice") with Service {
    override val value = "05"
  }

  def formValues(implicit messages: Messages): Seq[CheckboxItem] = all.zipWithIndex
    .map { case (service, index) =>
      CheckboxItem(
        content = Text(messages(s"asp.service.lbl.${service.value}")),
        value = service.toString,
        id = Some(s"services_$index"),
        name = Some(s"services[$index]")
      )
    }
    .sortBy(_.content.asHtml.body)

  val all: Seq[Service] = Seq(
    Accountancy,
    PayrollServices,
    BookKeeping,
    Auditing,
    FinancialOrTaxAdvice
  )

  implicit val enumerable: Enumerable[Service] = Enumerable(all.map(v => v.toString -> v): _*)

  import play.api.libs.json.JsonValidationError
  implicit val jsonServiceReads: Reads[Service] =
    Reads {
      case JsString("01") => JsSuccess(Accountancy)
      case JsString("02") => JsSuccess(PayrollServices)
      case JsString("03") => JsSuccess(BookKeeping)
      case JsString("04") => JsSuccess(Auditing)
      case JsString("05") => JsSuccess(FinancialOrTaxAdvice)
      case _              => JsError((JsPath \ "services") -> JsonValidationError("error.invalid"))
    }

  implicit val jsonServiceWrites: Writes[Service] = Writes[Service] {
    case Accountancy          => JsString("01")
    case PayrollServices      => JsString("02")
    case BookKeeping          => JsString("03")
    case Auditing             => JsString("04")
    case FinancialOrTaxAdvice => JsString("05")
  }

}

object ServicesOfBusiness {
  implicit val formats: OFormat[ServicesOfBusiness] = Json.format[ServicesOfBusiness]
}
