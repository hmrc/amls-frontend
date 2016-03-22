package models.businessmatching

import models.FormTypes
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Success, Path, Failure}
import play.api.data.validation.ValidationError
import play.api.libs.json.JsSuccess

class TypeOfBusinessSpec extends PlaySpec {

  "TypeOfBusiness" must {

    "validate form Read" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"))
      TypeOfBusiness.formRead.validate(formInput) must be(Success(TypeOfBusiness("sometext")))
    }

    "throw error when required field is missing" in {
      TypeOfBusiness.formRead.validate(Map.empty) must be(Failure(Seq((Path \ "typeOfBusiness", Seq(ValidationError("error.required"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"*10))
      TypeOfBusiness.formRead.validate(formInput) must be(Failure(Seq((Path \ "typeOfBusiness", Seq(ValidationError("error.maxLength", FormTypes.maxTypeOfBusinessLength))))))
    }

    "validate form write" in {
      TypeOfBusiness.formWrite.writes(TypeOfBusiness("sometext")) must be(Map("typeOfBusiness" -> Seq("sometext")))
    }

  }
}
