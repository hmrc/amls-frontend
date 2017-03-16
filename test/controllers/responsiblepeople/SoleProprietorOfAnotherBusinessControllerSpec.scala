import connectors.DataCacheConnector
import controllers.responsiblepeople.{routes, SoleProprietorOfAnotherBusinessController, VATRegisteredController}
import models.responsiblepeople.{ResponsiblePeople, PersonName}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
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

  "SoleProprietorOfAnotherBusinessController" when {

    "get is called" must {
      "display page" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

        val result = controller.get(1)(request)

        status(result) must be(OK)
      }
    }

    "post is called" must {
      "when edit is true" must {
        "redirect to the detailed answers controller" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("soleProprietorOfAnotherBusiness" -> "true")

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(None)
          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.post(1,true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
        }
      }
      "when edit is false" must {
        "redirect to the vat registered controller when yes is selected" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("soleProprietorOfAnotherBusiness" -> "true")

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(None)
          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(1).url))
        }

        "redirect to the sole proprietor another business controller when another type is selected" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("soleProprietorOfAnotherBusiness" -> "false")

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(None)
          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))
        }


      }

      "respond with BAD_REQUEST" when {
        "fail submission on empty string" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("soleProprietorOfAnotherBusiness" -> "")

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

          val newRequest = request.withFormUrlEncodedBody("soleProprietorOfAnotherBusiness" -> "true")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(1)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }
    }

  }
}