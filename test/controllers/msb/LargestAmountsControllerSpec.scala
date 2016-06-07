package controllers.msb

import connectors.DataCacheConnector
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.mockito.MockitoMatchers
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

class LargestAmountsControllerSpec extends PlaySpec with MockitoMatchers {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new ServicesController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "LargestAmountsController" must {

  }
}
