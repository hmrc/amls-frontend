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

class TimeAtAdditionalExtraAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val timeAtAdditionalExtraAddressController = new TimeAtAdditionalExtraAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val mockCacheMap = mock[CacheMap]
  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAdditionalExtraAddressController" must {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "use the correct services" in new Fixture {
      TimeAtAdditionalExtraAddressController.dataCacheConnector must be(DataCacheConnector)
      TimeAtAdditionalExtraAddressController.authConnector must be(AMLSAuthConnector)
    }

    "display the return with status 200" when {
      "get is called" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAdditionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)
      }
    }

    "must pass on post with all the mandatory non-UK parameters supplied" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "timeAtAddress" -> "03"
      )

      val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

      when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
    }

    "respond with BAD_REQUEST on submission" when {

      "nothing is selected" when {

        "editing" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          val responsiblePeople = ResponsiblePeople()

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true)(requestWithParams)
          val document: Document  = Jsoup.parse(contentAsString(result))
          val errorCount = 1
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("error.required.timeAtAddress"))
          }

        }

        "not editing" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)

        }
      }
    }

    "must go to the correct location when edit mode is on" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "timeAtAddress" -> "02"
      )

      val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

      when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = timeAtAdditionalExtraAddressController.post(RecordId, true)(requestWithParams)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
    }

    "must go to the correct location when edit mode is off" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "timeAtAddress" -> "02"
      )

      val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

      when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(RecordId).url))
    }

  }

}
