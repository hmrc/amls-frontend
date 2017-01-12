package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.aboutthebusiness.routes
import models.DateOfChange
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOfficeUK}
import models.responsiblepeople.TimeAtAddress.{ZeroToFiveMonths, OneToThreeYears}
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class CurrentAddressDateOfChangeControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {
  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true) )

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new CurrentAddressDateOfChangeController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "CurrentAddressDateOfChangeController" must {
    "return viewfor Date of Change" in new Fixture {
      val result = controller.get(0, false)(request)
      status(result) must be(OK)
    }


    "handle the date of change form post" when {
      "given valid data for a current address" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
        val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, ZeroToFiveMonths)
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
        val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

        val updatedCurrentAddress = currentAddress.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(controller.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(0)(postRequest)

        status(result) must be(SEE_OTHER)

        //      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        //
        //      val captor = ArgumentCaptor.forClass(classOf[AboutTheBusiness])
        //      verify(controller.dataCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), captor.capture())(any(), any(), any())
        //
        //      captor.getValue.registeredOffice match {
        //        case Some(savedOffice: RegisteredOfficeUK) => savedOffice must be(updatedOffice)
        //      }

      }
    }
  }

}
