package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching._
import models.moneyservicebusiness.MoneyServiceBusiness
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }
  val mockCacheMap = mock[CacheMap]

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {

      val model = MoneyServiceBusiness(None)
      val msbServices = Some(
        MsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(model))

      when(controller.dataCache.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      val msbServices = Some(
        MsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }
}
