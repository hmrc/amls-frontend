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

  "TradingPremisesAddController" when {
    "get is called" should {
      "add empty trading premises and redirect to the correct page" in new Fixture {

        val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))
        val tpSeq =
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(testSeq)))

        val resultF = controller.get(guidanceRequested)(request)

        status(resultF) must be(SEE_OTHER)
        redirectLocation(resultF) must be(Some(expectedRedirect.url))

        verify(controller.dataCacheConnector)
          .save[Seq[TradingPremises]](meq(TradingPremises.key), meq(testSeq :+ TradingPremises()))(any(), any(), any())

        reset(controller.dataCacheConnector)
      }
    }
  }
}