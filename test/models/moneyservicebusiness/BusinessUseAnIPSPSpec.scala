package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}
import scala.collection.Seq

class BusinessUseAnIPSPSpec extends PlaySpec {

  "BusinessUseAnIPSP" should {

    "FormValidation" must {

      val formData = Map("useAnIPSP" -> Seq("true"),
        "name" -> Seq("TEST"),
        "referenceNumber" -> Seq("123456789123456"))

      "Successfully read form data option yes" in {

        BusinessUseAnIPSP.formRule.validate(formData) must be(Success(BusinessUseAnIPSPYes("TEST", "123456789123456")))
      }

      "Successfully read form data option no" in {

        val map = Map("useAnIPSP" -> Seq("false"))
        BusinessUseAnIPSP.formRule.validate(map) must be(Success(BusinessUseAnIPSPNo))
      }

      "Throw an error message on missing mandatory field" in {

        BusinessUseAnIPSP.formRule.validate(Map.empty) must be(Failure(Seq((Path \ "useAnIPSP", Seq(ValidationError("error.required.msb.ipsp"))))))
      }

      "Throw an error message on missing mandatory field for option yes" in {

        val map = Map("useAnIPSP" -> Seq("true"),
          "name" -> Seq(""),
          "referenceNumber" -> Seq("1234567891"))

        BusinessUseAnIPSP.formRule.validate(map) must be(Failure(Seq((Path \ "name", Seq(ValidationError("error.required.msb.ipsp.name"))),
          (Path \ "referenceNumber", Seq(ValidationError("error.required.msb.ipsp.reference"))))))
      }


      "Throw an error message on invalid data" in {
        val map = Map("useAnIPSP" -> Seq("true"),
          "name" -> Seq("abcd" * 100),
          "referenceNumber" -> Seq("1234567891"))

        BusinessUseAnIPSP.formRule.validate(map) must be(Failure(Seq((Path \ "name", Seq(ValidationError("error.invalid.msb.ipsp.name"))),
          (Path \ "referenceNumber", Seq(ValidationError("error.required.msb.ipsp.reference"))))))

      }

      "Successfully write form data" in {

        val obj = BusinessUseAnIPSPYes("TEST", "123456789123456")
        BusinessUseAnIPSP.formWrites.writes(obj) must be(formData)

      }
    }

    "JsonValidation" must {

      "Successfully read the Json value" in {
        val data = BusinessUseAnIPSPYes("TEST", "123456789123456")
        BusinessUseAnIPSP.jsonReads.reads(BusinessUseAnIPSP.jsonWrites.writes(data)) must be (JsSuccess(data, JsPath \ "useAnIPSP"))

      }
    }
  }
}
