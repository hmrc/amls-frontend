package controllers.auth

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{OrgAccount, SaAccount, CtAccount, Accounts}

class AmlsRegimeSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  "AmlsRegimeSpec" should {

    "Correctly calculate isAuthorised" in {

      val ct = Accounts(ct = Some(mock[CtAccount]))
      val org = Accounts(org = Some(mock[OrgAccount]))
      val sa = Accounts(sa = Some(mock[SaAccount]))

      AmlsRegime.isAuthorised(ct) must be (true)
      AmlsRegime.isAuthorised(org) must be (true)
      AmlsRegime.isAuthorised(sa) must be (true)
      AmlsRegime.isAuthorised(Accounts()) must be (false)

    }

    "return the correct route for unauthorised page" in {
      AmlsRegime.unauthorisedLandingPage must be (Some(controllers.routes.AmlsController.unauthorised().url))
    }


  }

}
