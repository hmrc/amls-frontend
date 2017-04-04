package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.renewal.{MostTransactions, Renewal}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MostTransactionsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]
    val controller = new MostTransactionsController(self.authConnector, self.cache)
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = Renewal(
        mostTransactions = Some(
          MostTransactions(
            models.countries.take(3)
          )
        )
      )

      when(cache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".amls-error-summary").size mustEqual 0
    }

    "return a Bad request with errors on invalid submission" in new Fixture {

      val result = controller.post()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 1
    }

    "successfully submit valid data" in new Fixture {

      val incomingModel = Renewal()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      when(cache.fetch[Renewal](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Renewal(mostTransactions = None))))

      when(cache.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }
}
