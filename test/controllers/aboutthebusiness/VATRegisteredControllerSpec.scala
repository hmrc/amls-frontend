package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, Partnership}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future


class VATRegisteredControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new VATRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessRegisteredForVATController" must {

    "on get display the registered for VAT page" in new Fixture {
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.registeredforvat.title"))
    }


  "on get display the registered for VAT page with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(AboutTheBusiness(Some(PreviouslyRegisteredYes("")), None, Some(VATRegisteredYes("123456789"))))))

    val result = controller.get()(request)
    status(result) must be(OK)

    val document = Jsoup.parse(contentAsString(result))
    document.select("input[value=true]").hasAttr("checked") must be(true)
  }

    "on post with valid data" must {

      "go to RegisteredOfficeController if customer is a Partnership" in new Fixture {

        val partnership = ReviewDetails("BusinessName", Some(Partnership),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(partnership))))

        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(vatRegistered = Some(VATRegisteredNo))))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val newRequest = request.withFormUrlEncodedBody(
          "registeredForVAT" -> "true",
          "vrnNumber" -> "123456789"
        )

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeController.get().url))
      }

      "go to CorporationTaxRegistered if customer is not a partnership" in new Fixture {

        val partnership = ReviewDetails("BusinessName", Some(LPrLLP),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(Some(partnership))))

        when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
          .thenReturn(Some(AboutTheBusiness(vatRegistered = Some(VATRegisteredNo))))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val newRequest = request.withFormUrlEncodedBody(
          "registeredForVAT" -> "true",
          "vrnNumber" -> "123456789"
        )

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.CorporationTaxRegisteredController.get().url))
      }
    }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "registeredForVATYes" -> "1234567890"
    )

    val mockCacheMap = mock[CacheMap]

    when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)

    contentAsString(result) must include(Messages("error.required.atb.registered.for.vat"))
  }

   "on post with valid data in edit mode" in new Fixture {

     val newRequest = request.withFormUrlEncodedBody(
       "registeredForVAT" -> "true",
       "vrnNumber" -> "123456789"
     )

     val partnership = ReviewDetails("BusinessName", Some(LPrLLP),
       Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

     val mockCacheMap = mock[CacheMap]

     when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
       .thenReturn(Some(BusinessMatching(Some(partnership))))

     when(mockCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key))
       .thenReturn(Some(AboutTheBusiness(vatRegistered = Some(VATRegisteredNo))))

     when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
       .thenReturn(Future.successful(Some(mockCacheMap)))

     val result = controller.post(true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
   }

  }

}


