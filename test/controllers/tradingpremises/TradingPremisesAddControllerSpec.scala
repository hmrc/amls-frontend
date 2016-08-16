package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching._
import models.tradingpremises.TradingPremises
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import utils.AuthorisedFixture
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._

import scala.concurrent.Future


class TradingPremisesAddControllerSpec extends WordSpecLike
  with MustMatchers with MockitoSugar with ScalaFutures with OneAppPerSuite with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TradingPremisesAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }
}