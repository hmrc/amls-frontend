package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    "validate model with few check box selected" in {
      val model = Map(
        "services" -> Seq("02", "01", "03")
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Auction, Commercial, Relocation)))

    }

   /* "validate model with residential estate option selected and redress option yes selected" in {
      val model = Map(
        "services" -> Seq("02", "01", "05"),
        "isRedress" -> true ,
        "PropertyRedressScheme" -> ""
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Auction, Commercial, Relocation), RedressRegisteredYes(OmbudsmanServices)))

    }*/

    "validate model with residential estate agency check box selected" in {
      val model = Map(
        "services" -> Seq("09")
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Residential(None))))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "services" -> Seq("")
      )

      Service.servicesFormRule.validate(data) must
        be(Failure(Seq(
          (Path \ "services") -> Seq(ValidationError("Invalid Service Type String "))
        )))
    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services" -> Seq("02", "99", "03")
      )

      Service.servicesFormRule.validate(model) must
        be(Failure(Seq((Path \ "services", Seq(ValidationError("Invalid Service Type String 99"))))))
    }

    "write correct data for services value" in {

      Service.formWrites.writes(Seq(Auction, Commercial, Relocation)) must
        be(Map("services" -> Seq("02","01", "03")))
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {
        Json.fromJson[Seq[Service]](Json.obj("services" -> Seq("01","02"))) must
          be(JsSuccess(Seq(Commercial, Auction), JsPath \ "services"))
      }

      "fail when on invalid data" in {
        Json.fromJson[Seq[Service]](Json.obj("service" -> "01")) must
          be(JsError((JsPath \ "services") -> ValidationError("error.path.missing")))
      }
    }

    "json write" must {
      Json.toJson(Seq(Auction)) must
        be(Json.obj("services" -> Seq("02")))
    }
  }
}
