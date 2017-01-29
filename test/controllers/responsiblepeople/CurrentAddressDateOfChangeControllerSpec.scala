package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.DateOfChange
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ThreeYearsPlus, OneToThreeYears, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class CurrentAddressDateOfChangeControllerSpec extends GenericTestHelper with MockitoSugar {

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new CurrentAddressDateOfChangeController() {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "CurrentAddressDateOfChangeController" must {
    "when get is called" must {
      "return view for Date of Change when given a valid request" in new Fixture {
        val result = controller.get(0, false)(request)
        status(result) must be(OK)
      }
    }

    "when post is called" when {
      "given valid data for a current address time ZeroToFiveMonths" must {
        "redirect to the additional address page" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, ZeroToFiveMonths)
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePeople(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
            positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))

        }
      }
      "given valid data for a current address time SixToElevenMonths" must {
        "redirect to the additional address page" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, SixToElevenMonths)
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePeople(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
            positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))

        }
      }
      "given valid data for a current address with time OneToThreeYears" must {
        "redirect to the details page" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, OneToThreeYears)
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePeople(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
            positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

        }
      }
      "given valid data for a current address with time ThreeYearsPlus" must {
        "redirect to the details page" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, ThreeYearsPlus)
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePeople(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
            positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

        }
      }
    }
    "respond with BAD_REQUEST" when {
      "given invalid data" in new Fixture {

        val invalidPostRequest = request.withFormUrlEncodedBody()

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
        val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, ThreeYearsPlus)
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
        val responsiblePeople = ResponsiblePeople(
          addressHistory = Some(history),
          personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
          positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, true)(invalidPostRequest)

        status(result) must be(BAD_REQUEST)

      }
      "given a date before the responsible person start date" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01",
          "activityStartDate" -> new LocalDate(2017, 1, 1).toString("yyyy-MM-dd")
        )

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
        val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, ThreeYearsPlus, Some(DateOfChange(new LocalDate(2017,1,1))))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
        val responsiblePeople = ResponsiblePeople(
          addressHistory = Some(history),
          personName = Some(PersonName("firstName", Some("middleName"), "LastName", None, None)),
          positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2017,1,1)))))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, true)(postRequest)

        status(result) must be(BAD_REQUEST)

      }
    }
    "respond with NOT_FOUND" when {
      "post is called with an out of bounds index" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(40, true)(postRequest)

        status(result) must be(NOT_FOUND)

      }
    }
  }

}
