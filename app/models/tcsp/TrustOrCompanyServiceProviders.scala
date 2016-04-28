package models.tcsp

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLength

case class TrustOrCompanyServiceProviders(serviceProviders: Set[TrustOrCompanyServiceProvider])

sealed trait TrustOrCompanyServiceProvider {
  val value: String =
    this match {
      case NomineeShareholdersProvider => "01"
      case TrusteeProvider => "02"
      case RegisteredOfficeEtc => "03"
      case CompanyDirectorEtc(_, _) => "04"
    }
}

case object NomineeShareholdersProvider extends TrustOrCompanyServiceProvider
case object TrusteeProvider extends TrustOrCompanyServiceProvider
case object RegisteredOfficeEtc extends TrustOrCompanyServiceProvider
case class CompanyDirectorEtc (
                                onlyOffTheShelfCompsSold:Boolean,
                                complexCorpStructureCreation: Boolean
                              ) extends TrustOrCompanyServiceProvider

object TrustOrCompanyServiceProviders {

  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, TrustOrCompanyServiceProviders] = {
    From[UrlFormEncoded] { __ =>
      (__ \ "serviceProviders").read(minLength[Set[String]](1).withMessage("error.required.tcsp.service.providers")) flatMap { service =>
        service.map {
          case "01" => Rule[UrlFormEncoded, TrustOrCompanyServiceProvider](_ => Success(NomineeShareholdersProvider))
          case "02" => Rule[UrlFormEncoded, TrustOrCompanyServiceProvider](_ => Success(TrusteeProvider))
          case "03" => Rule[UrlFormEncoded, TrustOrCompanyServiceProvider](_ => Success(RegisteredOfficeEtc))
          case "04" =>
            ((__ \ "onlyOffTheShelfCompsSold").read[Boolean].withMessage("error.required.tcsp.off.the.shelf.companies") and
              (__ \ "complexCorpStructureCreation").read[Boolean].withMessage("error.required.tcsp.complex.corporate.structures")) (CompanyDirectorEtc.apply _)
          case _ =>
            Rule[UrlFormEncoded, TrustOrCompanyServiceProvider] { _ =>
              Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[TrustOrCompanyServiceProvider]]](
          Rule[UrlFormEncoded, Set[TrustOrCompanyServiceProvider]](_ => Success(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m fmap {
                _ + x
              }
            }
        } fmap TrustOrCompanyServiceProviders.apply
      }
    }
  }

  implicit def formWrites = Write[TrustOrCompanyServiceProviders, UrlFormEncoded] {
    case TrustOrCompanyServiceProviders(services) =>
      Map(
        "serviceProviders[]" -> (services map { _.value }).toSeq
      ) ++ services.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, CompanyDirectorEtc(sold, creation)) =>
          m ++ Map("onlyOffTheShelfCompsSold" -> Seq(sold.toString),
                    "complexCorpStructureCreation" -> Seq(creation.toString))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[TrustOrCompanyServiceProviders] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (__ \ "serviceProviders").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(NomineeShareholdersProvider)) map identity[TrustOrCompanyServiceProvider]
        case "02" => Reads(_ => JsSuccess(TrusteeProvider)) map identity[TrustOrCompanyServiceProvider]
        case "03" => Reads(_ => JsSuccess(RegisteredOfficeEtc)) map identity[TrustOrCompanyServiceProvider]
        case "04" =>
          ((__ \ "onlyOffTheShelfCompsSold").read[Boolean] and
            (__ \ "complexCorpStructureCreation").read[Boolean])(CompanyDirectorEtc.apply _) map identity[TrustOrCompanyServiceProvider]
        case _ =>
          Reads(_ => JsError((JsPath \ "serviceProviders") -> ValidationError("error.invalid")))
      }.foldLeft[Reads[Set[TrustOrCompanyServiceProvider]]](
        Reads[Set[TrustOrCompanyServiceProvider]](_ => JsSuccess(Set.empty))
      ) {
        (result, data) =>
          data flatMap { m =>
            result.map { n =>
              n + m
            }
          }
      } map TrustOrCompanyServiceProviders.apply
    }
  }

  implicit val jsonWrite = Writes[TrustOrCompanyServiceProviders] {
    case TrustOrCompanyServiceProviders(services) =>
      Json.obj(
        "serviceProviders" -> (services map {
          _.value
        }).toSeq
      ) ++ services.foldLeft[JsObject](Json.obj()) {
        case (m, CompanyDirectorEtc(sold, creation)) =>
          m ++ Json.obj("onlyOffTheShelfCompsSold" -> sold.toString,
            "complexCorpStructureCreation" -> creation)
        case (m, _) =>
          m
      }
  }
}
