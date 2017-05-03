package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.{Country, DateOfChange}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class NewHomeAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controllers = app.injector.instanceOf[NewHomeAddressController]
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99
  val personName = Some(PersonName("firstname", None, "lastname", None, None))

  "NewHomeAddressController" when {

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePeople()

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controllers.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the new home address page successfully" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controllers.get(RecordId)(request)
        status(result) must be(OK)

      }
    }

    "post is called" must {
      "redirect to DetailedAnswersController" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(OneToThreeYears), Some(DateOfChange(LocalDate.now().plusMonths(13))))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controllers.dataCacheConnector.fetch[NewHomeDateOfChange](meq(NewHomeDateOfChange.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(LocalDate.now().plusMonths(13)))))

          when(controllers.dataCacheConnector.save[ResponsiblePeople](meq(ResponsiblePeople.key), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(responsiblePeople)))(any(), any(), any())
        }

        "all the mandatory UK parameters are supplied and date of move is more then 6 months" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(SixToElevenMonths), Some(DateOfChange(LocalDate.now().plusMonths(7))))
      
          val additionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 11", "Line 22", None, None, "AB1 1BA"), Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 21", "Line 22", None, None, "BB1 1BB"), Some(ZeroToFiveMonths))

          val upHistory = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(upHistory))

          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controllers.dataCacheConnector.fetch[NewHomeDateOfChange](meq(NewHomeDateOfChange.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(LocalDate.now().plusMonths(7)))))

          when(controllers.dataCacheConnector.save[ResponsiblePeople](meq(ResponsiblePeople.key), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(responsiblePeople)))(any(), any(), any())
        }

        "all the mandatory UK parameters are supplied and date of move is more then 3 years" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 11",
            "addressLine2" -> "Line 21",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 11", "Line 21", None, None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ThreeYearsPlus), Some(DateOfChange(LocalDate.now().plusMonths(37))))
          val additionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 11", "Line 22", None, None, "AB1 1BA"), Some(ZeroToFiveMonths))
          val additionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 21", "Line 22", None, None, "BB1 1BB"), Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress),
            additionalAddress = Some(additionalAddress),
            additionalExtraAddress = Some(additionalExtraAddress))

          val responsiblePeople1 = ResponsiblePeople(addressHistory = Some(history))
          val updatedHistory = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress), additionalAddress = None, additionalExtraAddress = None)

          when(controllers.dataCacheConnector.fetch[NewHomeDateOfChange](meq(NewHomeDateOfChange.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(LocalDate.now().plusMonths(37)))))

          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople1))))

          when(controllers.dataCacheConnector.save[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key),
            any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePeople]](any(),
            meq(Seq(responsiblePeople1.copy(addressHistory = Some(updatedHistory), hasChanged = true))))(any(), any(), any())
        }

        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line 1",
            "addressLineNonUK2" -> "Line 2",
            "country" -> "ES"
          )
          val NonUKAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain","ES"))
          val currentAddress = ResponsiblePersonCurrentAddress(NonUKAddress, Some(ZeroToFiveMonths), Some(DateOfChange(LocalDate.now)))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controllers.dataCacheConnector.fetch[NewHomeDateOfChange](meq(NewHomeDateOfChange.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(NewHomeDateOfChange(LocalDate.now()))))

          when(controllers.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))

          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePeople]](any(),
            meq(Seq(responsiblePeople.copy(addressHistory = Some(history)))))(any(),any(), any())
        }

      }

      "respond with BAD_REQUEST" when {

        "given an invalid address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line &1",
            "addressLine2" -> "Line *2",
            "postCode" -> "AA1 1AA"
          )
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controllers.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)
        }

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(controllers.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          val result = controllers.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)
        }

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> ""
          )
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controllers.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)
        }

        "the default fields for overseas are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> ""
          )
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
            val result = controllers.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

        }

        "respond with NOT_FOUND" when {
          "given an out of bounds index" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "AA1 1AA"
            )
            val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(controllers.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controllers.post(outOfBounds)(requestWithParams)

            status(result) must be(NOT_FOUND)
          }
        }
      }
    }
  }
}


