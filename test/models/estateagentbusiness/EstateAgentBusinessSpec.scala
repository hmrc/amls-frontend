package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class EstateAgentBusinessSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "TODO" in {}
  }

  "Default" must {
    "convert an `Option[EAB]` to `EAB`" in {

      val model = EstateAgentBusiness(None, Some("foo"), Some("bar"))
      val zero = EstateAgentBusiness(None, None, None)

      (Some(model): EstateAgentBusiness) must be(model)
      (None: EstateAgentBusiness) must be(zero)
    }
  }
}
