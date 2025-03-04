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

package models.businessmatching

import models.{CheckYourAnswersField, DateOfChange, Enumerable, WithName}
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

case class BusinessActivities(
  businessActivities: Set[BusinessActivity],
  additionalActivities: Option[Set[BusinessActivity]] = None,
  removeActivities: Option[Set[BusinessActivity]] = None,
  dateOfChange: Option[DateOfChange] = None
) {

  def hasBusinessOrAdditionalActivity(activity: BusinessActivity) =
    businessActivities.union(additionalActivities.getOrElse(Set.empty)) contains activity

  def hasOnlyOneBusinessActivity: Boolean =
    businessActivities.size == 1
}

sealed trait BusinessActivity extends CheckYourAnswersField {

  val value: String

  def getMessage(usePhrasedMessage: Boolean = false)(implicit messages: Messages): String = {
    val phrasedString = if (usePhrasedMessage) ".phrased" else ""
    val message       = s"businessmatching.registerservices.servicename.lbl."

    messages(s"$message$value$phrasedString")
  }
}

object BusinessActivity extends Enumerable.Implicits {

  case object AccountancyServices extends WithName("accountancyServices") with BusinessActivity {
    override val value: String = "01"
  }

  case object ArtMarketParticipant extends WithName("artMarketParticipant") with BusinessActivity {
    override val value: String = "02"
  }

  case object BillPaymentServices extends WithName("billPaymentServices") with BusinessActivity {
    override val value: String = "03"
  }

  case object EstateAgentBusinessService extends WithName("estateAgentBusinessService") with BusinessActivity {
    override val value: String = "04"
  }

  case object HighValueDealing extends WithName("highValueDealing") with BusinessActivity {
    override val value: String = "05"
  }

  case object MoneyServiceBusiness extends WithName("moneyServiceBusiness") with BusinessActivity {
    override val value: String = "06"
  }

  case object TrustAndCompanyServices extends WithName("trustAndCompanyServices") with BusinessActivity {
    override val value: String = "07"
  }

  case object TelephonePaymentService extends WithName("telephonePaymentService") with BusinessActivity {
    override val value: String = "08"
  }

  implicit val jsonActivityReads: Reads[BusinessActivity] = Reads {
    case JsString("01") => JsSuccess(AccountancyServices)
    case JsString("08") => JsSuccess(ArtMarketParticipant)
    case JsString("02") => JsSuccess(BillPaymentServices)
    case JsString("03") => JsSuccess(EstateAgentBusinessService)
    case JsString("04") => JsSuccess(HighValueDealing)
    case JsString("05") => JsSuccess(MoneyServiceBusiness)
    case JsString("06") => JsSuccess(TrustAndCompanyServices)
    case JsString("07") => JsSuccess(TelephonePaymentService)
    case _              => JsError((JsPath \ "businessActivities") -> play.api.libs.json.JsonValidationError("error.invalid"))
  }

  implicit val jsonActivityWrite: Writes[BusinessActivity] = Writes[BusinessActivity] {
    case AccountancyServices        => JsString("01")
    case ArtMarketParticipant       => JsString("08")
    case BillPaymentServices        => JsString("02")
    case EstateAgentBusinessService => JsString("03")
    case HighValueDealing           => JsString("04")
    case MoneyServiceBusiness       => JsString("05")
    case TrustAndCompanyServices    => JsString("06")
    case TelephonePaymentService    => JsString("07")
  }

  implicit val enumerable: Enumerable[BusinessActivity] =
    Enumerable(BusinessActivities.all.toSeq.map(v => v.toString -> v): _*)
}

object BusinessActivities extends Logging {

  import models.businessmatching.BusinessActivity._
  import utils.MappingUtils.Implicits._

