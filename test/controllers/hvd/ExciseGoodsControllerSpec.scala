package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.{ExciseGoods, Hvd}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ExciseGoodsControllerSpec extends PlaySpec  with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ExciseGoodsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExciseGoodsController" must {

    "successfully load UI for the first time" in new Fixture {
      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
    }

    "successfully load UI from save4later" in new Fixture {

      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Hvd(exciseGoods = Some(ExciseGoods(true))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe Messages("hvd.excise.goods.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
      htmlValue.getElementById("exciseGoods-true").`val`() mustBe "true"
      htmlValue.getElementById("exciseGoods-false").`val`() mustBe "false"
    }

    "successfully redirect to next page when submitted with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "true")

      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.HowWillYouSellGoodsController.get().url))
    }

    "successfully redirect to nex page when submitted with valida data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("exciseGoods" -> "false")

      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }

    "fail with validation error when mandatory field is missing" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(

      )
      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.hvd.excise.goods"))
    }

  }

}
