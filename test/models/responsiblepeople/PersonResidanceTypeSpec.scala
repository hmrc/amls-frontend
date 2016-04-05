package models.responsiblepeople

import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}

class PersonResidanceTypeSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate" in {

      val ukModel = Map(
        "isUKResidence" -> Seq("true"),
        "nino" -> Seq("12346464646"),
        "countryOfBirth" -> Seq("GB"),
        "nationality" -> Seq("GB")
      )

      PersonResidenceType.formRule.validate(ukModel) must
        be(Success(PersonResidenceType(UKResidence("12346464646"), Country("United Kingdom", "GB"), "GB")))
    }
  }


}
