package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}

class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    "validate model with only 2 check box selected" in {
      val model = Map(
        "services" -> Seq("01", "02", "03"),
        "test" -> Seq("false")
      )

      Service.formRuleStr.validate(model) must
        be(Success(Seq("vdv","hjg","fjklh")))
    }

    "fail to validate when given invalid data" in {


    }

  }
}
