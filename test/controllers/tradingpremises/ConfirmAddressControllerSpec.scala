package controllers.tradingpremises

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{Address => BCAddress, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future


class ConfirmAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCache: DataCacheConnector = mock[DataCacheConnector]
    val controller = new ConfirmAddressController(messagesApi, self.dataCache, self.authConnector)
    val mockCacheMap = mock[CacheMap]



    when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(CacheMap("testCacheMap", Map()))))
  }

  "ConfirmTradingPremisesAddress" must {

    val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
      BCAddress("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "ghghg")
    val bm = BusinessMatching(Some(reviewDtls))


    "Get Option:" must {

      "Load Confirm trading premises address page successfully" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(Some(bm)))
        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.confirmaddress.title"))
      }

      "redirect to where is your trading premises page" when {
        "business matching model does not exist" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "business matching ->review details is empty" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(bm.copy(reviewDetails = None))))
          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }
    }

    "Post" must {
      val emptyCache = CacheMap("", Map.empty)

      val ytp = YourTradingPremises(
        "foo",
        Address(
          "line11",
          "line2",
          Some("line3"),
          Some("line4"),
          "AA1 1AA"
        )
      )

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned address is the trading premises address" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "true"
          )
          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
            .thenReturn(Future.successful(Some(bm)))

          when(controller.dataCacheConnector.save[TradingPremises](any(),
            meq(TradingPremises(yourTradingPremises = Some(ytp))))(any(),any(),any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ActivityStartDateController.get(1).url))
        }

        "option is 'No' is selected confirming the mentioned address is the trading premises address" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "false"
          )
          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
            .thenReturn(Future.successful(Some(bm)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }

      "throw error message on not selecting the option" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
        )
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(bm)))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.tp.confirm.address"))
      }

    }
  }
}
