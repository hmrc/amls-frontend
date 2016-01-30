package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

import scala.collection.mutable.ArrayBuffer

class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    "validate model with only 2 check box selected" in {
      val model = Map(
        "services" -> Seq("02", "01", "03")
      )

      Service.servicesFormRule.validate(model) must
        be(Success(Seq(Auction, Commercial, Relocation)))
    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services" -> Seq("02", "99", "03")
      )

      Service.servicesFormRule.validate(model) must
        be(Failure(Seq((Path \ "services", Seq(ValidationError("Invalid Service Type String 99"))))))
    }
  }
}
