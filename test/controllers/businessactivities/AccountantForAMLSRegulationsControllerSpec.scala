package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class AccountantForAMLSRegulationsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new AccountantForAMLSRegulationsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AccountantForAMLSRegulationsController" when {

    "get is called" must {

      "load the Accountant For AMLSRegulations page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val pageTitle = Messages("businessactivities.accountantForAMLSRegulations.title") + " - " +
          Messages("progress.businessactivities.name") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe pageTitle
      }

      "load Yes when accountant For AMLS Regulations from save4later returns True" in new Fixture {

        val accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true))
        val activities = BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-true").attr("checked") mustBe "checked"

      }

      "load No when accountant For AMLS Regulations from save4later returns No" in new Fixture {

        val accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(false))
        val activities = BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-false").attr("checked") mustBe "checked"

      }
    }

    "Post is called" must {

      "respond with SEE_OTHER" when {
        "edit is true" must {
          "redirect to the WhoIsYourAccountantController when 'yes' is selected'" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhoIsYourAccountantController.get().url))
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "accountantForAMLSRegulations" -> "false"
            )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
          }
        }

        "edit is false" must {
          "redirect to the WhoIsYourAccountantController on selection of 'Yes'" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.WhoIsYourAccountantController.get().url))
          }

          "successfully redirect to the SummaryController on selection of Option 'No'" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "accountantForAMLSRegulations" -> "false"
            )
            when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "no options are selected so that the request body is empty" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody()

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.ba.business.use.accountant"))

        }

        "given invalid json" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "WhatYouNeedController" -> ""
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("err.summary"))

        }
      }
    }
  }
}
