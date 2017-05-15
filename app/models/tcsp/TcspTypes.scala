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

package models.tcsp

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR

case class TcspTypes(serviceProviders: Set[ServiceProvider])

sealed trait ServiceProvider {
  val value: String =
    this match {
      case NomineeShareholdersProvider => "01"
      case TrusteeProvider => "02"
      case RegisteredOfficeEtc => "03"
      case CompanyDirectorEtc => "04"
      case CompanyFormationAgent(_, _) => "05"
    }
}

case object NomineeShareholdersProvider extends ServiceProvider
case object TrusteeProvider extends ServiceProvider
case object RegisteredOfficeEtc extends ServiceProvider
case object CompanyDirectorEtc extends ServiceProvider
case class CompanyFormationAgent (
                                onlyOffTheShelfCompsSold:Boolean,
                                complexCorpStructureCreation: Boolean
                              ) extends ServiceProvider

object TcspTypes {

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
          case "05" =>
            ((__ \ "onlyOffTheShelfCompsSold").read[Boolean].withMessage("error.required.tcsp.off.the.shelf.companies") ~
              (__ \ "complexCorpStructureCreation").read[Boolean].withMessage("error.required.tcsp.complex.corporate.structures")
              ) (CompanyFormationAgent.apply _)
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
        case (m, CompanyFormationAgent(sold, creation)) =>
          m ++ Map("onlyOffTheShelfCompsSold" -> Seq(sold.toString),
                    "complexCorpStructureCreation" -> Seq(creation.toString))
        case (m, _) =>
          m
      }
  }

  import play.api.data.validation.ValidationError
  import play.api.libs.json._

  implicit val jsonReads: Reads[TcspTypes] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (__ \ "serviceProviders").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(NomineeShareholdersProvider)) map identity[ServiceProvider]
        case "02" => Reads(_ => JsSuccess(TrusteeProvider)) map identity[ServiceProvider]
        case "03" => Reads(_ => JsSuccess(RegisteredOfficeEtc)) map identity[ServiceProvider]
        case "04" => Reads(_ => JsSuccess(CompanyDirectorEtc)) map identity[ServiceProvider]
        case "05" =>
          ((__ \ "onlyOffTheShelfCompsSold").read[Boolean] and
            (__ \ "complexCorpStructureCreation").read[Boolean])(CompanyFormationAgent.apply _) map identity[ServiceProvider]
        case _ =>
          Reads(_ => JsError((JsPath \ "serviceProviders") -> ValidationError("error.invalid")))
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
        case (m, CompanyFormationAgent(sold, creation)) =>
          m ++ Json.obj("onlyOffTheShelfCompsSold" -> sold,
            "complexCorpStructureCreation" -> creation)
        case (m, _) =>
          m
      }
  }
}
