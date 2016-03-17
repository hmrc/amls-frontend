package models.businessmatching

import models.businessmatching.BusinessType._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class BusinessTypeSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      BusinessType.formR.validate(Map("businessType" -> Seq("01"))) must
        be(Success(LimitedCompany))

      BusinessType.formR.validate(Map("businessType" -> Seq("02"))) must
        be(Success(SoleProprietor))

      BusinessType.formR.validate(Map("businessType" -> Seq("03"))) must
        be(Success(Partnership))

      BusinessType.formR.validate(Map("businessType" -> Seq("04"))) must
        be(Success(LPrLLP))

      BusinessType.formR.validate(Map("businessType" -> Seq("05"))) must
        be(Success(UnincorporatedBody))

    }

    "write correct data from enum value" in {

      BusinessType.formW.writes(LimitedCompany) must
        be(Map("businessType" -> Seq("01")))

      BusinessType.formW.writes(SoleProprietor) must
        be(Map("businessType" -> Seq("02")))

      BusinessType.formW.writes(Partnership) must
        be(Map("businessType" -> Seq("03")))

      BusinessType.formW.writes(LPrLLP) must
        be(Map("businessType" -> Seq("04")))

      BusinessType.formW.writes(UnincorporatedBody) must
        be(Map("businessType" -> Seq("05")))

    }

    "businessType toString" in {
      SoleProprietor.toString must be("SOP")
      LimitedCompany.toString must be("LTD")
      Partnership.toString must be("OBP")
      LPrLLP.toString must be("LP")
      UnincorporatedBody.toString must be("UIB")
    }

    "throw error on invalid data" in {
      BusinessType.formR.validate(Map("businessType" -> Seq("20"))) must
        be(Failure(Seq((Path \ "businessType", Seq(ValidationError("error.invalid"))))))
    }
  }
}
