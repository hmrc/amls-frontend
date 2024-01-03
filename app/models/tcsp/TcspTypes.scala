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

package models.tcsp

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.forms.Rules.{minLength => _, _}
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{CheckboxItem, Text}
import utils.TraversableValidators.minLengthR

case class TcspTypes(serviceProviders: Set[ServiceProvider])

sealed trait ServiceProvider {
  val value: String
}

object TcspTypes extends Enumerable.Implicits {

  case object NomineeShareholdersProvider extends WithName("nomineeShareholdersProvider") with ServiceProvider {
    override val value: String = "01"
  }

  case object TrusteeProvider extends WithName("trusteeProvider") with ServiceProvider {
    override val value: String = "02"
  }

  case object RegisteredOfficeEtc extends WithName("registeredOfficeEtc") with ServiceProvider {
    override val value: String = "03"
  }

  case object CompanyDirectorEtc extends WithName("companyDirectorEtc") with ServiceProvider {
    override val value: String = "04"
  }

  case object CompanyFormationAgent extends WithName("companyFormationAgent") with ServiceProvider {
    override val value: String = "05"
  }

  val all: Seq[ServiceProvider] = Seq(
    NomineeShareholdersProvider,
    TrusteeProvider,
    RegisteredOfficeEtc,
    CompanyDirectorEtc,
    CompanyFormationAgent
  )

  def formValues(implicit messages: Messages): Seq[CheckboxItem] = all.zipWithIndex.map { case (sp, index) =>
    CheckboxItem(
      content = Text(messages(s"tcsp.service.provider.lbl.${sp.value}")),
      value = sp.toString,
      id = Some(s"serviceProviders_$index"),
      name = Some(s"serviceProviders[$index]")
    )
  }.sortBy(_.content.asHtml.body)

  implicit val enumerable: Enumerable[ServiceProvider] = Enumerable(all.map(v => v.toString -> v): _*)

  import utils.MappingUtils.Implicits._
  import cats.data.Validated.{Invalid, Valid}

  implicit val formReads: Rule[UrlFormEncoded, TcspTypes] = {
    From[UrlFormEncoded] { __ =>
      (__ \ "serviceProviders").read(minLengthR[Set[String]](1).withMessage("error.required.tcsp.service.providers")) flatMap { service =>
        service.map {
          case "01" => Rule[UrlFormEncoded, ServiceProvider](_ => Valid(NomineeShareholdersProvider))
          case "02" => Rule[UrlFormEncoded, ServiceProvider](_ => Valid(TrusteeProvider))
          case "03" => Rule[UrlFormEncoded, ServiceProvider](_ => Valid(RegisteredOfficeEtc))
          case "04" => Rule[UrlFormEncoded, ServiceProvider](_ => Valid(CompanyDirectorEtc))
          case "05" => Rule[UrlFormEncoded, ServiceProvider](_ => Valid(CompanyFormationAgent))
          case _ =>
            Rule[UrlFormEncoded, ServiceProvider] { _ =>
              Invalid(Seq((Path \ "serviceProviders") -> Seq(jto.validation.ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[ServiceProvider]]](
          Rule[UrlFormEncoded, Set[ServiceProvider]](_ => Valid(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m map {
                _ + x
              }
            }
        } map TcspTypes.apply
      }
    }
  }

  implicit def formWrites = Write[TcspTypes, UrlFormEncoded] {
    case TcspTypes(services) =>
      Map(
        "serviceProviders[]" -> (services map { _.value }).toSeq
      ) ++ services.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, _) =>
          m
      }
  }

  import play.api.libs.json.JsonValidationError
  import play.api.libs.json._

  implicit val jsonReads: Reads[TcspTypes] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._

    (__ \ "serviceProviders").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(NomineeShareholdersProvider)) map identity[ServiceProvider]
        case "02" => Reads(_ => JsSuccess(TrusteeProvider)) map identity[ServiceProvider]
        case "03" => Reads(_ => JsSuccess(RegisteredOfficeEtc)) map identity[ServiceProvider]
        case "04" => Reads(_ => JsSuccess(CompanyDirectorEtc)) map identity[ServiceProvider]
        case "05" => Reads(_ => JsSuccess(CompanyFormationAgent)) map identity[ServiceProvider]
        case _ =>
          Reads(_ => JsError((JsPath \ "serviceProviders") -> JsonValidationError("error.invalid")))
      }.foldLeft[Reads[Set[ServiceProvider]]](
        Reads[Set[ServiceProvider]](_ => JsSuccess(Set.empty))
      ) {
        (result, data) =>
          data flatMap { m =>
            result.map { n =>
              n + m
            }
          }
      } map TcspTypes.apply
    }
  }

  implicit val jsonWrite = Writes[TcspTypes] {
    case TcspTypes(services) =>
      Json.obj(
        "serviceProviders" -> (services map {
          _.value
        }).toSeq
      ) ++ services.foldLeft[JsObject](Json.obj()) {
        case (m, _) =>
          m
      }
  }
}
