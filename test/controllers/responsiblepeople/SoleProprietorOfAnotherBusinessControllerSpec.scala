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

  val personName = Some(PersonName("firstname", None, "lastname", None, None))
  val soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))

  "SoleProprietorOfAnotherBusinessController" when {

    "get is called" must {
      "display page" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.get(1)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(false)
        document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
      }

      "display page and prepopulate data from save4later" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName, soleProprietorOfAnotherBusiness = soleProprietorOfAnotherBusiness)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(true)
        document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)

      }

      "display page Not Found" when {
        "neither soleProprietorOfAnotherBusiness nor name is set" in new Fixture {

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)

        }
      }

    }

    "post is called" when {

      "soleProprietorOfAnotherBusiness is set to true" must {
        "go to VATRegisteredController" in new Fixture {

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

          verify(controller.dataCacheConnector).save(
            any(),
            meq(Seq(ResponsiblePeople(
              soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true)), vatRegistered = Some(VATRegisteredNo)
            )))
          )(any(), any(), any())

        }
      }

      "soleProprietorOfAnotherBusiness is set to false" when {

        "edit is true" must {
          "go to DetailedAnswersController" in new Fixture {

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

        "edit is false" must {
          "go to RegisteredForSelfAssessmentController" in new Fixture {

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

            verify(controller.dataCacheConnector).save(
              any(),
              meq(Seq(ResponsiblePeople(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)))))
            )(any(), any(), any())
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "given an invalid form" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "respond with NOT_FOUND" when {
        "ResponsiblePeople model cannot be found with given index" in new Fixture {

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