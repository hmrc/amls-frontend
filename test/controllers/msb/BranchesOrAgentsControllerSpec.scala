package controllers.msb

import connectors.DataCacheConnector
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.test.Helpers._

import scala.concurrent.Future

class BranchesOrAgentsControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new BranchesOrAgentsController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "BranchesOrAgentsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[name=hasCountries][checked]").size mustEqual 0
    }

    "show a prefilled form when store contains data" in new Fixture {

      val model = MoneyServiceBusiness(
        branchesOrAgents = Some(
          BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))
        )
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("input[name=hasCountries]").size mustEqual 2
      document.select("input[name=hasCountries][checked]").`val` mustEqual "true"
      document.select("*[selected]").size mustEqual 1
    }
  }
}
