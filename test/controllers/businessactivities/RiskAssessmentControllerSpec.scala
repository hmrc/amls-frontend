package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, MoneyServiceBusiness, AccountancyServices, BusinessMatching}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class RiskAssessmentControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RiskAssessmentController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RiskAssessmentController" when {

    "get is called" must {
      "load the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("businessactivities.riskassessment.policy.title"))

      }

      "pre-populate the Risk assessment Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyYes(Set(PaperBased)))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)

      }
    }

    "post is called" must {
      "when edit is false" must {
        "on post with valid data and load check your answers page when businessActivity is ASP" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(AccountancyServices, MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
        }

        "on post with valid data and load advice on MLR due to diligence page when businessActivity is not ASP" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get().url))
        }

        "respond with BAD_REQUEST" when {
          "hasPolicy field is missing" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "riskassessments[0]" -> "01",
              "riskassessments[1]" -> "02"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }

          "hasPolicy field is missing, represented by an empty string" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "",
              "riskassessments[0]" -> "01",
              "riskassessments[1]" -> "02"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            println(document)
            document.select("a[href=#hasPolicy]").html() must include(Messages("error.required.ba.option.risk.assessment"))
          }

          "riskassessments fields are missing" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "true"
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))
            println(document)
            document.select("a[href=#riskassessments]").html() must include(Messages("error.required.ba.risk.assessment.format"))
          }

          "riskassessments fields are missing, represented by an empty string" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "hasPolicy" -> "true",
              "riskassessments[0]" -> "",
              "riskassessments[1]" -> ""
            )

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())
              (any(), any(), any())).thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(BAD_REQUEST)

            val document = Jsoup.parse(contentAsString(result))

            println(document)

            document.select("a[href=#riskassessments[0]-riskassessments]").html() must include(Messages("error.invalid"))
          }
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasPolicy" -> "true",
            "riskassessments[0]" -> "01",
            "riskassessments[1]" -> "02"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))
          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(Some(BusinessActivities(riskAssessmentPolicy = Some(RiskAssessmentPolicyNo))))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      RiskAssessmentController.authConnector must be(AMLSAuthConnector)
      RiskAssessmentController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
