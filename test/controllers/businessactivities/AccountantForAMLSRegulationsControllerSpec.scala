package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.aboutthebusiness.ConfirmRegisteredOfficeController
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future


class AccountantForAMLSRegulationsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new AccountantForAMLSRegulationsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "AccountantForAMLSRegulationsController" must {

    "use correct services" in new Fixture {
      ConfirmRegisteredOfficeController.authConnector must be(AMLSAuthConnector)
      ConfirmRegisteredOfficeController.dataCache must be(DataCacheConnector)
    }

    "Get Option:" must {

      "load the Accountant For AMLSRegulations page" in new Fixture {

        when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("businessactivities.accountantForAMLSRegulations.title")
      }

      "load Yes when accountant For AMLS Regulations from save4later returns True" in new Fixture {

        val accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true))
        val activities = BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-true").attr("checked") mustBe "checked"

      }

      "load No when accountant For AMLS Regulations from save4later returns No" in new Fixture {

        val accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(false))
        val activities = BusinessActivities(accountantForAMLSRegulations = accountantForAMLSRegulations)

        when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-false").attr("checked") mustBe "checked"

      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

        when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

      "successfully redirect to the page on selection of 'Yes' when edit mode is off" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("accountantForAMLSRegulations" -> "true")

        when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is on" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "accountantForAMLSRegulations" -> "false"
      )
      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is off" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "accountantForAMLSRegulations" -> "false"
      )
      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }
  }

  "on post invalid data show error" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody()
    when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
      .thenReturn(Future.successful(None))

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)
    contentAsString(result) must include(Messages("This field is required"))

  }

  "on post with invalid data show error" in new Fixture {
    val newRequest = request.withFormUrlEncodedBody(
      "WhatYouNeedController" -> ""
    )
    when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())(any(), any(), any()))
      .thenReturn(Future.successful(None))

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)
    contentAsString(result) must include("There are errors in your form submission")

  }

}
