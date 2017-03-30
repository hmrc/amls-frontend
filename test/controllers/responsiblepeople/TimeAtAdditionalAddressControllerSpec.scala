package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.SubmissionReadyForReview
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConversions._
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TimeAtAdditionalAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val timeAtAdditionalAddressController = new TimeAtAdditionalAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAdditionalAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "display the page" when {
        "timeAtAddress has been previously saved" in new Fixture {

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalAddressController.get(RecordId)(request)
          status(result) must be(OK)

        }
        "timeAtAddress has not been previously saved" in new Fixture {

          val responsiblePeople = ResponsiblePeople(personName)

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalAddressController.get(RecordId)(request)
          status(result) must be(OK)

        }

      }


      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {

        val responsiblePeople = ResponsiblePeople()

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAdditionalAddressController.get(outOfBounds)(request)
        status(result) must be(NOT_FOUND)
      }

    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "a time at address has been selected" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "04"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

      }

      "respond with BAD_REQUEST" when {
        "no time has been selected" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "01"
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }

      "when edit mode is on" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "01"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId, true).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "03"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }
        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "04"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }
      }

      "when edit mode is off" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "01"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "03"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))
            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
        }

        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "04"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
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
