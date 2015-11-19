package controllers.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class TelephoningYourBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  override protected def authConnector: AuthConnector = mock[AuthConnector]

  object MockTelephoningYourBusiness extends TelephoningYourBusinessController {
    override def authConnector = authConnector
  }

}
