package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.aboutyou.RoleWithinBusinessController
import models.aboutyou.{BeneficialShareholder, AboutYou, RoleWithinBusiness}
import models.bankdetails.{NoBankAccount, PersonalAccount, BankDetails, BankAccountType}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.{Failure, Path, Success}
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class BankAccountTypeControllerSpec extends PlaySpec with  OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new BankAccountTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BankAccountTypeController" must {

    "on get display kind of bank account page" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
    }

    "on get display kind of bank account page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(Some(NoBankAccount), None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=04]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "01"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BankAccountTypeController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "04"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(BankDetails(Some(NoBankAccount), None)))))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "bankAccountType" -> "10"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[Seq[BankDetails]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include("Invalid value")
    }
  }
}
