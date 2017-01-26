package models.businessmatching

import org.scalatestplus.play.PlaySpec
import jto.validation.{Valid, Path, Invalid}
import jto.validation.ValidationError

class TypeOfBusinessSpec extends PlaySpec {

  "TypeOfBusiness" must {

    "validate form Read" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"))
      TypeOfBusiness.formRead.validate(formInput) must be(Valid(TypeOfBusiness("sometext")))
    }

    "throw error when required field is missing" in {
      val formInput = Map("typeOfBusiness" -> Seq(""))
      TypeOfBusiness.formRead.validate(formInput) must be(Invalid(Seq((Path \ "typeOfBusiness", Seq(ValidationError("error.required.bm.businesstype.type"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"*10))
      TypeOfBusiness.formRead.validate(formInput) must be(Invalid(Seq((Path \ "typeOfBusiness") -> Seq(ValidationError("error.invalid.bm.business.type")))))
    }

    "validate form write" in {
      TypeOfBusiness.formWrite.writes(TypeOfBusiness("sometext")) must be(Map("typeOfBusiness" -> Seq("sometext")))
    }

  }
}



