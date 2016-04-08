package models.estateagentbusiness
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._



class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    val businessServices:Set[Service] = Set(Residential, Commercial, Auction, Relocation,
                                            BusinessTransfer, AssetManagement, LandManagement, Development, SocialHousing)
    import play.api.data.mapping.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "services[]" -> Seq("01","02","03","04","05","06","07","08","09")
      )

      Services.formReads.validate(model) must
        be(Success(Services(businessServices)))

    }

    "fail to validate on empty Map" in {

      Services.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "services") -> Seq(ValidationError("error.required.eab.business.services")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services[]" -> Seq("02", "99", "03")
      )

      Services.formReads.validate(model) must
        be(Failure(Seq((Path \ "services"\ 1 \ "services", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      Services.formWrites.writes(Services(Set(Residential, Commercial, Auction))) must
        be(Map("services[]" -> Seq("01","02","03")))

      Services.formWrites.writes(Services(Set(AssetManagement, BusinessTransfer, LandManagement))) must
        be(Map("services[]" -> Seq("06","05","07")))

      Services.formWrites.writes(Services(Set(Relocation, Development, SocialHousing))) must
        be(Map("services[]" -> Seq("04","08","09")))

    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("services" -> Seq("01","02","03","04","05","06","07","08","09"))

        Json.fromJson[Services](json) must
          be(JsSuccess(Services(businessServices), JsPath \ "services"))
      }

      "fail when on invalid data" in {
        Json.fromJson[Services](Json.obj("services" -> Seq("40"))) must
          be(JsError(((JsPath \ "services")(0) \ "services") -> ValidationError("error.invalid")))
      }
    }

    "successfully validate json write" in {
      val json = Json.obj("services" -> Set("01","02","03","04","05","06","07","08","09"))
      Json.toJson(Services(businessServices)) must be(json)

    }
  }
}
