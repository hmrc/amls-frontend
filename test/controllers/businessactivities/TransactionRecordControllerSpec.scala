package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.bankdetails.BankDetails
import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TransactionRecordControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TransactionRecordController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "TransactionRecordController" must {

    "use correct services" in new Fixture {
      TransactionRecordController.authConnector must be(AMLSAuthConnector)
      TransactionRecordController.dataCacheConnector must be(DataCacheConnector)
    }

    "load the Customer Record Page" in new Fixture  {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.keep.customer.records.title"))

    }

    "pre-populate the Customer Record Page" in new Fixture  {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(None, None, None, None, Some(TransactionRecordYes(Set(Paper)))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)

    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isRecorded" -> "true",
        "transactions[0]" -> "01",
        "transactions[1]" -> "02"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(None, None, None, None, Some(TransactionRecordYes(Set(Paper, DigitalSpreadsheet)))))))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessFranchiseController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isRecorded" -> "true",
        "transactions[0]" -> "01",
        "transactions[1]" -> "02"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(None, None, None, None, Some(TransactionRecordYes(Set(Paper, DigitalSpreadsheet)))))))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "transactions[0]" -> "01",
        "transactions[1]" -> "02"
      )

      when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.saveDataShortLivedCache[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isRecorded]").html() must include("This field is required")
    }
  }

}
