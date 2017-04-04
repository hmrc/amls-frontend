package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{ReviewDetails, Address => BusinessCustomerAddress}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCache: DataCacheConnector = mock[DataCacheConnector]
    val controller = new ConfirmAddressController(messagesApi, self.dataCache, self.authConnector)

  }

  "ConfirmAddress" when {

    val reviewDtls = ReviewDetails(
      "BusinessName",
      Some(BusinessType.LimitedCompany),
      BusinessCustomerAddress("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")),
      "ghghg"
    )

    val personName = PersonName("firstName", Some("middleName"), "lastName", None, Some("name"))

    val rp = ResponsiblePeople (
      personName = Some(personName)
    )

    val responsiblePerson = Seq(rp)

    val bm = BusinessMatching(Some(reviewDtls))

    "Get is called" must {

      "Load Confirm address page successfully" in new Fixture {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bm))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.confirmaddress.heading", personName.titleName))
      }

      "redirect to current address page" when {
        "business matching model does not exist" in new Fixture {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople())))

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(None)

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        }

        "business matching ->review details is empty" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople())))

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(bm.copy(reviewDetails = None)))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressController.get(1).url))
        }
      }
    }

    "Post is called" must {

      val UKAddress = PersonAddressUK("line1", "line2", Some("line3"), Some("line4"), "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, None)
      val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
      val rp = ResponsiblePeople(addressHistory = Some(history))

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned address is the address" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople())))

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(bm))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtCurrentAddressController.get(1).url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](
            any(),
            meq(Seq(rp))
          )(any(), any(), any())
        }

        "option is 'No' is selected confirming the mentioned address is the address" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "false"
          )
          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
            .thenReturn(Future.successful(Some(bm)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressController.get(1).url))
        }
      }

      "throw error message on not selecting the option" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
        )

        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(responsiblePerson))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bm))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.rp.confirm.address", personName.titleName))
      }

    }
  }
}
