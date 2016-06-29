package controllers.businessactivities


import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => Activities, BusinessMatching}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture


import scala.concurrent.Future

class ExpectedAMLSTurnoverControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ExpectedAMLSTurnoverController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    val cache = mock[CacheMap]

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set.empty))
    )

    def model: Option[BusinessActivities] = None

    when(controller.dataCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(cache)))

    when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(businessMatching))

    when(cache.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
      .thenReturn(model)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedAMLSTurnoverController" must {

    "on get display the Turnover Expect In 12Months Related To AMLS page" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.turnover.title"))
    }

    "on get display the Role Within Business page with pre populated data" in new Fixture {

      override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "expectedAMLSTurnover" -> "01"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "expectedAMLSTurnover" -> "01"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      val result = controller.post(true)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST
      document.select(".amls-error-summary").size mustEqual 1
    }
  }
}
