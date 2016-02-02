package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    "validate model with only 2 check box selected" in {
      val model = Map(
        "services" -> Seq("02", "01", "03")
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Auction, Commercial, Relocation)))

    }

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
//        val js = Json.obj("services" -> Seq("01","02"))
//        val k: JsResult[Service] = Json.fromJson[Service](js)
//        k match
//          {
//          case d:JsSuccess[Service] => println(d)
//        }

        val noData = Json.fromJson[Seq[Service]](Json.obj("services" -> ""))

        val k = Json.fromJson[Seq[Service]](Json.obj("services" -> Seq("01","02")))
        println(k)
        Json.fromJson[Seq[Service]](Json.obj("services" -> Seq("01","02"))) must
          be(JsSuccess(Seq(Commercial, Auction), JsPath \ "services"))

//        Json.fromJson[Seq[Service]](Json.obj("services" -> Seq("01","05"))) must
//          be(JsSuccess(Seq(Commercial, Auction), JsPath \ "services"))

        noData must be(JsSuccess(Seq(Commercial, Auction), JsPath \ "services"))
      }
    }

    "json write" must {
      Json.toJson(Seq(Auction)) must
        be(Json.obj("services" -> Seq("02")))
    }
  }
}
