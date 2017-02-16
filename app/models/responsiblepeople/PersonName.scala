package models.responsiblepeople

import jto.validation.forms.Rules._
import jto.validation.forms._
import jto.validation._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import models.FormTypes._
import cats.data.Validated.{Invalid, Valid}

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
        required("error.required.rp.otherNames") andThen
          maxWithMsg(otherNamesLength, "error.invalid.length.otherNames")

      (
        (__ \ "firstName").read(genericNameRule("error.required.rp.first_name", "error.invalid.length.firstname")) ~
        (__ \ "middleName").read(optionR(genericNameRule(maxLengthMsg = "error.invalid.length.middlename"))) ~
        (__ \ "lastName").read(genericNameRule("error.required.rp.last_name", "error.invalid.length.lastname")) ~
        (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName").flatMap[Option[PreviousName]] {
          case true =>
            (__ \ "previous").read[PreviousName] map Some.apply
          case false =>
            Rule(_ => Valid(None)) 
        } ~
        (__ \ "hasOtherNames").read[Boolean].withMessage("error.required.rp.hasOtherNames").flatMap[Option[String]] {
          case true =>
            (__ \ "otherNames").read(otherNamesType) map Some.apply
          case false =>
            Rule(_ => Valid(None))
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
