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

    "validate model with residential estate option selected and redress option yes selected" in {
      val model = Map(
        "services" -> Seq("02", "01", "09"),
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("02"))

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Auction, Commercial, Residential(Some(OmbudsmanServices)))))

    }

    "validate model with residential estate agency check box selected" in {
      val model = Map(
        "services" -> Seq("09"),
        "isRedress" -> Seq("false")
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Residential(Some(RedressSchemedNo)))))
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

    "write correct data for services value when residential option is selected" in {

      Service.formWrites.writes(Seq(Auction, Commercial)) must
        be(Map("services" -> Seq("02","01")))
    }

    "write correct data for services value when residential option is selected1" in {

      Service.formWrites.writes(Seq(Residential(Some(OmbudsmanServices)))) must
        be(Map("services" -> Seq( "09"),
          "isRedress" -> Seq("true"),
          "propertyRedressScheme" -> Seq("02")))
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {
        val json =  Json.obj("services" -> Seq("01","02", "09"),
          "isRedress"-> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> "test")

        Json.fromJson[Seq[Service]](json) must
          be(JsSuccess(Seq(Commercial, Auction, Residential(Some(OmbudsmanServices))), JsPath \ "services"))
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

    "successfully validate json write" in {
      val json = Json.obj("services" -> Seq("01","02", "09"),
        "isRedress"-> true,
        "propertyRedressScheme" -> "04",
        "propertyRedressSchemeOther" -> "test")

      Json.toJson(Seq(Commercial, Auction, Residential(Some(Other("test"))))) must be(json)

    }
  }
}