  val all: Set[BusinessActivity] = Set(
    AccountancyServices,
    ArtMarketParticipant,
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  def formValues(filterValues: Option[Seq[BusinessActivity]] = None, hasHints: Boolean = true)(implicit
    messages: Messages
  ): Seq[CheckboxItem] = {

    val filteredValues = filterValues.fold(all.toSeq)(all.toSeq diff _)

    filteredValues
      .map { activity =>
        val hintOpt = if (hasHints) {
          Some(
            Hint(
              id = Some(s"businessActivities-${activity.value}-hint"),
              content = Text(messages(s"businessmatching.registerservices.servicename.details.${activity.value}"))
            )
          )
        } else {
          None
        }

        val id = activity.value.substring(1)

        CheckboxItem(
          content = Text(messages(s"businessmatching.registerservices.servicename.lbl.${activity.value}")),
          value = activity.toString,
          id = Some(s"value_$id"),
          name = Some(s"value[$id]"),
          hint = hintOpt
        )
      }
      .sortBy(_.content.mkString)
  }

  implicit val format: OWrites[BusinessActivities] = Json.writes[BusinessActivities]

  implicit val jsonReads: Reads[BusinessActivities] = {
    import play.api.libs.json.Reads.StringReads
    ((__ \ "businessActivities").read[Set[String]].flatMap[Set[BusinessActivity]] { ba =>
      activitiesReader(ba, "businessActivities").foldLeft[Reads[Set[BusinessActivity]]](
        Reads[Set[BusinessActivity]](_ => JsSuccess(Set.empty))
      ) { (result, data) =>
        data flatMap { r =>
          result.map(_ + r)
        }
      }
    } and
      (__ \ "additionalActivities").readNullable[Set[String]].flatMap[Option[Set[BusinessActivity]]] {
        case Some(a) =>
          activitiesReader(a, "additionalActivities").foldLeft[Reads[Option[Set[BusinessActivity]]]](
            Reads[Option[Set[BusinessActivity]]](_ => JsSuccess(None))
          ) { (result, data) =>
            data flatMap { r =>
              result.map {
                case Some(n) => Some(n + r)
                case _       => Some(Set(r))
              }
            }
          }
        case _       => None
      } and (__ \ "removeActivities").readNullable[Set[String]].flatMap[Option[Set[BusinessActivity]]] {
        case Some(a) =>
          activitiesReader(a, "removeActivities").foldLeft[Reads[Option[Set[BusinessActivity]]]](
            Reads[Option[Set[BusinessActivity]]](_ => JsSuccess(None))
          ) { (result, data) =>
            data flatMap { r =>
              result.map {
                case Some(n) => Some(n + r)
                case _       => Some(Set(r))
              }
            }
          }
        case _       => None
      } and (__ \ "dateOfChange").readNullable[DateOfChange])((a, b, c, d) => BusinessActivities(a, b, c, d))
  }

  private def activitiesReader(values: Set[String], path: String): Set[Reads[_ <: BusinessActivity]] =
    values map {
      case "01" => Reads(_ => JsSuccess(AccountancyServices)) map identity[BusinessActivity]
      case "08" => Reads(_ => JsSuccess(ArtMarketParticipant)) map identity[BusinessActivity]
      case "02" => Reads(_ => JsSuccess(BillPaymentServices)) map identity[BusinessActivity]
      case "03" => Reads(_ => JsSuccess(EstateAgentBusinessService)) map identity[BusinessActivity]
      case "04" => Reads(_ => JsSuccess(HighValueDealing)) map identity[BusinessActivity]
      case "05" => Reads(_ => JsSuccess(MoneyServiceBusiness)) map identity[BusinessActivity]
      case "06" => Reads(_ => JsSuccess(TrustAndCompanyServices)) map identity[BusinessActivity]
      case "07" => Reads(_ => JsSuccess(TelephonePaymentService)) map identity[BusinessActivity]
      case _    =>
        Reads(_ => JsError((JsPath \ path) -> play.api.libs.json.JsonValidationError("error.invalid")))
    }

  def getValue(ba: BusinessActivity): String =
    ba match {
      case AccountancyServices        => "01"
      case ArtMarketParticipant       => "02"
      case BillPaymentServices        => "03"
      case EstateAgentBusinessService => "04"
      case HighValueDealing           => "05"
      case MoneyServiceBusiness       => "06"
      case TrustAndCompanyServices    => "07"
      case TelephonePaymentService    => "08"
    }

  def getBusinessActivity(ba: String): BusinessActivity =
    ba match {
      case "01" => AccountancyServices
      case "02" => ArtMarketParticipant
      case "03" => BillPaymentServices
      case "04" => EstateAgentBusinessService
      case "05" => HighValueDealing
      case "06" => MoneyServiceBusiness
      case "07" => TrustAndCompanyServices
      case "08" => TelephonePaymentService
    }
}
