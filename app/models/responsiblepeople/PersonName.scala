package models.responsiblepeople

import org.joda.time.LocalDate

import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.libs.json.{Writes => _}
import utils.DateHelper
import utils.MappingUtils.Implicits._
import models.FormTypes._

case class PersonName(
                       firstName: String,
                       middleName: Option[String],
                       lastName: String,
                       previousName: Option[PreviousName],
                       otherNames: Option[String]
                     ) {

  val fullName = Seq(Some(firstName), middleName, Some(lastName)).flatten[String].mkString(" ")

}

object PersonName {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, PersonName] =
    From[UrlFormEncoded] { __ =>

      val otherNamesLength = 140
      val otherNamesType =
        required("error.required.rp.otherNames") compose
          maxWithMsg(otherNamesLength, "error.invalid.length.otherNames")

      (
        (__ \ "firstName").read(firstNameType) ~
        (__ \ "middleName").read(optionR(middleNameType)) ~
        (__ \ "lastName").read(lastNameType) ~
        (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName").flatMap[Option[PreviousName]] {
          case true =>
            (__ \ "previous").read[PreviousName] fmap Some.apply
          case false =>
            Rule(_ => Success(None))
        } ~
        (__ \ "hasOtherNames").read[Boolean].withMessage("error.required.rp.hasOtherNames").flatMap[Option[String]] {
          case true =>
            (__ \ "otherNames").read(otherNamesType) fmap Some.apply
          case false =>
            Rule(_ => Success(None))
        }
      )(PersonName.apply _)
    }

  implicit val formWrite = Write[PersonName, UrlFormEncoded] {
    model =>

      val name = Map(
        "firstName" -> Seq(model.firstName),
        "middleName" -> Seq(model.middleName getOrElse ""),
        "lastName" -> Seq(model.lastName)
      )

      val previousName = model.previousName match {
        case Some(previous) =>
          Map(
            "hasPreviousName" -> Seq("true"),
            "previous.firstName" -> Seq(previous.firstName getOrElse ""),
            "previous.middleName" -> Seq(previous.middleName getOrElse ""),
            "previous.lastName" -> Seq(previous.lastName getOrElse "")
          ) ++ (
            localDateWrite.writes(previous.date) map {
              case (path, value) =>
                s"previous.date.$path" -> value
            }
          )
        case None =>
          Map("hasPreviousName" -> Seq("false"))
      }

      val otherNames = model.otherNames match {
        case Some(otherNames) =>
          Map(
            "hasOtherNames" -> Seq("true"),
            "otherNames" -> Seq(otherNames)
          )
        case None =>
          Map("hasOtherNames" -> Seq("false"))
      }

      name ++ previousName ++ otherNames
  }

  implicit val format = Json.format[PersonName]
}
