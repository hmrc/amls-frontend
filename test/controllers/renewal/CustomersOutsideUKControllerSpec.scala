package controllers.renewal

import connectors.DataCacheConnector
import controllers.businessactivities.CustomersOutsideUKController
import models.Country
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing}
import models.renewal.{CustomersOutsideUK, Renewal}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class CustomersOutsideUKControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val dataCacheConnector = mock[DataCacheConnector]

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[CustomersOutsideUKController]

  }

  "The customer outside uk controller" when {

    "get is called" must {
      "load the page" in new Fixture {

        when(dataCacheConnector.fetch[Renewal](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)

        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        val pageTitle = Messages("renewal.customer.outside.uk.title") + " - " +
          Messages("summary.renewal") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle
      }

      "pre-populate the Customer outside UK Page" in new Fixture {

        when(dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Renewal(customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isOutside]").size mustEqual 2
        document.select("input[name=isOutside][checked]").`val` mustEqual "true"
        document.select("select[name=countries[0]] > option[value=GB]").hasAttr("selected") must be(true)

      }

    }

    "post is called" when {

      "given valid data" must {

        "redirect to the summary page" when {
          "business is not an hvd" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "isOutside" -> "true",
              "countries[0]" -> "GB",
              "countries[1]" -> "US"
            )

            when(dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching()))

            when(mockCacheMap.getEntry[Renewal](Renewal.key))
              .thenReturn(Some(Renewal()))

            when(dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }

        "redirect to the PercentageOfCashPaymentOver15000Controller" when {
          "business is not an hvd" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "isOutside" -> "true",
              "countries[0]" -> "GB",
              "countries[1]" -> "US"
            )

            when(dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(activities = Some(BusinessActivities(Set(HighValueDealing))))))

            when(mockCacheMap.getEntry[Renewal](Renewal.key))
              .thenReturn(Some(Renewal()))

            when(dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PercentageOfCashPaymentOver15000Controller.get().url))
          }
        }

      }
      "respond with BAD_REQUEST" when {
        "given invalid data represented by an empty string" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isOutside" -> "true",
            "countries[0]" -> "",
            "countries[1]" -> ""
          )

          when(dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#countries]").html() must include(Messages("error.required.country.name"))
        }
        "mandatory fields are missing" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isOutside" -> ""
          )

          when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isOutside]").html() must include(Messages("error.required.ba.select.country"))
        }
      }
    }

  }
}
