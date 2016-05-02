package models.tcsp

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLength

case class TcspTypes(serviceProviders: Set[ServiceProvider])

sealed trait ServiceProvider {
  val value: String =
    this match {
      case NomineeShareholdersProvider => "01"
      case TrusteeProvider => "02"
      case RegisteredOfficeEtc => "03"
      case CompanyDirectorEtc(_, _) => "04"
    }
}

case object NomineeShareholdersProvider extends ServiceProvider
case object TrusteeProvider extends ServiceProvider
case object RegisteredOfficeEtc extends ServiceProvider
case class CompanyDirectorEtc (
                                onlyOffTheShelfCompsSold:Boolean,
                                complexCorpStructureCreation: Boolean
                              ) extends ServiceProvider

object TcspTypes {

  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, TcspTypes] = {
    From[UrlFormEncoded] { __ =>
      (__ \ "serviceProviders").read(minLength[Set[String]](1).withMessage("error.required.tcsp.service.providers")) flatMap { service =>
        service.map {
          case "01" => Rule[UrlFormEncoded, ServiceProvider](_ => Success(NomineeShareholdersProvider))
          case "02" => Rule[UrlFormEncoded, ServiceProvider](_ => Success(TrusteeProvider))
          case "03" => Rule[UrlFormEncoded, ServiceProvider](_ => Success(RegisteredOfficeEtc))
          case "04" =>
            ((__ \ "onlyOffTheShelfCompsSold").read[Boolean].withMessage("error.required.tcsp.off.the.shelf.companies") and
              (__ \ "complexCorpStructureCreation").read[Boolean].withMessage("error.required.tcsp.complex.corporate.structures")) (CompanyDirectorEtc.apply _)
          case _ =>
            Rule[UrlFormEncoded, ServiceProvider] { _ =>
              Failure(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[ServiceProvider]]](
          Rule[UrlFormEncoded, Set[ServiceProvider]](_ => Success(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m fmap {
                _ + x
              }
            }
        } fmap TcspTypes.apply
      }
    }
  }

  implicit def formWrites = Write[TcspTypes, UrlFormEncoded] {
    case TcspTypes(services) =>
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

  implicit val jsonReads: Reads[TcspTypes] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (__ \ "serviceProviders").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(NomineeShareholdersProvider)) map identity[ServiceProvider]
        case "02" => Reads(_ => JsSuccess(TrusteeProvider)) map identity[ServiceProvider]
        case "03" => Reads(_ => JsSuccess(RegisteredOfficeEtc)) map identity[ServiceProvider]
        case "04" =>
          ((__ \ "onlyOffTheShelfCompsSold").read[Boolean] and
            (__ \ "complexCorpStructureCreation").read[Boolean])(CompanyDirectorEtc.apply _) map identity[ServiceProvider]
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
        case (m, CompanyDirectorEtc(sold, creation)) =>
          m ++ Json.obj("onlyOffTheShelfCompsSold" -> sold,
            "complexCorpStructureCreation" -> creation)
        case (m, _) =>
          m
      }
  }
}
