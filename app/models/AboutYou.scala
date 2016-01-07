package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class YourDetails(firstName: String, middleName: Option[String], lastName: String)

object YourDetails {
  implicit val formats = Json.format[YourDetails]
}

case class RoleWithinBusiness(roleWithinBusiness: String, other: String)

object RoleWithinBusiness {
  implicit val formats = Json.format[RoleWithinBusiness]
}

case class AboutYou(yourDetails: Option[YourDetails], roleWithinBusiness: Option[RoleWithinBusiness])

object AboutYou {

  val key = "about-you"

  implicit val reads: Reads[AboutYou] = (
    __.read[Option[YourDetails]] and
      __.read[Option[RoleWithinBusiness]]
    ) (AboutYou.apply _)

  implicit val writes: Writes[AboutYou] = Writes[AboutYou] {
    model =>
      Seq(
        Json.toJson(model.yourDetails).asOpt[JsObject],
        Json.toJson(model.roleWithinBusiness).asOpt[JsObject]
      ).flatten.fold(Json.obj()) { _ ++ _ }
  }

  def merge(aboutYou : Option[AboutYou], yourDetails : YourDetails) = {
    aboutYou.fold {
      AboutYou(Some(yourDetails), None)
    } {
      _.copy(yourDetails = Some(yourDetails))
    }
  }

  def merge(aboutYou : Option[AboutYou], yourRole : RoleWithinBusiness) =  {
    aboutYou.fold {
      AboutYou(None, Some(yourRole))
    } {
      _.copy(roleWithinBusiness = Some(yourRole))
    }
  }
}