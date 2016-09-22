package controllers.declaration

import connectors.{DESConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.declaration.{AddPerson, WhoIsRegistering}
import models.responsiblepeople._
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AuthEnrolmentsService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhoIsRegisteringControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new  WhoIsRegisteringController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val desConnector = mock[DESConnector]
      override val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, false)

  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsRegisteringController" must {

    val personName = PersonName("Divya", Some("Envy"), "Tadisetti", None, Some("name"))
    val positions = Positions(Set(BeneficialOwner, InternalAccountant))
    val rp = ResponsiblePeople(
      personName = Some(personName),
      positions = Some(positions)
    )

    "Get Option:" must {

      "load the who is registering page" in new Fixture {

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))
        when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
          .thenReturn(None)

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("declaration.who.is.registering.title")
        htmlValue.getElementById("person-DivyaTadisetti").`val`() must be("DivyaTadisetti")
      }

      "load WhoIsRegistering page with pre-populated data from save4later" in new Fixture {

        val whoIsRegistering = Some(WhoIsRegistering("DivyaTadisetti"))
        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))
        when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
          .thenReturn(whoIsRegistering)

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("person-DivyaTadisetti").attr("checked") mustBe "checked"
      }

      "load WhoIsRegistering page with pre-populated data from save4later for the option 'Someone else'" in new Fixture {

        val whoIsRegistering = Some(WhoIsRegistering("-1"))
        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))
        when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
          .thenReturn(whoIsRegistering)

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("person--1").attr("checked") mustBe "checked"
      }
    }

    "Post" must {

      "successfully redirect next page when user selects the option 'Someone else'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("person" -> "-1")

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))

        when(controller.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.AddPersonController.get().url))
      }


      "successfully redirect next page when user selects one of the responsible person from the options" in new Fixture {

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("")))

        when(controller.desConnector.status(any())(any(),any(),any(),any()))
          .thenReturn(Future.successful(notCompletedReadStatusResponse))

        val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.DeclarationController.get().url))
      }


      "on post invalid data show error" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq.empty)))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("declaration.who.is.registering.text"))

      }

      "redirect to the declaration page for amendments if status of amlsRegNo is pending" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("")))

        when(controller.desConnector.status(any())(any(),any(),any(),any()))
          .thenReturn(Future.successful(pendingReadStatusResponse))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
      }
      "redirect to the declaration page if amlsRegNo is None" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(rp)))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
      }

    }
  }
}
