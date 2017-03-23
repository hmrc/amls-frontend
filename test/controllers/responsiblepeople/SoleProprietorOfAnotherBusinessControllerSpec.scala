package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, ResponsiblePeople, SoleProprietorOfAnotherBusiness, VATRegisteredNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SoleProprietorOfAnotherBusinessControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]

    val controller = new SoleProprietorOfAnotherBusinessController(dataCacheConnector = mockDataCacheConnector, authConnector = self.authConnector) {
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val pageTitle = Messages("responsiblepeople.sole.proprietor.another.business.title", "firstname lastname") + " - " +
    Messages("summary.responsiblepeople") + " - " +
    Messages("title.amls") + " - " + Messages("title.gov")
  val personName = Some(PersonName("firstname", None, "lastname", None, None))
  val soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))

  "SoleProprietorOfAnotherBusinessController" when {

    "get is called" must {
      "display page" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.get(1)(request)

        status(result) must be(OK)
      }
    }

    "get" must {
      "display page and prepopulate data from save4later" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName, soleProprietorOfAnotherBusiness = soleProprietorOfAnotherBusiness)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=soleProprietorOfAnotherBusiness]").`val` must be("true")

      }
    }

    "post is called" must {
      "when edit is true" must {
        "redirect to the detailed answers controller" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "false",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(mockDataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))


          val result = controller.post(1,true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
        }
      }
      "when edit is false" must {
        "redirect to the vat registered controller when yes is selected" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "true",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(vatRegistered = Some(VATRegisteredNo))))))

          when(mockDataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(1).url))

          verify(controller.dataCacheConnector).save(any(),
            meq(Seq(ResponsiblePeople(hasChanged = false,
              soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true)), vatRegistered = None))))(any(), any(), any())

        }

        "redirect to the vat registered controller when yes is selected and edit is true" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "true",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(mockDataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(1, true).url))
        }

        "redirect to the sole proprietor another business controller when another type is selected" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "false",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(mockDataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))

          verify(controller.dataCacheConnector).save(any(),
            meq(Seq(ResponsiblePeople(hasChanged = false,
              soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false))))))(any(), any(), any())
        }
      }

      "respond with BAD_REQUEST" when {
        "fail submission on empty string" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title mustBe(pageTitle)
          document.select("a[href=#soleProprietorOfAnotherBusiness]").html() must
            include(Messages("error.required.rp.sole_proprietor", personName.get.fullName))

        }
      }

      "respond with NOT_FOUND" when {
        "return not found when no rps" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "true",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(1)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }
    }

  }
}