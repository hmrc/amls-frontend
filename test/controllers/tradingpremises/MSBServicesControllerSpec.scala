package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class MSBServicesControllerSpec extends PlaySpec with ScalaFutures with MockitoSugar with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new MSBServicesController {
      override val dataCacheConnector: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "MSBServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = TradingPremises(
        msbServices = Some(
          MsbServices(Set(TransmittingMoney, CurrencyExchange))
        )
      )
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select("input[value=02]").hasAttr("checked") mustBe true
      document.select(".amls-error-summary").size mustBe 0
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "invalid"
      )

      val result = controller.post(1)(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 1
    }

    "return a redirect to the 'How much Throughput' page on valid submission" in new Fixture {

      val model = TradingPremises(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1,edit = false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    "return a redirect to the 'X' page when adding 'Transmitting Money' as a service during edit" in new Fixture {

      val currentModel = TradingPremises(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney, CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    "return a redirect to the 'X' page when adding 'CurrencyExchange' as a service during edit" in new Fixture {

      val currentModel = TradingPremises(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    "return a redirect to the 'Check Your Answers' page when adding 'Cheque Cashing' as a service during edit" in new Fixture {

      Seq((ChequeCashingNotScrapMetal, "03"), (ChequeCashingScrapMetal, "04")) foreach {
        case (model, id) =>
          val currentModel = TradingPremises(
            msbServices = Some(MsbServices(
              Set(TransmittingMoney, CurrencyExchange)
            ))
          )

          val newRequest = request.withFormUrlEncodedBody(
            "msbServices[1]" -> "01",
            "msbServices[2]" -> "02",
            "msbServices[3]" -> id
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises(msbServices = None)))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

          val result = controller.post(1, edit = true)(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }
  }
}
