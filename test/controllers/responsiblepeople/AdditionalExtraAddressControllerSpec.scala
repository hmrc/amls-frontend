package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import scala.collection.JavaConversions._
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class AdditionalExtraAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val additionalExtraAddressController = new AdditionalExtraAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val mockCacheMap = mock[CacheMap]

  "AdditionalExtraAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "display the persons page" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

      }

      "display 404 Not Found" when {
        "name cannot be found" in new Fixture {
          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = additionalExtraAddressController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {

      "pass on with all the mandatory UK parameters supplied" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "AA1 1AA"
        )

        val responsiblePeople = ResponsiblePeople(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(SEE_OTHER)
      }

      "pass on with all the mandatory non-UK parameters supplied" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "false",
          "addressLineNonUK1" -> "Line 1",
          "addressLineNonUK2" -> "Line 2",
          "country" -> "ES"
        )

        val responsiblePeople = ResponsiblePeople()

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(SEE_OTHER)
      }

      "fail to submit" when {

        "isUK field is not given" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()
          val result = additionalExtraAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

        }

        "default fields for UK is not given" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> ""
          )

          val result = additionalExtraAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

        }

        "default fields for overseas is not given" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> ""
          )

          val result = additionalExtraAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

        }

        "an invalid uk address is given" when {
          "not editing" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "false",
              "addressLineNonUK1" -> "Line #1",
              "addressLineNonUK2" -> "Line #2",
              "country" -> "ES"
            )

            val responsiblePeople = ResponsiblePeople(personName)

            when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

            when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
            status(result) must be(BAD_REQUEST)

          }
          "editing" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line &1",
              "addressLine2" -> "Line &2",
              "postCode" -> "AA1 1AA"
            )

            val responsiblePeople = ResponsiblePeople()

            when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

            when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = additionalExtraAddressController.post(RecordId, true)(requestWithParams)
            status(result) must be(BAD_REQUEST)

          }
        }

      }

      "go to the correct location when edit mode is on" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "AA1 1AA"
        )

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId, true)(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
      }

      "go to timeAtAdditionalExtraAddress" when {

        "edit mode is off" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )

          val responsiblePeople = ResponsiblePeople()

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.TimeAtAdditionalExtraAddressController.get(RecordId).url))
        }

        "edit mode is on and time at address does not exist" in new Fixture {
          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )

          val responsiblePeople = ResponsiblePeople()

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId, true)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.TimeAtAdditionalExtraAddressController.get(RecordId, true).url))
        }
      }

    }

  }

  it must {
    "use the correct services" in new Fixture {
      AdditionalAddressController.dataCacheConnector must be(DataCacheConnector)
      AdditionalAddressController.authConnector must be(AMLSAuthConnector)
    }
  }

}
